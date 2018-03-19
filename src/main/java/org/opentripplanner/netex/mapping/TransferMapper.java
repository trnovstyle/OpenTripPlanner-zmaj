package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TransferMapper.class);

    public Transfer mapTransfer(ServiceJourneyInterchange interchange,
            OtpTransitBuilder transitBuilder, NetexDao netexDao) {
        Transfer transfer = new Transfer();

        transfer.setTransferType(1);

        if (interchange.isStaySeated() != null) {
            transfer.setStaySeated(interchange.isStaySeated());
        }
        if (interchange.isGuaranteed() != null) {
            transfer.setGuaranteed(interchange.isGuaranteed());
        }

        String fromStopId = netexDao.quayIdByStopPointRef.lookup(interchange.getFromPointRef().getRef());
        String toStopId = netexDao.quayIdByStopPointRef.lookup(interchange.getToPointRef().getRef());

        transfer.setFromStop(
                transitBuilder.getStops().get(AgencyAndIdFactory.createAgencyAndId(fromStopId)));
        transfer.setToStop(
                transitBuilder.getStops().get(AgencyAndIdFactory.createAgencyAndId(toStopId)));

        transfer.setFromTrip(transitBuilder.getTrips().get(AgencyAndIdFactory
                .createAgencyAndId(interchange.getFromJourneyRef().getRef())));
        transfer.setToTrip(transitBuilder.getTrips()
                .get(AgencyAndIdFactory.createAgencyAndId(interchange.getToJourneyRef().getRef())));

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null) {
            LOG.warn("Trips not found for transfer " + interchange.getId());
        }

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null
                || transfer.getToStop() == null || transfer.getFromTrip() == null) {
            return null;
        }

        transfer.setFromRoute(transfer.getFromTrip().getRoute());
        transfer.setToRoute(transfer.getToTrip().getRoute());

        LOG.info(transfer.toString());

        return transfer;
    }
}
