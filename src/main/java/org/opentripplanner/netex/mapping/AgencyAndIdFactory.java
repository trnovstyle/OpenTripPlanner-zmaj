package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;

public class AgencyAndIdFactory {
    private static String agencyId = "NETEX_AGENCY_ID_NOT_SET";

    public static AgencyAndId createAgencyAndId(String netexId) {
        return new AgencyAndId(agencyId, netexId);
    }

    public static void setAgencyId(String agencyId) {
        AgencyAndIdFactory.agencyId = agencyId;
    }
}
