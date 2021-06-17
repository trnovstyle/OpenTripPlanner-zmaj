package org.opentripplanner.routing.algorithm.transferoptimization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizePathService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizeTransferService<T extends RaptorTripSchedule> {

  private static final Logger LOG = LoggerFactory.getLogger(OptimizeTransferService.class);

  private final OptimizePathService<T> optimizePathService;
  private final MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator;
  private final TransferWaitTimeCalculator transferWaitTimeCostCalculator;

  public OptimizeTransferService(
          OptimizePathService<T> optimizePathService,
          MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator,
          TransferWaitTimeCalculator transferWaitTimeCalculator
  ) {
    this.optimizePathService = optimizePathService;
    this.minSafeTransferTimeCalculator = minSafeTransferTimeCalculator;
    this.transferWaitTimeCostCalculator = transferWaitTimeCalculator;
  }

  public OptimizeTransferService(OptimizePathService<T> optimizePathService) {
    this.optimizePathService = optimizePathService;
    this.minSafeTransferTimeCalculator = null;
    this.transferWaitTimeCostCalculator = null;
  }

  public List<Path<T>> optimize(Collection<Path<T>> paths) {
    long start = System.currentTimeMillis();
    setup(paths);

    List<Path<T>> results = new ArrayList<>();

    for (Path<T> path : paths) {
      results.addAll(optimize(path));
    }

    System.err.println("Time: " + (System.currentTimeMillis() - start) + "ms "
            + (results.isEmpty() ? "<empty>" : results.get(0).numberOfTransfers())
    );
    return results;
  }

  /**
   * Initiate calculation.
   */
  @SuppressWarnings("ConstantConditions")
  private void setup(Collection<Path<T>> paths) {
    if (transferWaitTimeCostCalculator != null) {
      transferWaitTimeCostCalculator.setMinSafeTransferTime(
              minSafeTransferTimeCalculator.minSafeTransferTime(paths)
      );
    }
  }

  /**
   * Optimize a single transfer, finding all possible permutations of transfers for the path and
   * filtering the list down one path, or a few equally good paths.
   */
  private Collection<OptimizedPath<T>> optimize(Path<T> path) {
    // Skip transfer optimization if no transfers exist.
    if (path.numberOfTransfersExAccessEgress() == 0) {
      return List.of(new OptimizedPath<>(path));
    }

    long start = System.currentTimeMillis();
    LOG.debug("Optimize path: {}", path);

    var allPossibleTransfers = optimizePathService.findBestTransitPath(path);

    if(LOG.isDebugEnabled()) {
      for (OptimizedPath<T> it : allPossibleTransfers) {
        if(it.isSameAsOriginal()) {
          LOG.debug("Original path kept after optimization.");
        }
        else {
          LOG.debug("Optimize new:  {}", it);
        }
      }
    }
    LOG.debug("Optimized path done in {} ms.", System.currentTimeMillis() -start);
    return allPossibleTransfers;
  }
}