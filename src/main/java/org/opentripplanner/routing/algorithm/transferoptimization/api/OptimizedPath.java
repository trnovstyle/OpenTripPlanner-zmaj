package org.opentripplanner.routing.algorithm.transferoptimization.api;

import java.util.Map;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * The optimized path decorate the path returned from Raptor with a transfer-priority-cost and
 * a wait-time-optimized-cost.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizedPath<T extends RaptorTripSchedule> extends Path<T>
        implements TransferOptimized
{
    private final Path<T> originalPath;
    private final Map<PathLeg<T>, Transfer> transfersTo;
    private final int transferPriorityCost;
    private final int waitTimeOptimizedCost;


    public OptimizedPath(Path<T> originalPath) {
        this(
                originalPath,
                originalPath,
                Map.of(),
                Transfer.NEUTRAL_PRIORITY_COST,
                originalPath.generalizedCost()
        );
    }

    public OptimizedPath(
            Path<T> originalPath,
            Path<T> path,
            Map<PathLeg<T>, Transfer> transfersTo,
            int transferPriorityCost,
            int waitTimeOptimizedCost
    ) {
        super(path);
        this.originalPath = originalPath;
        this.transfersTo = transfersTo;
        this.transferPriorityCost = transferPriorityCost;
        this.waitTimeOptimizedCost = waitTimeOptimizedCost;
    }

    public int getTransferPriorityCost() {
        return transferPriorityCost;
    }

    public int getWaitTimeOptimizedCost() {
        return waitTimeOptimizedCost;
    }

    public Transfer getTransferTo(PathLeg<?> leg) {
        return transfersTo.get(leg);
    }

    public boolean isSameAsOriginal() {
        PathLeg<T> originalLeg = originalPath.accessLeg();
        PathLeg<T> newLeg = accessLeg();

        while (!originalLeg.isEgressLeg() && !newLeg.isEgressLeg()) {
            if(originalLeg.toStop() != newLeg.toStop()) {
                return false;
            }
            originalLeg = originalLeg.nextLeg();
            newLeg = newLeg.nextLeg();
        }
        return true;
    }
}
