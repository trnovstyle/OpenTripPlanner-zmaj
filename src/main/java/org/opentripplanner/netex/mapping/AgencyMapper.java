package org.opentripplanner.netex.mapping;

import com.google.common.base.Joiner;
import org.opentripplanner.model.Agency;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

public class AgencyMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    public Agency mapAgency(Authority authority, String timeZone){
        Agency agency = new Agency();
        agency.setId(authority.getId());
        agency.setName(authority.getName().getValue());
        agency.setTimezone(timeZone);
        if (authority.getContactDetails() != null) {
            agency.setUrl(authority.getContactDetails().getUrl());
            agency.setPhone(authority.getContactDetails().getPhone());
        }
        return agency;
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
