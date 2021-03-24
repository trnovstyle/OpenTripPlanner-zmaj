package org.opentripplanner.routing.algorithm.transferoptimization;

import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferDiffDebug.debugDiffAfterPriorityFilter;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferDiffDebug.debugDiffAfterWaitTimeFilter;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferDiffDebug.debugDiffOriginalVsPermutations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.ToIntFunction;
import org.opentripplanner.routing.algorithm.transferoptimization.model.FilterPathsUtil;
import org.opentripplanner.routing.algorithm.transferoptimization.services.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizeTransferCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.PriorityBasedTransfersCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransfersPermutationService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizeTransferService<T extends RaptorTripSchedule> {

  private static final Logger LOG = LoggerFactory.getLogger(OptimizeTransferService.class);

  private final TransfersPermutationService<T> transfersPermutationService;
  private final PriorityBasedTransfersCostCalculator<T> priorityBasedTransfersCostCalculator;
  private final MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator;
  private final OptimizeTransferCostCalculator transferCostCalculator;
  private final ToIntFunction<Path<T>> minimizeWaitTimeFunction;

  public OptimizeTransferService(
      TransfersPermutationService<T> transfersPermutationService,
      PriorityBasedTransfersCostCalculator<T> priorityBasedTransfersCostCalculator,
      MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator,
      OptimizeTransferCostCalculator optimizeTransferCostCalculator
  ) {
    this.transfersPermutationService = transfersPermutationService;
    this.priorityBasedTransfersCostCalculator = priorityBasedTransfersCostCalculator;
    this.minSafeTransferTimeCalculator = minSafeTransferTimeCalculator;
    this.transferCostCalculator = optimizeTransferCostCalculator;
    this.minimizeWaitTimeFunction = transferCostCalculator::cost;
  }

  public OptimizeTransferService(
      TransfersPermutationService<T> transfersPermutationService,
      PriorityBasedTransfersCostCalculator<T> priorityBasedTransfersCostCalculator
  ) {
    this.transfersPermutationService = transfersPermutationService;
    this.priorityBasedTransfersCostCalculator = priorityBasedTransfersCostCalculator;
    this.minSafeTransferTimeCalculator = null;
    this.transferCostCalculator = null;
    this.minimizeWaitTimeFunction = Path::generalizedCost;
  }

  public List<Path<T>> optimize(Collection<Path<T>> paths) {
    setup(paths);

    List<Path<T>> results = new ArrayList<>();

    for (Path<T> path : paths) {
      results.addAll(optimize(path));
    }
    return results;
  }

  /**
   * Initiate calculation.
   */
  @SuppressWarnings("ConstantConditions")
  private void setup(Collection<Path<T>> paths) {
    if (transferCostCalculator != null) {
      transferCostCalculator.setMinSafeTransferTime(
          minSafeTransferTimeCalculator.minSafeTransferTime(paths)
      );
    }
  }

  /**
   * Optimize a single transfer, finding all possible permutations of transfers for the path and
   * filtering the list down one path, or a few equally good paths.
   */
  private Collection<Path<T>> optimize(Path<T> path) {
    TransitPathLeg<T> leg0 = path.accessLeg().nextTransitLeg();

    // Path has no transit legs(possible with flex access) or
    // the path have no transfers, then use the path found.
    if (leg0 == null || leg0.nextTransitLeg() == null) {
      return List.of(path);
    }
    LOG.debug("Optimize path: {}", path);

    var allPossibleTransfers = transfersPermutationService.findAllTransitPathPermutations(path);
    debugDiffOriginalVsPermutations(path, allPossibleTransfers);

    var priorityFilteredPaths = FilterPathsUtil.filter(
        allPossibleTransfers,
        priorityBasedTransfersCostCalculator::cost
    );
    debugDiffAfterPriorityFilter(allPossibleTransfers, priorityFilteredPaths);

    // The path passed in is not allowed, and no other path exist
    if (priorityFilteredPaths.isEmpty()) { return List.of(); }

    var waitTimeFilteredPaths = FilterPathsUtil.filter(
        priorityFilteredPaths,
        minimizeWaitTimeFunction
    );
    debugDiffAfterWaitTimeFilter(priorityFilteredPaths, waitTimeFilteredPaths);

    return waitTimeFilteredPaths;
  }
}
