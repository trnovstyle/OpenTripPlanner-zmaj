package org.opentripplanner.netex.mapping;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.timetable.Trip;
import org.rutebanken.netex.model.DirectionTypeEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This maps a NeTEx ServiceJourney to an OTP Trip. A ServiceJourney can be connected to a Line (OTP
 * Route) in two ways. Either directly from the ServiceJourney or through JourneyPattern → Route.
 * The former has precedent over the latter.
 */
class TripMapper {

  private static final Logger LOG = LoggerFactory.getLogger(TripMapper.class);

  private final FeedScopedIdFactory idFactory;
  private final DataImportIssueStore issueStore;
  private final EntityById<org.opentripplanner.transit.model.network.Route> otpRouteById;
  private final ReadOnlyHierarchicalMap<String, Route> routeById;
  private final ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternsById;
  private final Map<String, FeedScopedId> serviceIds;
  private final Set<FeedScopedId> shapePointIds;
  private final EntityById<Operator> operatorsById;
  private final TransportModeMapper transportModeMapper = new TransportModeMapper();
  private final EntityById<Trip> mappedTrips = new EntityById<>();

  TripMapper(
    FeedScopedIdFactory idFactory,
    DataImportIssueStore issueStore,
    EntityById<Operator> operatorsById,
    EntityById<org.opentripplanner.transit.model.network.Route> otpRouteById,
    ReadOnlyHierarchicalMap<String, Route> routeById,
    ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternsById,
    Map<String, FeedScopedId> serviceIds,
    Set<FeedScopedId> shapePointIds
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.otpRouteById = otpRouteById;
    this.routeById = routeById;
    this.journeyPatternsById = journeyPatternsById;
    this.serviceIds = serviceIds;
    this.shapePointIds = shapePointIds;
    this.operatorsById = operatorsById;
  }

  /**
   * Map a service journey to a trip.
   * <p>
   *
   * @return valid trip or {@code null} if unable to map to a valid trip.
   */
  @Nullable
  Trip mapServiceJourney(ServiceJourney serviceJourney, Supplier<String> headsign) {
    FeedScopedId serviceId = serviceIds.get(serviceJourney.getId());

    if (serviceId == null) {
      LOG.warn("Unable to map ServiceJourney, missing Route. SJ id: {}", serviceJourney.getId());
      return null;
    }

    org.opentripplanner.transit.model.network.Route route = resolveRoute(serviceJourney);

    if (route == null) {
      LOG.warn(
        "Unable to map ServiceJourney, missing serviceId. SJ id: {}",
        serviceJourney.getId()
      );
      return null;
    }

    FeedScopedId id = idFactory.createId(serviceJourney.getId());

    if (mappedTrips.containsKey(id)) {
      return mappedTrips.get(id);
    }

    var wheelChairBoarding = WheelChairMapper.wheelchairAccessibility(
      serviceJourney.getAccessibilityAssessment(),
      WheelchairAccessibility.NO_INFORMATION
    );

    var builder = Trip.of(id);
    builder.withRoute(route);
    builder.withServiceId(serviceId);
    builder.withShapeId(getShapeId(serviceJourney));
    builder.withWheelchairBoarding(wheelChairBoarding);

    if (serviceJourney.getPrivateCode() != null) {
      builder.withNetexInternalPlanningCode(serviceJourney.getPrivateCode().getValue());
    }

    builder.withShortName(serviceJourney.getPublicCode());
    builder.withOperator(findOperator(serviceJourney));

    if (serviceJourney.getTransportMode() != null) {
      T2<TransitMode, String> transitMode = null;
      try {
        transitMode =
          transportModeMapper.map(
            serviceJourney.getTransportMode(),
            serviceJourney.getTransportSubmode()
          );
      } catch (TransportModeMapper.UnsupportedModeException e) {
        issueStore.add(
          "UnsupportedModeInServiceJourney",
          "Unsupported mode in ServiceJourney. Mode: %s, sj: %s",
          e.mode,
          serviceJourney.getId()
        );
        return null;
      }
      builder.withMode(transitMode.first);
      builder.withNetexSubmode(transitMode.second);
    }

    builder.withDirection(DirectionMapper.map(resolveDirectionType(serviceJourney)));

    builder.withNetexAlteration(
      TripServiceAlterationMapper.mapAlteration(serviceJourney.getServiceAlteration())
    );

    // TODO RTM - Instead of getting the first headsign from the StopTime this could be the
    //          - default behaviour of the TransitModel - So, in the NeTEx mapper we would just
    //          - ignore setting the headsign on the Trip.
    builder.withHeadsign(headsign.get());

    return builder.build();
  }

  private DirectionTypeEnumeration resolveDirectionType(ServiceJourney serviceJourney) {
    Route netexRoute = lookUpNetexRoute(serviceJourney);
    if (netexRoute != null && netexRoute.getDirectionType() != null) {
      return netexRoute.getDirectionType();
    } else {
      return null;
    }
  }

  @Nullable
  private FeedScopedId getShapeId(ServiceJourney serviceJourney) {
    JourneyPattern journeyPattern = journeyPatternsById.lookup(
      serviceJourney.getJourneyPatternRef().getValue().getRef()
    );
    FeedScopedId serviceLinkId = journeyPattern != null
      ? idFactory.createId(journeyPattern.getId().replace("JourneyPattern", "ServiceLink"))
      : null;

    return shapePointIds.contains(serviceLinkId) ? serviceLinkId : null;
  }

  private Route lookUpNetexRoute(ServiceJourney serviceJourney) {
    if (serviceJourney.getJourneyPatternRef() != null) {
      JourneyPattern journeyPattern = journeyPatternsById.lookup(
        serviceJourney.getJourneyPatternRef().getValue().getRef()
      );
      if (journeyPattern != null && journeyPattern.getRouteRef() != null) {
        String routeRef = journeyPattern.getRouteRef().getRef();
        return routeById.lookup(routeRef);
      }
    }
    return null;
  }

  private org.opentripplanner.transit.model.network.Route resolveRoute(
    ServiceJourney serviceJourney
  ) {
    String lineRef = null;
    // Check for direct connection to Line
    JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

    if (lineRefStruct != null) {
      // Connect to Line referenced directly from ServiceJourney
      lineRef = lineRefStruct.getValue().getRef();
    } else if (serviceJourney.getJourneyPatternRef() != null) {
      // Connect to Line referenced through JourneyPattern->Route
      JourneyPattern journeyPattern = journeyPatternsById.lookup(
        serviceJourney.getJourneyPatternRef().getValue().getRef()
      );
      String routeRef = journeyPattern.getRouteRef().getRef();
      lineRef = routeById.lookup(routeRef).getLineRef().getValue().getRef();
    }
    org.opentripplanner.transit.model.network.Route route = otpRouteById.get(
      idFactory.createId(lineRef)
    );

    if (route == null) {
      LOG.warn(
        "Unable to link ServiceJourney to Route. ServiceJourney id: " +
        serviceJourney.getId() +
        ", Line ref: " +
        lineRef
      );
    }
    return route;
  }

  @Nullable
  private Operator findOperator(ServiceJourney serviceJourney) {
    var opeRef = serviceJourney.getOperatorRef();
    if (opeRef == null) {
      return null;
    }
    return operatorsById.get(idFactory.createId(opeRef.getRef()));
  }
}
