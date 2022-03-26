package org.opentripplanner.transit.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

/**
 * Search for trip where first stop is not possible to board with wheelchair.
 */
public class B05_WheelchairAccess implements RaptorTestConstants {

    private final TestTransitData transitData = new TestTransitData();
    private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
            new RaptorRequestBuilder<>();
    private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
            RaptorConfig.defaultConfigForTest()
    );

    @BeforeEach
    public void setup() {
        TestTripPattern pattern = pattern("R1", STOP_B, STOP_C, STOP_D);

        pattern.setNoWheelchairAccess(0);
        transitData.withRoute(
                route(
                        pattern
                )
                        .withTimetable(
                                schedule("00:01, 00:03, 00:05")
                        )
        );

        requestBuilder.searchParams()
                .addAccessPaths(walk(STOP_B, D30s))
                .addEgressPaths(walk(STOP_D, D20s))
                .earliestDepartureTime(T00_00)
                .latestArrivalTime(T00_10)
                .timetableEnabled(true)
                .wheelchairAccess(true);

        ModuleTestDebugLogging.setupDebugLogging(transitData, requestBuilder);

    }

    @Test
    public void boardWithWheelchair() {
        var request = requestBuilder.profile(RaptorProfile.STANDARD).build();
        var response = raptorService.route(request, transitData);

        assertTrue(response.paths().isEmpty(), "Paths should be empty");
    }

    @Test
    public void boardWithoutWheelchair() {

        var request = requestBuilder.profile(RaptorProfile.STANDARD)
                .searchParams().wheelchairAccess(false)
                .build();
        var response = raptorService.route(request, transitData);

        assertTrue(response.paths().size() > 0, "Paths should not be empty");
    }

}
