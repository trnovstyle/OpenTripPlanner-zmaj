package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.model.modes.TransitModeService;
import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;
import org.opentripplanner.netex.mapping.calendar.CalendarServiceBuilder;
import org.opentripplanner.netex.mapping.calendar.DatedServiceJourneyMapper;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * This is the ROOT mapper to map from the Netex domin model into the OTP internal model. This class delegates to
 * type/argegate specific mappers and take the result from each such mapper and add the result to the
 * {@link OtpTransitServiceBuilder}.
 * </p>
 * <p>
 * The transit builder is updated with the new OTP model entities, holding ALL entities parsed so fare including
 * previous Netex files in the same bundle. This enable the mapping code to make direct references between entities
 * in the OTP domain model.
 * </p>
 */
public class NetexMapper {
    private static final Logger LOG = LoggerFactory.getLogger(NetexMapper.class);

    private static final int LEVEL_SHARED = 0;
    private static final int LEVEL_GROUP = 1;

    private final FeedScopedIdFactory idFactory;
    private final OtpTransitServiceBuilder transitBuilder;
    private final TransitModeService transitModeService;
    private final Deduplicator deduplicator;
    private final DataImportIssueStore issueStore;
    private final Multimap<String, Station> stationsByMultiModalStationRfs = ArrayListMultimap.create();
    private final CalendarServiceBuilder calendarServiceBuilder;
    private final TripCalendarBuilder tripCalendarBuilder;
    private final Set<String> ferryIdsNotAllowedForBicycle;

    /** Map entries that cross reference entities within a group/operator, for example Interchanges. */
    private GroupNetexMapper groupMapper;

    private NetexEntityIndexReadOnlyView currentNetexIndex;
    private int level = LEVEL_SHARED;

    /**
     * This is needed to assign a notice to a stop time. It is not part of the target
     * OTPTransitService, so we need to temporally cash this here.
     */
    private final Map<String, StopTime> stopTimesByNetexId = new HashMap<>();


    public NetexMapper(
            OtpTransitServiceBuilder transitBuilder,
            String feedId,
            Deduplicator deduplicator,
            DataImportIssueStore issueStore,
            Set<String> ferryIdsNotAllowedForBicycle,
            TransitModeService transitModeService
    ) {
        this.transitBuilder = transitBuilder;
        this.deduplicator = deduplicator;
        this.idFactory = new FeedScopedIdFactory(feedId);
        this.transitModeService = transitModeService;
        this.issueStore = issueStore;
        this.ferryIdsNotAllowedForBicycle = ferryIdsNotAllowedForBicycle;
        this.calendarServiceBuilder = new CalendarServiceBuilder(idFactory);
        this.tripCalendarBuilder = new TripCalendarBuilder(this.calendarServiceBuilder, issueStore);
    }

    /**
     * Prepare to for mapping of a new sub-level of entities(shared-files, shared-group-files and
     * group-files). This is a life-cycle method used to notify this class that a new dataset is
     * about to be processed. Any existing intermediate state must be saved, so it can be accessed
     * during the next call to {@link #mapNetexToOtp(NetexEntityIndexReadOnlyView)} and after.
     */
    public NetexMapper push() {
        ++level;
        this.tripCalendarBuilder.push();
        setupGroupMapping();
        return this;
    }

    /**
     * It is now safe to discard any intermediate state generated by the last call to
     * {@link #mapNetexToOtp(NetexEntityIndexReadOnlyView)}.
     */
    public NetexMapper pop() {
        performGroupMapping();
        this.tripCalendarBuilder.pop();
        --level;
        return this;
    }

    /**
     * Any post processing step in the mapping is done in this method. The method is called
     * ONCE after all other mapping is complete. Note! Hierarchical data structures are not
     * accessible any more.
     */
    public void finnishUp() {
        // Add Calendar data created during the mapping of dayTypes, dayTypeAssignments,
        // datedServiceJourney and ServiceJourneys
        transitBuilder.getCalendarDates().addAll(
                calendarServiceBuilder.createServiceCalendar()
        );
    }

