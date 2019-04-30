package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.BookingArrangement;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Agency id must be added when the stop is related to a line
 */

public class TripMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TripMapper.class);

    private KeyValueMapper keyValueMapper = new KeyValueMapper();
    private TransportModeMapper transportModeMapper = new TransportModeMapper();
    private BookingArrangementMapper bookingArrangementMapper = new BookingArrangementMapper();

    public Trip mapServiceJourney(ServiceJourney serviceJourney, OtpTransitBuilder gtfsDao, NetexDao netexDao, String defaultFlexMaxTravelTime){

        Line_VersionStructure line = lineFromServiceJourney(serviceJourney, netexDao);

        Trip trip = new Trip();
        trip.setId(AgencyAndIdFactory.createAgencyAndId(serviceJourney.getId()));

        trip.setRoute(gtfsDao.getRoutes().get(AgencyAndIdFactory.createAgencyAndId(line.getId())));
        if (serviceJourney.getOperatorRef() != null) {
            Operator operator = gtfsDao.getOperatorsById().get(AgencyAndIdFactory.createAgencyAndId(serviceJourney.getOperatorRef().getRef()));
            trip.setTripOperator(operator);
        }

        String serviceId = ServiceIdMapper.mapToServiceId(serviceJourney.getDayTypes());

        // Add all unique service ids to map. Used when mapping calendars later.
        netexDao.addCalendarServiceId(serviceId);

        trip.setServiceId(AgencyAndIdFactory.createAgencyAndId(serviceId));

        if (serviceJourney.getPrivateCode() != null) {
            trip.setTripPrivateCode(serviceJourney.getPrivateCode().getValue());
        }

        if (serviceJourney.getPublicCode() != null) {
            trip.setTripPublicCode(serviceJourney.getPublicCode());
        }

        // Temp fix to prevent frontend from breaking
        if (trip.getTripPrivateCode() == null) {
            trip.setTripPrivateCode("");
        }

        // Temp fix to prevent frontend from breaking
        if (trip.getTripPublicCode() == null) {
            trip.setTripPublicCode("");
        }

        // Temp fix to prevent frontend from breaking
        if (trip.getTripShortName() == null) {
            trip.setTripShortName("");
        }
        trip.setKeyValues(keyValueMapper.mapKeyValues(serviceJourney.getKeyList()));
        trip.setWheelchairAccessible(0); // noInformation
        trip.setServiceAlteration(mapServiceAlteration(serviceJourney.getServiceAlteration()));
        trip.setTransportSubmode(transportModeMapper.getTransportSubmode(serviceJourney.getTransportSubmode()));
        if (trip.getTransportSubmode()==null) {
            trip.setTransportSubmode(trip.getRoute().getTransportSubmode());
        }

        // Map to right shapeId
        JourneyPattern journeyPattern = netexDao.journeyPatternsById.lookup(serviceJourney.getJourneyPatternRef().getValue().getRef());
        AgencyAndId serviceLinkId = AgencyAndIdFactory.createAgencyAndId(journeyPattern.getId().replace("JourneyPattern", "ServiceLink"));
        if (gtfsDao.getShapePoints().get(serviceLinkId) != null) {
            trip.setShapeId(serviceLinkId);
        }

        // Map to default until support is added in NeTEx
        if (line instanceof FlexibleLine) {
            trip.setDrtMaxTravelTime(defaultFlexMaxTravelTime);
        }

        if (serviceJourney.getFlexibleServiceProperties()!=null) {
            mapFlexibleServicePropertiesProperties(serviceJourney.getFlexibleServiceProperties(), trip);
        }

        if (journeyPattern.getRouteRef() != null) {
            Route route = netexDao.routeById.lookup(journeyPattern.getRouteRef().getRef());
            if (route.getDirectionType() == null) {
                trip.setDirectionId("-1");
            } else {
                switch (route.getDirectionType()) {
                    case OUTBOUND:
                        trip.setDirectionId("0");
                        break;
                    case INBOUND:
                        trip.setDirectionId("1");
                        break;
                    case CLOCKWISE:
                        trip.setDirectionId("2");
                        break;
                    case ANTICLOCKWISE:
                        trip.setDirectionId("3");
                        break;
                }
            }
        }

        return trip;
    }

    private void mapFlexibleServicePropertiesProperties(FlexibleServiceProperties flexibleServiceProperties, Trip otpTrip) {
        if (flexibleServiceProperties.getFlexibleServiceType() != null) {
            otpTrip.setFlexibleTripType(Trip.FlexibleTripTypeEnum.valueOf(flexibleServiceProperties.getFlexibleServiceType().value()));
        }
        BookingArrangement otpBookingArrangement = bookingArrangementMapper.mapBookingArrangement(flexibleServiceProperties.getBookingContact(), flexibleServiceProperties.getBookingNote(),
                flexibleServiceProperties.getBookingAccess(), flexibleServiceProperties.getBookWhen(), flexibleServiceProperties.getBuyWhen(), flexibleServiceProperties.getBookingMethods(),
                flexibleServiceProperties.getMinimumBookingPeriod(), flexibleServiceProperties.getLatestBookingTime());
        otpTrip.setBookingArrangements(otpBookingArrangement);
    }

    private Trip.ServiceAlteration mapServiceAlteration(ServiceAlterationEnumeration netexValue) {
        if (netexValue == null) {
            return null;
        }
        try {
            return Trip.ServiceAlteration.valueOf(netexValue.value());
        } catch (Exception e) {
            LOG.warn("Unable to map unknown ServiceAlterationEnumeration value from NeTEx:" + netexValue);
        }
        return null;
    }

    public static Line_VersionStructure lineFromServiceJourney(ServiceJourney serviceJourney, NetexDao netexDao) {
        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();
        String lineRef = null;
        if(lineRefStruct != null){
            lineRef = lineRefStruct.getValue().getRef();
        }else if(serviceJourney.getJourneyPatternRef() != null){
            JourneyPattern journeyPattern = netexDao.journeyPatternsById.lookup(serviceJourney.getJourneyPatternRef().getValue().getRef());
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = netexDao.routeById.lookup(routeRef).getLineRef().getValue().getRef();
        }
        return netexDao.lineById.lookup(lineRef);
    }

    /**
     *
     * @param trip Trip to be modified
     * @param bookingArrangements Booking arrangements for StopPointsInJourneyPatterns for this trip
     *
     * This maps minimumBookingPeriod from one of three levels to trip. Order of precedence is StopPointInJourneyPattern -
     * ServiceJourney - Line.
     */

    public void setdrtAdvanceBookMin(Trip trip, List<BookingArrangement> bookingArrangements) {
        if (bookingArrangements.stream().anyMatch(b -> b != null && b.getMinimumBookingPeriod() != null)) {
            trip.setDrtAdvanceBookMin(durationToMins(bookingArrangements.stream().filter(b -> b.getMinimumBookingPeriod() != null).findFirst()
                    .get().getMinimumBookingPeriod()));
        } else if (trip.getBookingArrangements() != null && trip.getBookingArrangements().getMinimumBookingPeriod() != null) {
            trip.setDrtAdvanceBookMin(durationToMins(trip.getBookingArrangements().getMinimumBookingPeriod()));
        } else if (trip.getRoute().getBookingArrangements() != null
                && trip.getRoute().getBookingArrangements().getMinimumBookingPeriod() != null) {
            trip.setDrtAdvanceBookMin(durationToMins(trip.getRoute().getBookingArrangements().getMinimumBookingPeriod()));
        }
    }

    private double durationToMins(Duration duration) {
        return duration.get(ChronoUnit.SECONDS) / 60.0;
    }
}
