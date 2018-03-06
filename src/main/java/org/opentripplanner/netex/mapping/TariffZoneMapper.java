package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.TariffZone;

public class TariffZoneMapper {
    public TariffZone mapTariffZone(org.rutebanken.netex.model.TariffZone tariffZone) {
        TariffZone otpTariffZone = new TariffZone();
        otpTariffZone.setId(AgencyAndIdFactory.createAgencyAndId(tariffZone.getId()));
        otpTariffZone.setName(tariffZone.getName().getValue());
        return otpTariffZone;
    }
}