    /**
     * Any post processing step in the mapping is done in this method for a . The method is called
     * ONCE after all other mapping is complete. Note! Hierarchical data structures are not
     * accessible any more.
     */
    public void finnishUpGroup() {
        // Add Calendar data created during the mapping of dayTypes, dayTypeAssignments,
        // datedServiceJourney and ServiceJourneys
        transitBuilder.getCalendarDates().addAll(
                calendarServiceBuilder.createServiceCalendar()
        );
    }

    /**
     * <p>
     * This method mapes the last Netex file imported using the *local* entities in the
     * hierarchical {@link NetexEntityIndexReadOnlyView}.
     * </p>
     * <p>
     * Note that the order in which the elements are mapped is important. For example, if a file
     * contains Authorities, Line and Notices - they need to be mapped in that order, since
     * Route have a reference on Agency, and Notice may reference on Route.
     * </p>
     *
     * @param netexIndex The parsed Netex entities to be mapped
     */
    public void mapNetexToOtp(NetexEntityIndexReadOnlyView netexIndex) {
        // Be careful, the order matter. For example a Route has a reference to Agency; Hence Agency must be mapped
        // before Route - if both entities are defined in the same file.
        this.currentNetexIndex = netexIndex;
        mapAuthorities();
        mapOperators();
        mapShapePoints();

        // The tariffZoneMapper is used to map all currently valid zones and to map the correct
        // referenced zone in StopPlace - which may not be the most currently valid zone.
        // This is a workaround until versioned entities are supported by OTP
        var tariffZoneMapper = mapTariffZones();
        mapStopPlaceAndQuays(tariffZoneMapper);
        mapMultiModalStopPlaces();
        mapGroupsOfStopPlaces();
        mapFlexibleStopPlaces();
        mapDatedServiceJourneys();
        mapDayTypeAssignments();

        // DayType and DSJ is mapped to a service calendar and a serviceId is generated
        Map<String, FeedScopedId> serviceIds = createCalendarForServiceJourney();

        mapRoute();
        mapTripPatterns(serviceIds);
        mapNoticeAssignments();

        addEntriesToGroupMapperForPostProcessingLater();
    }

    /* PRIVATE METHODS */

    private void setupGroupMapping() {
        if(level != LEVEL_GROUP) { return; }
        this.groupMapper = new GroupNetexMapper(idFactory, issueStore, transitBuilder);
    }

    /**
     * Group mappings should be done after all individual processed files and most entities are
     * mapped. The group mapping should only be used to map entities(relations) that reference
     * elements in other files within a group(netex namespace);
     */
    private void performGroupMapping() {
        if(level != LEVEL_GROUP) { return; }
        this.groupMapper.mapGroupEntries();
        // Throw away group data and make it available for garbage collection
        this.groupMapper = null;
    }

    private void mapAuthorities() {
        AuthorityToAgencyMapper agencyMapper = new AuthorityToAgencyMapper(idFactory, currentNetexIndex.getTimeZone());
        for (Authority authority : currentNetexIndex.getAuthoritiesById().localValues()) {
            Agency agency = agencyMapper.mapAuthorityToAgency(authority);
            transitBuilder.getAgenciesById().add(agency);
        }
    }

    private void mapOperators() {
        OperatorToAgencyMapper mapper = new OperatorToAgencyMapper(idFactory);
        for (org.rutebanken.netex.model.Operator operator : currentNetexIndex.getOperatorsById().localValues()) {
            transitBuilder.getOperatorsById().add(mapper.mapOperator(operator));
        }
    }

    private void mapShapePoints() {
        ServiceLinkMapper serviceLinkMapper = new ServiceLinkMapper(idFactory, issueStore);
        for (JourneyPattern journeyPattern : currentNetexIndex.getJourneyPatternsById().localValues()) {

            Collection<ShapePoint> shapePoints = serviceLinkMapper.getShapePointsByJourneyPattern(
                journeyPattern,
                currentNetexIndex.getServiceLinkById(),
                currentNetexIndex.getQuayIdByStopPointRef(),
                currentNetexIndex.getQuayById());

            for (ShapePoint shapePoint : shapePoints) {
                transitBuilder.getShapePoints().put(shapePoint.getShapeId(), shapePoint);
            }
        }
    }

