package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;

public class AgencyAndIdFactory {
    private static String agencyId = "";

    public static AgencyAndId getAgencyAndId(String netexId) {
        return new AgencyAndId(agencyId, netexId);
    }

    public static void setAgencyId(String agencyId) {
        AgencyAndIdFactory.agencyId = agencyId;
    }
}
