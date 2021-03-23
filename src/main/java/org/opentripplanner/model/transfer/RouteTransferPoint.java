package org.opentripplanner.model.transfer;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

public class RouteTransferPoint extends TripTransferPoint {

  private final Route route;

  public RouteTransferPoint(Route route, Trip trip, int stopPosition) {
    super(trip, stopPosition);
    this.route = route;
  }

  @Override
  public int getSpecificityRanking() { return 1; }

  @Override
  public String toString() {
    return route + "-" + super.toString();
  }

}
