package org.opentripplanner.updater;

import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.Trip;
import uk.org.siri.siri20.CourseOfJourneyRefStructure;
import uk.org.siri.siri20.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri20.VehicleActivityStructure;

import java.util.Set;

public class SiriFuzzyTripMatcherTest extends GtfsTest {

    private final boolean forceCacheRebuild = true;

    public void testMatch() throws Exception {
        SiriFuzzyTripMatcher matcher = new SiriFuzzyTripMatcher(graph.index, forceCacheRebuild);

        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = new VehicleActivityStructure.MonitoredVehicleJourney();
        FramedVehicleJourneyRefStructure framedVehicleJourneyRef = new FramedVehicleJourneyRefStructure();
        framedVehicleJourneyRef.setDatedVehicleJourneyRef("10W1020");
        monitoredVehicleJourney.setFramedVehicleJourneyRef(framedVehicleJourneyRef);


        VehicleActivityStructure activity = new VehicleActivityStructure();
        activity.setMonitoredVehicleJourney(monitoredVehicleJourney);
        Set<Trip> match = matcher.match(activity);

        assertTrue(match.size() == 1);

        Trip trip = match.iterator().next();
        assertNotNull(trip);
        assertTrue(trip.getId().getId().equals(framedVehicleJourneyRef.getDatedVehicleJourneyRef()));

    }

    public void testNoMatch() throws Exception {
        SiriFuzzyTripMatcher matcher = new SiriFuzzyTripMatcher(graph.index, forceCacheRebuild);

        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = new VehicleActivityStructure.MonitoredVehicleJourney();
        CourseOfJourneyRefStructure courseOfJourney = new CourseOfJourneyRefStructure();
        courseOfJourney.setValue("FOOBAR");
        monitoredVehicleJourney.setCourseOfJourneyRef(courseOfJourney);


        VehicleActivityStructure activity = new VehicleActivityStructure();
        activity.setMonitoredVehicleJourney(monitoredVehicleJourney);
        Set<Trip> match = matcher.match(activity);

        assertTrue(match == null || match.isEmpty());

    }

    @Override
    public String getFeedName() {
        return "google_transit.zip";
    }
}
