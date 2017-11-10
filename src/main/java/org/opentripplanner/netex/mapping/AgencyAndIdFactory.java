package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;

class AgencyAndIdFactory {
    static AgencyAndId createAgencyAndId(String netexId) {
        return new AgencyAndId("RB", netexId);
    }
}