    private TariffZoneMapper mapTariffZones() {
        TariffZoneMapper tariffZoneMapper = new TariffZoneMapper(
                getStartOfPeriod(),
                idFactory,
                currentNetexIndex.getTariffZonesById()
        );
        transitBuilder.getFareZonesById().addAll(tariffZoneMapper.listAllCurrentFareZones());
        return tariffZoneMapper;
    }

    private void mapStopPlaceAndQuays(TariffZoneMapper tariffZoneMapper) {
        StopAndStationMapper stopMapper = new StopAndStationMapper(
                idFactory,
                currentNetexIndex.getQuayById(),
                tariffZoneMapper,
                issueStore
        );
        for (String stopPlaceId : currentNetexIndex.getStopPlaceById().localKeys()) {
            Collection<StopPlace> stopPlaceAllVersions = currentNetexIndex.getStopPlaceById().lookup(stopPlaceId);
            stopMapper.mapParentAndChildStops(stopPlaceAllVersions);
        }
        transitBuilder.getStops().addAll(stopMapper.resultStops);
        transitBuilder.getStations().addAll(stopMapper.resultStations);
        stationsByMultiModalStationRfs.putAll(stopMapper.resultStationByMultiModalStationRfs);

    }

    private void mapMultiModalStopPlaces() {
        MultiModalStationMapper mapper = new MultiModalStationMapper(idFactory);
        for (StopPlace multiModalStopPlace : currentNetexIndex.getMultiModalStopPlaceById().localValues()) {
            transitBuilder.getMultiModalStationsById().add(
                mapper.map(
                    multiModalStopPlace,
                    stationsByMultiModalStationRfs.get(multiModalStopPlace.getId())
                )
            );
        }
    }

    private void mapGroupsOfStopPlaces() {
        GroupOfStationsMapper groupOfStationsMapper = new GroupOfStationsMapper(
                idFactory,
                transitBuilder.getMultiModalStationsById(),
                transitBuilder.getStations()
        );
        for (GroupOfStopPlaces groupOfStopPlaces : currentNetexIndex.getGroupOfStopPlacesById().localValues()) {
            transitBuilder.getGroupsOfStationsById().add(groupOfStationsMapper.map(groupOfStopPlaces));
        }
    }

    private void mapFlexibleStopPlaces() {
        FlexStopLocationMapper flexStopLocationMapper = new FlexStopLocationMapper(idFactory, transitBuilder.getStops().values());

        for (FlexibleStopPlace flexibleStopPlace : currentNetexIndex.getFlexibleStopPlacesById().localValues()) {
            StopLocation stopLocation = flexStopLocationMapper.map(flexibleStopPlace);
            if (stopLocation instanceof FlexStopLocation) {
                transitBuilder.getLocations().add((FlexStopLocation) stopLocation);
            } else if (stopLocation instanceof FlexLocationGroup) {
                transitBuilder.getLocationGroups().add((FlexLocationGroup) stopLocation);
            }
        }
    }

    private void mapDatedServiceJourneys() {
        tripCalendarBuilder.addDatedServiceJourneys(
            currentNetexIndex.getOperatingDayById(),
            DatedServiceJourneyMapper.indexDSJBySJId(
                currentNetexIndex.getDatedServiceJourneys()
            )
        );
    }

    private void mapDayTypeAssignments() {
        tripCalendarBuilder.addDayTypeAssignments(
            currentNetexIndex.getDayTypeById(),
            currentNetexIndex.getDayTypeAssignmentByDayTypeId(),
            currentNetexIndex.getOperatingDayById(),
            currentNetexIndex.getOperatingPeriodById()
        );
    }

    private Map<String, FeedScopedId> createCalendarForServiceJourney() {
        return tripCalendarBuilder.createTripCalendar(
            currentNetexIndex.getServiceJourneyById().localValues()
        );
    }

