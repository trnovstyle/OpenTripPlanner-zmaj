package org.opentripplanner.transit.model.network;

import java.io.Serializable;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

/**
 * The split between TripPattern and RoutingTripPattern is done for the following technical reasons:
 *  - The RTP is accessed frequently during the Raptor search, and we want it to be as small as
 *    possible to load/access it in the cache and CPU for performance reasons.
 *  - Also, we deduplicate these so a RTP can be reused by more than one TP.
 *  - This also provide explicit documentation on witch fields are used during a search and witch
 *    are not.
 */
public class RoutingTripPattern implements RaptorTripPattern, Serializable {

  private static final AtomicInteger INDEX_COUNTER = new AtomicInteger(0);
  private final int index;
  private final TripPattern pattern;
  private final int[] stopIndexes;
  private final BitSet boardingPossible;
  private final BitSet alightingPossible;
  private final BitSet wheelchairAccessible;
  private final int slackIndex;
  private final String debugInfo;

  RoutingTripPattern(TripPattern pattern, TripPatternBuilder builder) {
    this.pattern = pattern;
    this.stopIndexes = pattern.getStops().stream().mapToInt(StopLocation::getIndex).toArray();
    this.index = INDEX_COUNTER.getAndIncrement();

    final int nStops = stopIndexes.length;
    boardingPossible = new BitSet(nStops);
    alightingPossible = new BitSet(nStops);
    wheelchairAccessible = new BitSet(nStops);

    for (int s = 0; s < nStops; s++) {
      boardingPossible.set(s, pattern.canBoard(s));
      alightingPossible.set(s, pattern.canAlight(s));
      wheelchairAccessible.set(s, pattern.wheelchairAccessible(s));
    }

    this.slackIndex = builder.slackIndex();
    this.debugInfo = pattern.getRoute().getMode().name() + " " + pattern.getRoute().getName();
  }

  /**
   * This is the OTP internal <em>synthetic key</em>, used to reference a StopLocation inside OTP.  This is used
   * to optimize routing, we do not access the stop instance only keep the {code index}. The index will not change.
   * <p>
   * Do NOT expose this index in the APIs, it is not guaranteed to be the same across different OTP instances,
   * use the {code id} for external references.
   */
  public int getIndex() {
    return index;
  }

  public final TripPattern getPattern() {
    return this.pattern;
  }

  public BitSet getBoardingPossible() {
    return boardingPossible;
  }

  public BitSet getAlightingPossible() {
    return alightingPossible;
  }

  public BitSet getWheelchairAccessible() {
    return wheelchairAccessible;
  }

  @Override
  public int numberOfStopsInPattern() {
    return stopIndexes.length;
  }

  /**
   * See {@link org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern#stopIndex(int)}
   */
  public int stopIndex(int stopPositionInPattern) {
    return stopIndexes[stopPositionInPattern];
  }

  public boolean boardingPossibleAt(int stopPositionInPattern) {
    return boardingPossible.get(stopPositionInPattern);
  }

  public boolean alightingPossibleAt(int stopPositionInPattern) {
    return alightingPossible.get(stopPositionInPattern);
  }

  @Override
  public int slackIndex() {
    return slackIndex;
  }

  @Override
  public String debugInfo() {
    return debugInfo;
  }

  @Override
  public int hashCode() {
    return index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RoutingTripPattern that)) {
      return false;
    }
    return index == that.getIndex();
  }

  @Override
  public String toString() {
    return "TripPattern{" + "index=" + index + ", debugInfo=" + debugInfo + '}';
  }

  public static int indexCounter() {
    return INDEX_COUNTER.get();
  }

  /**
   * Use this ONLY when deserializing the graph. Sets the counter value to the highest recorded value
   */
  public static void initIndexCounter(int indexCounter) {
    INDEX_COUNTER.set(indexCounter);
  }
}
