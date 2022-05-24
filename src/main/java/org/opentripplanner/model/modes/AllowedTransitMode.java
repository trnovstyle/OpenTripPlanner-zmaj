package org.opentripplanner.model.modes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TransitSubMode;

/**
 * Used to filter out modes for routing requests. If both mainMode and subMode are specified, they
 * must match exactly. If subMode is set to null, that means that all possible subModes are accepted.
 * This class is separated from the TransitMode class because the meanings of the fields are slightly
 * different.
 */
public class AllowedTransitMode {

  private final TransitMode mainMode;

  private final TransitSubMode subMode;

  public static AllowedTransitMode fromMainModeEnum(TransitMode mainMode) {
    return new AllowedTransitMode(mainMode, null);
  }

  public AllowedTransitMode(TransitMode mainMode, TransitSubMode subMode) {
    this.mainMode = mainMode;
    this.subMode = subMode;
  }

  /**
   * Check if this filter allows the provided TransitMode
   */
  public boolean allows(TransitMode transitMode, TransitSubMode subMode) {
    return mainMode == transitMode && (
            this.subMode == null || this.subMode.equals(subMode)
    );
  }

  public TransitMode getMainMode() {
    return mainMode;
  }

  /**
   * Is the sub-mode set for this main mode
   */
  public boolean hasSubMode() {
    return subMode != null;
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes.
   */
  public static Set<AllowedTransitMode> getAllTransitModes() {
    return Arrays
        .stream(TransitMode.values())
        .map(m -> new AllowedTransitMode(m, null))
        .collect(Collectors.toSet());
  }

  /**
   * Returns a set of AllowedModes that will cover all available TransitModes except airplane.
   */
  public static Set<AllowedTransitMode> getAllTransitModesExceptAirplane() {
    return TransitMode.transitModesExceptAirplane().stream()
        .map(m -> new AllowedTransitMode(m, null))
        .collect(Collectors.toSet());
  }
}
