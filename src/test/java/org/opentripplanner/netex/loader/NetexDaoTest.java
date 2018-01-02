package org.opentripplanner.netex.loader;

import org.junit.Before;
import org.junit.Test;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPlace;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (01.12.2017)
 */
public class NetexDaoTest {

    private static final String ID = "ID:A";
    private static final String ID_2 = "ID:B";
    private static final String REF = "REF";
    private static final String REF_2 = "REF-2";
    private static final String TIMEZONE_NO = "Oslo/Norway";
    private static final String TIMEZONE_PST = "PST";

    private NetexDao root;
    private NetexDao child;

    @Before
    public void setup() {
        root = new NetexDao();
        child = new NetexDao(root);
    }

    @Test
    public void lookupStopsById() throws Exception {
        StopPlace stopPlaceA = stopPlace(ID, null);
        StopPlace stopPlaceB = stopPlace(ID, "image_1");

        assertEquals(emptyList(), root.stopPlaceById.lookup(ID));
        assertEquals(emptyList(), child.stopPlaceById.lookup(ID));

        root.stopPlaceById.add(stopPlaceA);

        assertEquals(singletonList(stopPlaceA), root.stopPlaceById.lookup(ID));
        assertEquals(singletonList(stopPlaceA), child.stopPlaceById.lookup(ID));

        child.stopPlaceById.add(stopPlaceB);

        assertEquals(singletonList(stopPlaceB), child.stopPlaceById.lookup(ID));
    }

    @Test
    public void lookupQuayIdByStopPointRef() {
        assertNull(root.quayIdByStopPointRef.lookup(ID));
        assertNull(child.quayIdByStopPointRef.lookup(ID));

        root.quayIdByStopPointRef.add(ID, REF);

        assertEquals(REF, root.quayIdByStopPointRef.lookup(ID));
        assertEquals(REF, child.quayIdByStopPointRef.lookup(ID));

        child.quayIdByStopPointRef.add(ID, REF_2);

        assertEquals(REF_2, child.quayIdByStopPointRef.lookup(ID));
    }

    @Test
    public void lookupQuayById() {
        Quay quayA = quay(ID, null);
        Quay quayB = quay(ID, "image_1");

        assertTrue(root.quayById.lookup(ID).isEmpty());
        assertTrue(child.quayById.lookup(ID).isEmpty());

        root.quayById.add(quayA);

        assertEquals(singletonList(quayA), root.quayById.lookup(ID));
        assertEquals(singletonList(quayA), child.quayById.lookup(ID));

        child.quayById.add(quayB);

        assertEquals(singletonList(quayB), child.quayById.lookup(ID));
    }

    @Test
    public void lookupDayTypeAvailable() throws Exception {
        assertNull(root.dayTypeAvailable.lookup(ID));
        assertNull(child.dayTypeAvailable.lookup(ID));

        root.dayTypeAvailable.add(ID, TRUE);

        assertEquals(TRUE, root.dayTypeAvailable.lookup(ID));
        assertEquals(TRUE, child.dayTypeAvailable.lookup(ID));

        child.dayTypeAvailable.add(ID, FALSE);

        assertEquals(FALSE, child.dayTypeAvailable.lookup(ID));
    }

    @Test
    public void lookupDayTypeAssignment() throws Exception {
        DayTypeAssignment dta = dayTypeAssignment(ID, REF);
        DayTypeAssignment dta2 = dayTypeAssignment(ID, REF_2);

        assertEquals(emptyList(), root.dayTypeAssignmentByDayTypeId.lookup(ID));
        assertEquals(emptyList(), child.dayTypeAssignmentByDayTypeId.lookup(ID));

        root.dayTypeAssignmentByDayTypeId.add(ID, dta);

        assertEquals(singletonList(dta), root.dayTypeAssignmentByDayTypeId.lookup(ID));
        assertEquals(singletonList(dta), child.dayTypeAssignmentByDayTypeId.lookup(ID));

        child.dayTypeAssignmentByDayTypeId.add(ID, dta2);

        assertEquals(singletonList(dta2), child.dayTypeAssignmentByDayTypeId.lookup(ID));
    }

    @Test
    public void getTimeZone() throws Exception {
        assertNull(root.getTimeZone());
        assertNull(child.getTimeZone());

        root.setTimeZone(TIMEZONE_NO);

        assertEquals(TIMEZONE_NO, root.getTimeZone());
        assertEquals(TIMEZONE_NO, child.getTimeZone());

        child.setTimeZone(TIMEZONE_PST);

        assertEquals(TIMEZONE_PST, child.getTimeZone());
    }

    @Test
    public void lookupServiceJourneysById() throws Exception {
        ServiceJourney value = new ServiceJourney();
        root.serviceJourneyByPatternId.add(ID, value);
        assertEquals(singletonList(value), child.serviceJourneyByPatternId.lookup(ID));
    }

    @Test
    public void lookupJourneyPatternById() throws Exception {
        JourneyPattern value = new JourneyPattern();
        value.withId(ID);
        root.journeyPatternsById.add(value);
        assertEquals(value, child.journeyPatternsById.lookup(ID));
    }

    @Test
    public void lookupRouteById() throws Exception {
        Route value = new Route();
        value.withId(ID);
        root.routeById.add(value);
        assertEquals(value, child.routeById.lookup(ID));
    }

    @Test
    public void lookupOperatingPeriodById() throws Exception {
        OperatingPeriod value = new OperatingPeriod();
        value.withId(ID);

        assertFalse(child.operatingPeriodById.containsKey(ID));

        root.operatingPeriodById.add(value);

        assertEquals(value, child.operatingPeriodById.lookup(ID));
        assertTrue(child.operatingPeriodById.containsKey(ID));
    }

    @Test
    public void operatingPeriodExist() throws Exception {
        Route value = new Route();
        value.withId(ID);
        root.routeById.add(value);
    }


    /* private methods */

    private static StopPlace stopPlace(String id, String image) {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setId(id);
        stopPlace.withImage(image);
        return stopPlace;
    }

    private static Quay quay(String id, String image) {
        Quay quay = new Quay();
        quay.setId(id);
        quay.withImage(image);
        return quay;
    }

    private static DayTypeAssignment dayTypeAssignment(String id, String dataSourceRef) {
        DayTypeAssignment value = new DayTypeAssignment();
        value.setId(id);
        value.withDataSourceRef(dataSourceRef);
        return value;
    }
}
