/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Collection;
import org.opentripplanner.model.base.ToStringBuilder;

public final class Transfer implements Serializable {

  private static final long serialVersionUID = 1L;

  private final TransferPoint from;

  private final TransferPoint to;

  private final TransferPriority priority;

  private final boolean staySeated;

  private final boolean guaranteed;

  public Transfer(
      TransferPoint from,
      TransferPoint to,
      TransferPriority priority,
      boolean staySeated,
      boolean guaranteed
  ) {
    this.from = from;
    this.to = to;
    this.priority = priority;
    this.staySeated = staySeated;
    this.guaranteed = guaranteed;
  }

  public TransferPoint getFrom() {
    return from;
  }

  public TransferPoint getTo() {
    return to;
  }

  public TransferPriority getPriority() {
    return priority;
  }

  public boolean isStaySeated() {
    return staySeated;
  }

  public boolean isGuaranteed() {
    return guaranteed;
  }

  /**
   * <a href="https://developers.google.com/transit/gtfs/reference/gtfs-extensions#specificity-of-a-transfer">
   * Specificity of a transfer
   * </a>
   */
  public int getSpecificityRanking() {
    return from.getSpecificityRanking() + to.getSpecificityRanking();
  }

  public String toString() {
    return ToStringBuilder.of(Transfer.class)
        .addObj("from", from)
        .addObj("to", to)
        .addBoolIfTrue("staySeated", staySeated)
        .addBoolIfTrue("guaranteed", guaranteed)
        .toString();
  }

  static Transfer findBestSpecificityRankedTransfer(Collection<Transfer> result) {
    Transfer bestTransfer = null;
    int bestRank = -1;

    for (Transfer it : result) {
      if(it.getSpecificityRanking() > bestRank) {
        bestTransfer = it;
        bestRank = it.getSpecificityRanking();
      }
    }
    return bestTransfer;
  }
}
