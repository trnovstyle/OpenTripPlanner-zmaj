package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.BookingArrangement;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.DirectionTypeEnumeration;
import org.rutebanken.netex.model.FlexibleServiceProperties;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;

/**
 * Agency id must be added when the stop is related to a line
 */

public class TripMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TripMapper.class);

    private KeyValueMapper keyValueMapper = new KeyValueMapper();
    private TransportModeMapper transportModeMapper = new TransportModeMapper();
    private BookingArrangementMapper bookingArrangementMapper = new BookingArrangementMapper();

    public Trip mapServiceJourney(ServiceJourney serviceJourney, OtpTransitBuilder gtfsDao, NetexDao netexDao){

        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

        String lineRef = null;
        if(lineRefStruct != null){
            lineRef = lineRefStruct.getValue().getRef();
        }else if(serviceJourney.getJourneyPatternRef() != null){
            JourneyPattern journeyPattern = netexDao.journeyPatternsById.lookup(serviceJourney.getJourneyPatternRef().getValue().getRef());
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = netexDao.routeById.lookup(routeRef).getLineRef().getValue().getRef();
        }

        Trip trip = new Trip();
        trip.setId(AgencyAndIdFactory.createAgencyAndId(serviceJourney.getId()));

        trip.setRoute(gtfsDao.getRoutes().get(AgencyAndIdFactory.createAgencyAndId(lineRef)));

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

        if (serviceJourney.getFlexibleServiceProperties()!=null) {
            mapFlexibleServicePropertiesProperties(serviceJourney.getFlexibleServiceProperties(), trip);
        }

        if (serviceJourney.getRouteRef() != null) {
            Route route = netexDao.routeById.lookup(serviceJourney.getRouteRef().getRef());
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
}
