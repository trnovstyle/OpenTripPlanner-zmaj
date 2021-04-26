package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Default alteration for a trip, if no DSJ exist then this is the ServiceJourney
 * alteration. If DSJs exist, then this is null.
 *
 * This is planned, by default (e.g. GTFS and if not set explicit).
 */
class TripAlterationSchedule implements Serializable {
  private final TripAlteration alteration;
  private final Map<ServiceDate, TripAlterationOnDate> alterations;

  private static final long serialVersionUID = 1L;

  private static final Map<TripAlteration, TripAlterationSchedule> PLAIN_ALTERATIONS
      = Arrays.stream(TripAlteration.values())
          .map(TripAlterationSchedule::new)
          .collect(toUnmodifiableMap(it -> it.alteration, identity()));

  private TripAlterationSchedule(TripAlteration alteration) {
    this.alteration = alteration;
    this.alterations = null;
  }

  private TripAlterationSchedule(Map<ServiceDate, TripAlterationOnDate> alterations) {
    this.alteration = null;
    this.alterations = alterations;
  }

  static TripAlterationSchedule defaultAlteration() {
    return PLAIN_ALTERATIONS.get(TripAlteration.planned);
  }
  /**
   * Create a alteration schedule for a NONE DSJ ServiceJourney. This is unmodifiable and
   * guarantees that the same same object is returned every time for the same given input.
   */
  static TripAlterationSchedule createAlterationSchedule(TripAlteration alteration) {
    return alteration == null ? defaultAlteration() : PLAIN_ALTERATIONS.get(alteration);
  }

  static TripAlterationSchedule createAlterationSchedule(
      Map<ServiceDate, TripAlterationOnDate> alterations
  ) {
    return new TripAlterationSchedule(alterations);
  }


  /**
   * Return true if the source is a NOT DatedServiceJourney object. Return {@code true} if the
   * source is a regular ServiceJourney or GTFS Trip.
   */
  boolean isRegular() { return alteration != null; }

  /**
   * Return true if the source is a DatedServiceJourney object. Return {@code false} if the
   * source is a regular ServiceJourney or GTFS Trip.
   */
  boolean isMappedToDate() { return !isRegular(); }

  boolean isCanceledOrReplaced() {
    //noinspection ConstantConditions
    return isRegular() && alteration.isCanceledOrReplaced();
  }

  @Nullable
  TripAlteration fixedAlteration() { return alteration; }

  @Nullable
  TripAlterationOnDate alterationForDate(ServiceDate date) {
    //noinspection ConstantConditions
    return isRegular() ? null : alterations.get(date);
  }

  @NotNull
  TripAlteration alteration(ServiceDate date) {
    //noinspection ConstantConditions
    return isRegular() ? alteration : alterations.get(date).getAlteration();
  }

  Iterable<TripAlterationOnDate> listAll() {
    //noinspection ConstantConditions
    return isRegular() ? List.of() : alterations.values();
  }
}
