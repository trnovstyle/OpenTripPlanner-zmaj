package org.opentripplanner.routing.algorithm.prioritizedtransfers;

import org.opentripplanner.routing.algorithm.prioritizedtransfers.services.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.services.OptimizeTransferCostCalculator;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.services.TransfersPermutationService;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

public class OptimizeTransferService<T extends RaptorTripSchedule> {


  private final OptimizeTransferCostCalculator transferCostCalculator;
  private final ToIntFunction<Path<T>> waitCostFunction;
  private final TransfersPermutationService<T> transfersPermutationService;
  private final MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator;

  public OptimizeTransferService(
      TransitLayer transitLayer,
      TransfersPermutationService<T> transfersPermutationService,
      MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator,
      boolean optimizeTransferCost
  ) {
    this.transfersPermutationService = transfersPermutationService;

    if(optimizeTransferCost) {
      this.minSafeTransferTimeCalculator = minSafeTransferTimeCalculator;
      this.transferCostCalculator = new OptimizeTransferCostCalculator(4.0);
      this.transferCostCalculator.transitLayer = transitLayer;
      this.waitCostFunction = transferCostCalculator::cost;
    }
    else {
      this.minSafeTransferTimeCalculator = null;
      this.transferCostCalculator = null;
      this.waitCostFunction = Path::generalizedCost;
    }
  }

  public List<Path<T>> optimize(Collection<Path<T>> paths) {
    setup(paths);

    List<Path<T>> results = new ArrayList<>();

    for (Path<T> path : paths) {
      Path<T> result = optimize(path);
      if(result != null) {
        results.add(result);
      }
    }
    return results;
  }


  @Nullable
  private Path<T> optimize(Path<T> path) {
    TransitPathLeg<T> leg0 = path.accessLeg().nextTransitLeg();

    // Path has no transit legs(possible with flex access) or
    // the path have no transfers, then use the path found.
    if(leg0 == null || leg0.nextTransitLeg() == null) {
      return path;
    }

    List<Path<T>> optimizedPaths = transfersPermutationService.findAllTransitPathPermutations(path);

    // The path passed in is not allowed, and no other path exist
    if(optimizedPaths.isEmpty()) { return null; }

    return optimizedPaths.stream()
        .min(Comparator.comparingInt(waitCostFunction))
        .orElse(null);
  }

  /**
   * Initiate calculation.
   */
  private void setup(Collection<Path<T>> paths) {
    if(transferCostCalculator != null) {
      transferCostCalculator.setMinSafeTransferTime(
          minSafeTransferTimeCalculator.minSafeTransferTime(paths)
      );
    }
  }
}
