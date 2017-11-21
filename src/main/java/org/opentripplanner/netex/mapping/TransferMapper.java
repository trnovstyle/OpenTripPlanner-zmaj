package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.impl.OtpTransitDaoBuilder;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rutebanken.netex.model.*;

public class TransferMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    public Transfer mapTransfer(ServiceJourneyInterchange interchange, OtpTransitDaoBuilder transitBuilder, NetexDao netexDao){
        Transfer transfer = new Transfer();

        transfer.setTransferType(1);

        String fromStopId = netexDao.getStopPointQuayMap().get(interchange.getFromPointRef().getRef());
        String toStopId = netexDao.getStopPointQuayMap().get(interchange.getToPointRef().getRef());

        transfer.setFromStop(transitBuilder.getStops().get(AgencyAndIdFactory.getAgencyAndId(fromStopId)));
        transfer.setToStop(transitBuilder.getStops().get(AgencyAndIdFactory.getAgencyAndId(toStopId)));

        transfer.setFromTrip(transitBuilder.getTrips().get(AgencyAndIdFactory.getAgencyAndId(interchange.getFromJourneyRef().getRef())));
        transfer.setToTrip(transitBuilder.getTrips().get(AgencyAndIdFactory
                .getAgencyAndId(interchange.getToJourneyRef().getRef())));

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null) {
            LOG.warn("Trips not found for transfer " +  interchange.getId());
        }

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null || transfer.getToStop() == null || transfer.getFromTrip() == null) {
            return null;
        }

        transfer.setFromRoute(transfer.getFromTrip().getRoute());
        transfer.setToRoute(transfer.getToTrip().getRoute());

        return transfer;
    }
}
