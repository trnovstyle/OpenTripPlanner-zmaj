package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.routing.algorithm.raptor.path.PathDiff;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for logging diffs before and after path filtering.
 */
public class TransferDiffDebug {
  private static final String DIFF_PERMUTATION = "Permutation";
  private static final String DIFF_ORIGINAL_PATH = "Original path";
  private static final String DIFF_TRANSFER_PRIORITY = "TransferPriority";
  private static final String DIFF_UNCHANGED = "Unchanged";
  private static final String DIFF_WAIT_TIME_OPTIMIZED = "WaitTimeOptimized";

  private static final Logger LOG = LoggerFactory.getLogger(TransferDiffDebug.class);

  /** Utility private constructor prevent instantiation */
  private TransferDiffDebug() {}

  public static <T extends RaptorTripSchedule> void debugDiffOriginalVsPermutations(
      Path<T> original, Collection<Path<T>> permutations
  ) {
    logPathDiff(DIFF_ORIGINAL_PATH, List.of(original), DIFF_PERMUTATION, permutations);
  }

  public static <T extends RaptorTripSchedule> void debugDiffAfterPriorityFilter(
      Collection<Path<T>> permutations, Collection<Path<T>> priority
  ) {
    logPathDiff(DIFF_PERMUTATION, permutations, DIFF_TRANSFER_PRIORITY, priority);
  }

  public static <T extends RaptorTripSchedule>  void debugDiffAfterWaitTimeFilter(
      Collection<Path<T>> priority, Collection<Path<T>> waitTime
  ) {
    logPathDiff(DIFF_TRANSFER_PRIORITY, priority, DIFF_WAIT_TIME_OPTIMIZED, waitTime);
  }

  @SuppressWarnings("Convert2MethodRef")
  public static <T extends RaptorTripSchedule>  void logPathDiff(
      String originalName,
      Collection<Path<T>> originalPaths,
      String optimizedName,
      Collection<Path<T>> optimizedPaths
  ) {
    if(LOG.isDebugEnabled()) {
      PathDiff.logDiff(
          originalName, originalPaths,
          optimizedName, optimizedPaths,
          DIFF_UNCHANGED,
          false, false,
          // Keep lambda to get correct class/line number in log
          m -> LOG.debug(m)
      );
    }
  }

}
