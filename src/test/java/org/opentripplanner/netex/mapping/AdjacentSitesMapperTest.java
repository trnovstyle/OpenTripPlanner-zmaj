package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.SiteRefStructure;
import org.rutebanken.netex.model.SiteRefs_RelStructure;

import javax.xml.bind.JAXBElement;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;


public class AdjacentSitesMapperTest {

    private static final String NSR_STOP_PLACE = "NSR:StopPlace:1";
    private static final String NSR_STOP_PLACE_2 = "NSR:StopPlace:2";

    private ObjectFactory netexObjectFactory = new ObjectFactory();

    private AdjacentSitesMapper adjacentSitesMapper = new AdjacentSitesMapper();

    @Test
    public void mapAdjacentSite() {
        SiteRefs_RelStructure netexSiteRefsStructure = createNetexSiteRefsStructure(NSR_STOP_PLACE);
        Collection<String> mappedAdjacentValues = adjacentSitesMapper.mapAdjacentSites(netexSiteRefsStructure);
        assertThat(mappedAdjacentValues, hasItem(NSR_STOP_PLACE));
    }

    @Test
    public void mapAdjacentSitesIgnoringDuplicates() {
        SiteRefs_RelStructure netexSiteRefsStructure = createNetexSiteRefsStructure(NSR_STOP_PLACE, NSR_STOP_PLACE_2, NSR_STOP_PLACE);
        Collection<String> mappedAdjacentValues = adjacentSitesMapper.mapAdjacentSites(netexSiteRefsStructure);
        assertThat(mappedAdjacentValues, hasItem(NSR_STOP_PLACE));
        assertThat(mappedAdjacentValues, hasItem(NSR_STOP_PLACE_2));
        assertThat(mappedAdjacentValues.size(), is(2));
    }

    @Test
    public void mapAdjacentSitesHandleNullValue() {
        Collection<String> mappedAdjacentValues = adjacentSitesMapper.mapAdjacentSites(null);
        assertThat(mappedAdjacentValues.size(), is(0));
    }

    @Test
    public void mapAdjacentSitesNullValueJaxbElement() {
        SiteRefs_RelStructure netexSiteRefsStructure = createNetexSiteRefsStructure();
        netexSiteRefsStructure.getSiteRef().add(netexObjectFactory.createSiteRef(null));
        Collection<String> mappedAdjacentValues = adjacentSitesMapper.mapAdjacentSites(netexSiteRefsStructure);
        assertEquals("Adjacent sites should be empty", mappedAdjacentValues.isEmpty(), true);
    }

    private SiteRefs_RelStructure createNetexSiteRefsStructure(String... references) {
        SiteRefs_RelStructure siteRefs_relStructure = new SiteRefs_RelStructure();
        for (String reference : references) {
            SiteRefStructure siteRefStructure = new SiteRefStructure();
            siteRefStructure.setRef(reference);
            JAXBElement<SiteRefStructure> jaxbSiteRefStructure = netexObjectFactory.createSiteRef(siteRefStructure);
            siteRefs_relStructure.getSiteRef().add(jaxbSiteRefStructure);
        }
        return siteRefs_relStructure;
    }

}