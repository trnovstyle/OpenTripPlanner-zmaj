package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Operator;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.rutebanken.netex.model.ContactStructure;

public class OperatorMapper {
    public Operator map(org.rutebanken.netex.model.Operator source, OtpTransitBuilder transitBuilder){
        Operator target = new Operator();

        target.setId(AgencyAndIdFactory.createAgencyAndId(source.getId()));
        target.setName(source.getName().getValue());
        if (source.getBrandingRef() != null) {
            target.setBranding(transitBuilder.getBrandingById().get(AgencyAndIdFactory.createAgencyAndId(source.getBrandingRef().getRef())));
        }
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
