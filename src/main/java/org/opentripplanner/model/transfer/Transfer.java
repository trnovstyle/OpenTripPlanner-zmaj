/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.transfer;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;

public final class Transfer implements Serializable {

  /**
   * Regular street transfers should be given this cost.
   */
  public static final int NEUTRAL_TRANSFER_COST = 0;

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
        .addEnum("priority", priority, TransferPriority.ALLOWED)
        .addBoolIfTrue("staySeated", staySeated)
        .addBoolIfTrue("guaranteed", guaranteed)
        .toString();
  }

  /**
   * Calculate a cost for prioritizing transfers in a path to select the best path with respect to
   * transfers. This cost should not be mixed with the path generalized-cost.
   *
   * @see TransferPriority#cost(boolean, boolean)
   * @param t The transfer to return a cost for, or {@code null} if the transfer is a regular OSM
   *          street generated transfer.
   */
  public static int priorityCost(@Nullable Transfer t) {
    return t == null ? NEUTRAL_TRANSFER_COST : t.priority.cost(t.staySeated, t.guaranteed);
  }
}
