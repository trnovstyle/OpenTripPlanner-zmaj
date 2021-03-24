package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.model.Stop;

public class StopTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Stop stop;

  public StopTransferPoint(Stop stop) {
    this.stop = stop;
  }

  @Override
  public Stop getStop() {
    return stop;
  }

  @Override
  public int getSpecificityRanking() {
    return 0;
  }

  @Override
  public String toString() {
    return "(stop: " + stop.getId() + ")";
  }
}
