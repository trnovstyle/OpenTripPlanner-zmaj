package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class TransferOptimizedFilterFactory<T extends RaptorTripSchedule> {

    public static <T extends RaptorTripSchedule> MinCostFilterChain<OptimizedPathTail<T>> filter(
            boolean transferPriority,
            boolean optimizeWaitTime
    ) {
        return new TransferOptimizedFilterFactory<T>().create(transferPriority, optimizeWaitTime);
    }

    private MinCostFilterChain<OptimizedPathTail<T>> create(
            boolean transferPriority, boolean optimizeWaitTime
    ) {
        List<ToIntFunction<OptimizedPathTail<T>>> filters = new ArrayList<>(3);

        if(transferPriority) {
            filters.add(OptimizedPathTail::getTransferPriorityCost);
        }

        if(optimizeWaitTime) {
            filters.add(OptimizedPathTail::getWaitTimeOptimizedCost);
        }
        else {
            filters.add(it -> it.getLeg().tailGeneralizedCost());
        }

        filters.add(it -> tie(it.getLeg().stream()));

        return new MinCostFilterChain<>(List.copyOf(filters));
    }

    private int tie(Stream<PathLeg<T>> tail) {
        return tail.filter(PathLeg::isTransitLeg).mapToInt(PathLeg::fromTime).sum();
    }
}
