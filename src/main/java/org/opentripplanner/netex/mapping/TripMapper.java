package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LineRefStructure;
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
        trip.setWheelchairAccessible(1);
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

        return trip;
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
