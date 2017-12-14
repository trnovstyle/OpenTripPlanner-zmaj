package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.model.Trip;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;

/**
 * Agency id must be added when the stop is related to a line
 */

public class TripMapper {

    public Trip mapServiceJourney(ServiceJourney serviceJourney, OtpTransitBuilder gtfsDao, NetexDao netexDao){

        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

        String lineRef = null;
        if(lineRefStruct != null){
            lineRef = lineRefStruct.getValue().getRef();
        }else if(serviceJourney.getJourneyPatternRef() != null){
            JourneyPattern journeyPattern = netexDao.lookupJourneyPatternById(serviceJourney.getJourneyPatternRef().getValue().getRef());
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = netexDao.lookupRouteById(routeRef).getLineRef().getValue().getRef();
        }

        Trip trip = new Trip();
        trip.setId(AgencyAndIdFactory.createAgencyAndId(serviceJourney.getId()));

        trip.setRoute(gtfsDao.getRoutes().get(AgencyAndIdFactory.createAgencyAndId(lineRef)));

        String serviceId = ServiceIdMapper.mapToServiceId(serviceJourney.getDayTypes());

        // Add all unique service ids to map. Used when mapping calendars later.
        netexDao.addCalendarServiceId(serviceId);

        trip.setServiceId(AgencyAndIdFactory.createAgencyAndId(serviceId));

        if (serviceJourney.getPrivateCode() != null) {
            trip.setTripShortName(serviceJourney.getPrivateCode().getValue());
        }

        // Temp fix to prevent frontend from breaking
        if (trip.getTripShortName() == null) {
            trip.setTripShortName("");
        }

        return trip;
    }
}