    private void mapRoute() {
        RouteMapper routeMapper = new RouteMapper(
                idFactory,
                transitBuilder.getAgenciesById(),
                transitBuilder.getOperatorsById(),
                currentNetexIndex,
                currentNetexIndex.getTimeZone(),
                ferryIdsNotAllowedForBicycle,
                transitModeService
        );
        for (Line line : currentNetexIndex.getLineById().localValues()) {
            Route route = routeMapper.mapRoute(line);
            transitBuilder.getRoutes().add(route);
        }
        for (FlexibleLine line : currentNetexIndex.getFlexibleLineById().localValues()) {
            Route route = routeMapper.mapRoute(line);
            transitBuilder.getRoutes().add(route);
        }
    }

    private void mapTripPatterns(Map<String, FeedScopedId> serviceIds) {
        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                idFactory,
                transitBuilder.getOperatorsById(),
                transitBuilder.getStops(),
                transitBuilder.getLocations(),
                transitBuilder.getLocationGroups(),
                transitBuilder.getRoutes(),
                transitBuilder.getShapePoints().keySet(),
                currentNetexIndex.getRouteById(),
                currentNetexIndex.getJourneyPatternsById(),
                currentNetexIndex.getQuayIdByStopPointRef(),
                currentNetexIndex.getFlexibleStopPlaceByStopPointRef(),
                currentNetexIndex.getDestinationDisplayById(),
                currentNetexIndex.getServiceJourneyById(),
                currentNetexIndex.getFlexibleLineById(),
                serviceIds,
                deduplicator,
                transitModeService
        );

        for (JourneyPattern journeyPattern : currentNetexIndex.getJourneyPatternsById().localValues()) {
            TripPatternMapperResult result = tripPatternMapper.mapTripPattern(journeyPattern);

            for (Map.Entry<Trip, List<StopTime>> it : result.tripStopTimes.entrySet()) {
                transitBuilder.getStopTimesSortedByTrip().put(it.getKey(), it.getValue());
                transitBuilder.getTripsById().add(it.getKey());
            }
            for (TripPattern it : result.tripPatterns) {
                transitBuilder.getTripPatterns().put(it.getStopPattern(), it);
            }
            stopTimesByNetexId.putAll(result.stopTimeByNetexId);
            groupMapper.scheduledStopPointsIndex.putAll(result.scheduledStopPointsIndex);
        }
    }

    private void mapNoticeAssignments() {
        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                idFactory,
                currentNetexIndex.getServiceJourneyById().localValues(),
                currentNetexIndex.getNoticeById(),
                transitBuilder.getRoutes(),
                transitBuilder.getTripsById(),
                stopTimesByNetexId
        );
        for (NoticeAssignment noticeAssignment : currentNetexIndex.getNoticeAssignmentById().localValues()) {
            Multimap<TransitEntity, Notice> noticesByElementId;
            noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);
            transitBuilder.getNoticeAssignments().putAll(noticesByElementId);
        }
    }

    private void addEntriesToGroupMapperForPostProcessingLater() {
        if(level != 0) {
            groupMapper.addInterchange(
                    currentNetexIndex.getServiceJourneyInterchangeById().localValues());
        }
    }

    /**
     * The start of period is used to find the valid entities based on the current time.
     * This should probably be configurable in the future, or even better incorporate the version
     * number into the entity id, so we can operate with more than one version of an entity in OTPs
     * internal model.
     */
    private LocalDateTime getStartOfPeriod() {
        String timeZone = currentNetexIndex.getTimeZone();
        if(timeZone == null) {
            LocalDateTime time = LocalDateTime.now(ZoneId.of("UTC"));
            LOG.warn(
                    "No timezone set for the current NeTEx input data file. The import " +
                    "start-of-period is set to " + time + " UTC, used to check entity validity " +
                    "periods."
            );
            return time;

        }
        return LocalDateTime.now(ZoneId.of(timeZone));
    }
}
