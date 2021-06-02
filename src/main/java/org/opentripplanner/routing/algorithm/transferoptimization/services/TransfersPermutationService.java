package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.StopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * This class is responsible for generating all possible permutations of places to transfers for a
 * given path. For example, if a path ride 3 busses (Trip 1, 2, and 3) and there are 2 possible
 * places to transfer for each transfer. The transfer between trip 1 and 2 may take palce at stop A
 * or B, and the transfer between trip 2 and 3 may take palce at stop C or D. Then the following
 * paths are generated:
 * <pre>
 * Origin ~ Trip 1 ~ A ~ Trip 2 ~ C ~ Trip 3 ~ Destination
 * Origin ~ Trip 1 ~ B ~ Trip 2 ~ C ~ Trip 3 ~ Destination
 * Origin ~ Trip 1 ~ A ~ Trip 2 ~ D ~ Trip 3 ~ Destination
 * Origin ~ Trip 1 ~ B ~ Trip 2 ~ D ~ Trip 3 ~ Destination
 * </pre>
 */
public class TransfersPermutationService<T extends RaptorTripSchedule> {

  private final StandardTransferGenerator<T> t2tService;
  private final CostCalculator<T> costCalculator;
  private final RaptorSlackProvider slackProvider;

  public TransfersPermutationService(
      StandardTransferGenerator<T> t2tService,
      CostCalculator<T> costCalculator,
      RaptorSlackProvider slackProvider
  ) {
    this.t2tService = t2tService;
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
  }

  public List<OptimizedPath<T>> findAllTransitPathPermutations(Path<T> path) {
    List<TransitPathLeg<T>> transitLegs = listOfTransitLegs(path);

    // Find all possible transfer between each pair of transit legs
    var possibleTransfers = listOfPossibleTransfers(transitLegs);

    // Combine transit legs and transfers
    List<TransitPathLeg<T>> result = findAllTransferCombinations(
            path, transitLegs, possibleTransfers
    );

    // Create result; Add back the the access-leg to all paths
    return result.stream()
            .map(l -> newPath(path, l))
            .map(p -> new OptimizedPath<>(path, p))
            .collect(Collectors.toList());
  }

  private List<TransitPathLeg<T>> listOfTransitLegs(Path<T> path) {
    return path.legStream()
            .filter(PathLeg::isTransitLeg)
            .map(PathLeg::asTransitLeg)
            .collect(Collectors.toList());
  }

  private List<List<TripToTripTransfer<T>>> listOfPossibleTransfers(List<TransitPathLeg<T>> originalTransitLegs) {
    List<List<TripToTripTransfer<T>>> result = new ArrayList<>();
    var fromLeg = originalTransitLegs.get(0);
    TransitPathLeg<T> toLeg;
    var departure = fromStopTime(fromLeg);

    for (int i = 1; i < originalTransitLegs.size(); i++) {
      toLeg = originalTransitLegs.get(i);
      var transfers = t2tService.findTransfers(fromLeg.trip(), departure, toLeg.trip());
      result.add(transfers);

      // Setup next
      departure = transfers.stream()
              .map(TripToTripTransfer::to)
              .min(Comparator.comparingInt(TripStopTime::time))
              .orElseThrow();
      fromLeg = toLeg;
    }
    return result;
  }

  private List<TransitPathLeg<T>> findAllTransferCombinations(
          Path<T> path,
          List<TransitPathLeg<T>> originalTransitLegs,
          List<List<TripToTripTransfer<T>>> possibleTransfers
  ) {
    List<TransitPathLeg<T>> result = List.of(last(originalTransitLegs));
    List<TransitPathLeg<T>> tails;
    int accessArrivalTime = path.accessLeg().toTime();

    // Make sure we add the proper cost/slack for FLEX access
    boolean accessWithoutRides = !path.accessLeg().access().hasRides();

    for (int i = possibleTransfers.size()-1; i >=0; --i) {
      tails = result;
      result = new ArrayList<>();

      for (TripToTripTransfer<T> tx : possibleTransfers.get(i)) {
        for (TransitPathLeg<T> tail : tails) {
          // Make sure board time is before alight time
          if (tx.to().time() < tail.toTime()) {
            int txDepartureTime = arrivalTime(tx.from());
            int txArrivalTime = txDepartureTime + tx.transferDuration();

            TransitPathLeg<T> newTransitLeg = tail.mutate()
                    .boardStop(tx.to().stop(), tx.to().time())
                    .build(costCalculator, slackProvider, false, txArrivalTime);
            PathLeg<T> newTail = tx.sameStop()
                    ? newTransitLeg
                    : createTransfer(tx, txDepartureTime, txArrivalTime, newTransitLeg);

            boolean firstRide = i==0 && accessWithoutRides;
            result.add(
                  // Using the access-arrival-time as input here is only correct for the first
                  // transit leg. But the leg created here is temporary and will be mutated in
                  // the next iteration(except for the first transit leg)
                  originalTransitLegs.get(i).mutate()
                          .newTail(tx.from().time(), newTail)
                          .build(costCalculator, slackProvider, firstRide, accessArrivalTime)
            );
          }
        }
      }
    }
    return result;
  }

  private static <T extends RaptorTripSchedule> Path<T> newPath(
          Path<T> path,
          TransitPathLeg<T> tail
  ) {
    AccessPathLeg<T> accessPathLeg = new AccessPathLeg<>(path.accessLeg(), tail);
    return new Path<>(path.rangeRaptorIterationDepartureTime(), accessPathLeg);
  }

  private TransferPathLeg<T> createTransfer(
          TripToTripTransfer<T> tx,
          int departureTime,
          int arrivalTime,
          TransitPathLeg<T> tail
  ) {
    int cost = RaptorCostConverter.toOtpDomainCost(costCalculator.walkCost(tx.transferDuration()));
    return new TransferPathLeg<>(
            tx.from().stop(),
            departureTime,
            tx.to().stop(),
            arrivalTime,
            cost,
            tx.getTransfer(),
            tail
    );
  }

  /** Trip alight time + alight slack */
  private int arrivalTime(TripStopTime<T> arrival) {
    return arrival.time() + slackProvider.alightSlack(arrival.trip().pattern());
  }

  @Nonnull
  private StopTime fromStopTime(final TransitPathLeg<T> leg) {
    return StopTime.stopTime(leg.fromStop(), leg.fromTime());
  }

  private static <T> T last(List<T> list) { return list.get(list.size()-1);}
}
