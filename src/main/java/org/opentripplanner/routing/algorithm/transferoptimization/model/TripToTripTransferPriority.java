package org.opentripplanner.routing.algorithm.transferoptimization.model;

public enum TripToTripTransferPriority {
  No(3),
  ALLOWED(-1),
  RECOMMENDED(-1),
  PREFERRED(0);

  private final int gtfsCode;

  TripToTripTransferPriority(int gtfsCode) {
    this.gtfsCode = gtfsCode;
  }
}
