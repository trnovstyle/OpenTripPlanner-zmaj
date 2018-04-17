package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Branding;

public class BrandingMapper {

    public Branding mapBranding(org.rutebanken.netex.model.Branding netexBranding) {
        Branding branding = new Branding();
        branding.setId(AgencyAndIdFactory.createAgencyAndId(netexBranding.getId()));
        if (netexBranding.getName() != null) {
            branding.setName(netexBranding.getName().getValue());
        }
        if (netexBranding.getDescription() != null) {
            branding.setDescription(netexBranding.getDescription().getValue());
        }
        branding.setUrl(netexBranding.getUrl());
        branding.setImage(netexBranding.getImage());
        return branding;

    }


}
