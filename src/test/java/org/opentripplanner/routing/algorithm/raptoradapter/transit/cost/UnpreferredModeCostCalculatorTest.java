package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;

import java.util.Set;
import java.util.function.DoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnpreferredModeCostCalculatorTest {

    private static final int BOARD_COST_SEC = 5;
    private static final int TRANSFER_COST_SEC = 2;
    private static final double WAIT_RELUCTANCE_FACTOR = 0.5;
    private static final double TRANSIT_RELUCTANCE_FACTOR_1 = 1.0;
    private static final double TRANSIT_RELUCTANCE_FACTOR_2 = 0.8;
    private static final int TRANSIT_RELUCTANCE_1 = 0;
    private static final int TRANSIT_RELUCTANCE_2 = 1;
    private static final int STOP_A = 0;
    private static final int STOP_B = 1;
    private static final int UNPREFERRED_MODE_CONSTANT = 5;
    private static final double UNPREFERRED_MODE_COEFFICIENT = 0.3;

    private static final DoubleFunction<Double> unprefModeCost = RequestFunctions.createLinearFunction(
            UNPREFERRED_MODE_CONSTANT, UNPREFERRED_MODE_COEFFICIENT);


    private static final Set<TransitMode> unpreferredModes = Set.of(TransitMode.BUS);
    private final DefaultCostCalculator defaultCostCalculator = new DefaultCostCalculator(
            BOARD_COST_SEC,
            TRANSFER_COST_SEC,
            WAIT_RELUCTANCE_FACTOR,
            new double[] { TRANSIT_RELUCTANCE_FACTOR_1, TRANSIT_RELUCTANCE_FACTOR_2 },
            new int[] { 0, 25 }
    );

    private final UnpreferredModeCostCalculator subject = new UnpreferredModeCostCalculator(defaultCostCalculator, unprefModeCost, unpreferredModes);


    @Test
    public void transitArrivalCost() {
        TestTripSchedule trip = TestTripSchedule
                .schedule(TestTripPattern.pattern("L31", STOP_A, STOP_B))
                .arrivals("10:00 10:05")
                .departures("10:01 10:06")
                .build();

        assertEquals(RaptorCostConverter.toRaptorCost(UNPREFERRED_MODE_CONSTANT), subject.transitArrivalCost(0, 0, 0, TRANSIT_RELUCTANCE_1, STOP_A, trip), "Unpreferred constant penalty");
        assertEquals(1800, subject.transitArrivalCost(0, 0, 10, TRANSIT_RELUCTANCE_1, STOP_A, trip), "Unpreferred mode cost");

    }

    @Test
    public void transitArrivalCost_notUnpreferred() {
        TestTripSchedule trip = TestTripSchedule
                .schedule(TestTripPattern.pattern("L31", STOP_A, STOP_B))
                .arrivals("10:00 10:05")
                .departures("10:01 10:06")
                .build();

        UnpreferredModeCostCalculator subject = new UnpreferredModeCostCalculator(defaultCostCalculator, unprefModeCost, Set.of(TransitMode.RAIL));

        assertEquals(0, subject.transitArrivalCost(0, 0, 0, TRANSIT_RELUCTANCE_1, STOP_A, trip), "Zero cost");
        assertEquals(1000, subject.transitArrivalCost(1000, 0, 0, TRANSIT_RELUCTANCE_1, STOP_A, trip), "Board cost");
        assertEquals(50, subject.transitArrivalCost(0, 1, 0, TRANSIT_RELUCTANCE_1, STOP_A, trip), "Alight wait time cost");
        assertEquals(100, subject.transitArrivalCost(0, 0, 1, TRANSIT_RELUCTANCE_1, STOP_A, trip), "Transit time cost");
        assertEquals(25, subject.transitArrivalCost(0, 0, 0, TRANSIT_RELUCTANCE_1, STOP_B, trip), "Alight stop cost");
        assertEquals(1175, subject.transitArrivalCost(1000, 1, 1, TRANSIT_RELUCTANCE_1, STOP_B, trip), "Total cost");
        assertEquals(50,
                subject.transitArrivalCost(0, 1, 0, TRANSIT_RELUCTANCE_1, STOP_A, trip),
                "Alight wait time cost"
        );
        assertEquals(
                100,
                subject.transitArrivalCost(0, 0, 1, TRANSIT_RELUCTANCE_1, STOP_A, trip),
                "Transit time cost"
        );
        assertEquals(
                25,
                subject.transitArrivalCost(0, 0, 0, TRANSIT_RELUCTANCE_1, STOP_B, trip),
                "Alight stop cost"
        );
        assertEquals(
                1175,
                subject.transitArrivalCost(1000, 1, 1, TRANSIT_RELUCTANCE_1, STOP_B, trip),
                "Total cost"
        );
    }

}
