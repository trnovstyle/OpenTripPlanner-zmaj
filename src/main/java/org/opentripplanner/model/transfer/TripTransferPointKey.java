package org.opentripplanner.model.transfer;

import java.util.Objects;
import org.opentripplanner.model.Trip;

public class TripTransferPointKey {

  private final Trip trip;
  private final int stopPosition;

  public TripTransferPointKey(Trip trip, int stopPosition) {
    this.trip = trip;
    this.stopPosition = stopPosition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TripTransferPointKey that = (TripTransferPointKey) o;
    return stopPosition == that.stopPosition && trip.getId().equals(that.trip.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(trip.getId(), stopPosition);
  }
}
