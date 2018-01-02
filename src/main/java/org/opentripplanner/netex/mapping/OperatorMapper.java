package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Operator;
import org.rutebanken.netex.model.ContactStructure;

public class OperatorMapper {
    public Operator map(org.rutebanken.netex.model.Operator source){
        Operator target = new Operator();

        target.setId(AgencyAndIdFactory.createAgencyAndId(source.getId()));
        target.setName(source.getName().getValue());

        mapContactDetails(source.getContactDetails(), target);

        return target;
    }

    private void mapContactDetails(ContactStructure contactDetails, Operator target) {
        if(contactDetails == null) {
            return;
        }
        target.setUrl(contactDetails.getUrl());
        target.setPhone(contactDetails.getPhone());
    }
}
