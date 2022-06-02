package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;

/**
 * This filter remove all transit results which have a generalized-cost higher than the max-limit
 * computed by the {@link #costLimitFunction} plus the wait cost given by
 * {@link TransitGeneralizedCostFilter#getWaitTimeCost}.
 * <p>
 *
 * @see org.opentripplanner.routing.api.request.ItineraryFilterParameters#transitGeneralizedCostLimit
 */
public class TransitGeneralizedCostFilter implements ItineraryDeletionFlagger {

  private final DoubleFunction<Double> costLimitFunction;

  private final double waitAtBeginningOrEndCostFactor;

  public TransitGeneralizedCostFilter(
    DoubleFunction<Double> costLimitFunction,
    double waitAtBeginningOrEndCostFactor
  ) {
    this.costLimitFunction = costLimitFunction;
    this.waitAtBeginningOrEndCostFactor = waitAtBeginningOrEndCostFactor;
  }

  @Override
  public String name() {
    return "transit-cost-filter";
  }

  @Override
  public List<Itinerary> getFlaggedItineraries(List<Itinerary> itineraries) {
    List<Itinerary> transitItineraries = itineraries
      .stream()
      .filter(Itinerary::hasTransit)
      .sorted(Comparator.comparingDouble(it -> it.generalizedCost))
      .toList();

    return transitItineraries
      .stream()
      .filter(it ->
        transitItineraries
          .stream()
          .anyMatch(t ->
            it.generalizedCost > costLimitFunction.apply(t.generalizedCost) + getWaitTimeCost(t, it)
          )
      )
      .collect(Collectors.toList());
  }

  private double getWaitTimeCost(Itinerary a, Itinerary b) {
    return (
      waitAtBeginningOrEndCostFactor *
      Math.max(
        Math.abs(ChronoUnit.SECONDS.between(a.startTime(), b.startTime())),
        Math.abs(ChronoUnit.SECONDS.between(a.endTime(), b.endTime()))
      )
    );
  }
}
