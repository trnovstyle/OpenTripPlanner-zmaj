package org.opentripplanner.routing.algorithm.prioritizedtransfers.model;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import javax.annotation.Nullable;

public class TripToTripTransfer<T extends RaptorTripSchedule> {
  private final TripStopTime<T> from;
  private final TripStopTime<T> to;
  private final RaptorTransfer transfer;

  public TripToTripTransfer(
      TripStopTime<T> from,
      TripStopTime<T> to,
      RaptorTransfer transfer
  ) {
    this.from = from;
    this.to = to;
    this.transfer = transfer;
  }

  public TripStopTime<T> from() {
    return from;
  }

  public TripStopTime<T> to() {
    return to;
  }

  public int transferDuration() {
    return sameStop() ? 0 : transfer.durationInSeconds();
  }

  @Nullable
  public RaptorTransfer getTransfer() {
    return transfer;
  }

  public boolean sameStop() {
    return from.stop() == to.stop();
  }

}
