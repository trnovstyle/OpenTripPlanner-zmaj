package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.rutebanken.netex.model.Authority;

public class AuthorityToAgencyMapper {
    public Agency mapAgency(Authority source, String timeZone){
        Agency target = new Agency();

        target.setId(source.getId());
        target.setName(source.getName().getValue());
        target.setTimezone(timeZone);

        if (source.getContactDetails() != null) {
            target.setUrl(source.getContactDetails().getUrl());
            target.setPhone(source.getContactDetails().getPhone());
        }
        return target;
    }

    public Agency getDefaultAgency(String timeZone){
        Agency agency = new Agency();
        agency.setId("N/A");
        agency.setName("N/A");
        agency.setTimezone(timeZone);
        agency.setUrl("N/A");
        agency.setPhone("N/A");
        return agency;
    }
}
