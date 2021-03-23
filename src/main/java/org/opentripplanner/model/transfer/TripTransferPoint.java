package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.model.Trip;

public class TripTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Trip trip;
  private final int stopPosition;


  public TripTransferPoint(Trip trip, int stopPosition) {
    this.trip = trip;
    this.stopPosition = stopPosition;
  }

  @Override
  public final Trip getTrip() {
    return trip;
  }

  @Override
  public final int getStopPosition() {
    return stopPosition;
  }

  @Override
  public int getSpecificityRanking() { return 2; }

  @Override
  public String toString() {
    return trip + "@" + stopPosition;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) { return true; }
    if (!(o instanceof TripTransferPoint)) { return false; }

    TripTransferPoint that = (TripTransferPoint) o;
    return stopPosition == that.stopPosition && trip.getId().equals(that.trip.getId());
  }

  @Override
  public final int hashCode() {
    return Objects.hash(trip.getId(), stopPosition);
  }
}
