package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripScheduleWithOffset;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.HashSet;
import java.util.Set;
import java.util.function.DoubleFunction;

public class UnpreferredModeCostCalculator implements CostCalculator {

    private final CostCalculator deferred;
    private final DoubleFunction<Double> unpreferredModeCostFunction;
    private final Set<TransitMode> unpreferredModes;

    public UnpreferredModeCostCalculator(CostCalculator deferred,
                                         DoubleFunction<Double> unpreferredModeCostFunction,
                                         Set<TransitMode> unpreferredModes
                                         ) {
        this.deferred = deferred;
        this.unpreferredModeCostFunction = unpreferredModeCostFunction;
        this.unpreferredModes = unpreferredModes;

    }
    public UnpreferredModeCostCalculator(CostCalculator deferred, McCostParams mcCostParams) {
        this(deferred, mcCostParams.unpreferredModeCost(), mcCostParams.unpreferredModes());
    }
    @Override
    public int boardingCost(boolean firstBoarding, int prevArrivalTime, int boardStop, int boardTime, RaptorTripSchedule trip, RaptorTransferConstraint transferConstraints) {
        return deferred.boardingCost(firstBoarding,
                prevArrivalTime,
                boardStop,
                boardTime,
                trip,
                transferConstraints);
    }

    @Override
    public int onTripRelativeRidingCost(int boardTime, int transitFactorIndex) {
        return deferred.onTripRelativeRidingCost(boardTime, transitFactorIndex);
    }

    @Override
    public int transitArrivalCost(int boardCost, int alightSlack, int transitTime, int transitFactorIndex, int toStop, RaptorTripSchedule trip) {
        var defaultCost = deferred.transitArrivalCost(boardCost, alightSlack, transitTime, transitFactorIndex, toStop, trip);
        var mode = trip.pattern().getTransitMode();
        if (unpreferredModes.contains(mode)) {
            return defaultCost + RaptorCostConverter.toRaptorCost(unpreferredModeCostFunction.apply(transitTime));
        }
        return defaultCost;
    }

    @Override
    public int waitCost(int waitTimeInSeconds) {
        return deferred.waitCost(waitTimeInSeconds);
    }

    @Override
    public int calculateMinCost(int minTravelTime, int minNumTransfers) {

        return deferred.calculateMinCost(minTravelTime, minNumTransfers);
    }

    @Override
    public int costEgress(RaptorTransfer egress) {
        return deferred.costEgress(egress);
    }
}
