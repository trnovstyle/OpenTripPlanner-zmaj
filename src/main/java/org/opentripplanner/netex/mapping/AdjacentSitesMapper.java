package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.SiteRefStructure;
import org.rutebanken.netex.model.SiteRefs_RelStructure;

import javax.xml.bind.JAXBElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class AdjacentSitesMapper {

    public Collection<String> mapAdjacentSites(SiteRefs_RelStructure siteRefsRelStructure) {

        if (siteRefsRelStructure != null) {
            return siteRefsRelStructure.getSiteRef()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(JAXBElement::getValue)
                    .filter(Objects::nonNull)
                    .map(SiteRefStructure::getRef)
                    .collect(Collectors.toSet());
        }

        return Arrays.asList();
    }

}
