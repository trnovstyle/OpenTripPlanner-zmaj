package org.opentripplanner.routing.algorithm.transferoptimization.api;

import org.opentripplanner.model.transfer.Transfer;

/**
 * The transfer optimization is performed by calculating two "cost" values:
 * <ol>
 *     <li>transfer-priority-cost</li>
 *     <li>wait-time-optimized-cost</li>
 * </ol>
 *
 * If enabled both of these cost is used to find the optimal transfer-points for a given set of
 * transit legs. The transfer-priority take precedence over the wait-time-optimized-cost. The
 * wait-time-optimized-cost is used to break any ties if there are multiple paths with the same
 * transfer-priority-cost. For example for a given path the normal case is that the the
 * transfer-priority-cost is {@link Transfer#NEUTRAL_PRIORITY_COST}. Then we look at the
 * wait-time-optimized-cost.
 *
 * The wait-time-optimized-cost uses the generalized-cost as a baseline and adjust it to
 * optimize the wait-time. The cost of waiting is changed while the rest of the cost parts are
 * kept as is. If the wait-time-optimized-cost is not enabled, the generalized-cost from Raptor is
 * used instead.
 */
public interface TransferOptimized {
    /**
     * The generalized-cost adjusted with a better wait-time calculation.
     */
    int getWaitTimeOptimizedCost();

    /**
     * Return the total transfer priority cost. This have nothing to do with the
     * generalized-cost. Return {@code 0}(zero) if cost is neutral/no "special"-transfers exist.
     *
     * @see Transfer#priorityCost(Transfer)
     */
    int getTransferPriorityCost();

}
