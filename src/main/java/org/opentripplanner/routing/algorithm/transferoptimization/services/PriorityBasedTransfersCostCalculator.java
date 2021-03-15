package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfers.Transfer;
import org.opentripplanner.model.transfers.TransferPriority;
import org.opentripplanner.model.transfers.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


public class PriorityBasedTransfersCostCalculator<T extends RaptorTripSchedule> {
  private final IntFunction<Stop> stopLookup;
  private final TransferService transferService;

  public PriorityBasedTransfersCostCalculator(
      IntFunction<Stop> stopLookup,
      TransferService transferService
  ) {
    this.stopLookup = stopLookup;
    this.transferService = transferService;
  }


  public int cost(Path<T> path) {
    var legs = path
        .legStream()
        .filter(PathLeg::isTransitLeg)
        .map(PathLeg::asTransitLeg)
        .collect(Collectors.toList());

    int cost = 0;
    for (int i=1; i< legs.size(); ++i) {
      cost += cost(legs.get(i-1), legs.get(i));
    }
    return cost;
  }

  private int cost(TransitPathLeg<T> from, TransitPathLeg<T> to) {
    var t = transferService.findTransfer(
        stop(from.toStop()),
        stop(to.fromStop()),
        trip(from.trip()),
        trip(to.trip())
    );
    return costTransfer(t);
  }

  private Stop stop(int index) {
    return stopLookup.apply(index);
  }

  private Trip trip(T raptorTripSchedule) {
    return ((TripSchedule)raptorTripSchedule).getOriginalTripTimes().trip;
  }

  private static int costTransfer(Transfer t) {
    if(t == null) { return 0; }
    int cost = 0;
    if(t.isStaySeated()) { cost += 100; }
    if(t.isGuaranteed()) { cost += 10; }
    return cost + costPriority(t.getPriority());
  }

  private static int costPriority(TransferPriority priority) {
    switch (priority) {
      case RECOMMENDED: return 2;
      case PREFERRED: return 1;
      case ALLOWED: return 0;
      case NOT_ALLOWED: return -1_000;
    }
    throw new IllegalArgumentException("Priority not supported: " + priority);
  }
}
