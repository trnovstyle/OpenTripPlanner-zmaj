package org.opentripplanner.netex.mapping;

import com.google.common.base.Joiner;
import org.opentripplanner.model.Agency;
import org.rutebanken.netex.model.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

public class AgencyMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    public Agency mapAgency(Operator operator, String timeZone){
        Agency agency = new Agency();
        agency.setId(operator.getId());
        agency.setName(operator.getName().getValue());
        agency.setTimezone(timeZone);
        if (operator.getCustomerServiceContactDetails() != null) {
            agency.setUrl(operator.getCustomerServiceContactDetails().getUrl());
            agency.setPhone(operator.getCustomerServiceContactDetails().getPhone());
        }
        return agency;
    }

    public Agency combineAndMapAgency(Collection<Operator> operators, String timeZone){
        Agency compositeAgency = new Agency();

        Collection<String> ids = operators.stream().map(o -> o.getId()).collect(Collectors.toList());
        Collection<String> names = operators.stream().map(o -> o.getName().getValue()).collect(Collectors.toList());

        compositeAgency.setId(Joiner.on("+").join(ids));
        compositeAgency.setName(Joiner.on("+").join(names));
        compositeAgency.setUrl("");
        compositeAgency.setTimezone(timeZone);

        return compositeAgency;
    }
}
