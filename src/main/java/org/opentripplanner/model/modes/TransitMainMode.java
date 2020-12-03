package org.opentripplanner.model.modes;

import java.util.EnumSet;

/**
 * Equivalent to GTFS route_type or to NeTEx TransportMode. Used as part of the TransitMode class.
 */
public enum TransitMainMode {
  RAIL,
  COACH,
  SUBWAY,
  BUS,
  TRAM,
  FERRY,
  AIRPLANE,
  CABLE_CAR,
  GONDOLA,
  FUNICULAR,
  TROLLEYBUS,
  MONORAIL,
  /**
   * Not yet supported.
   */
  FLEXIBLE;

  private static final EnumSet<TransitMainMode> ON_STREET_MODES = EnumSet.of(
          COACH, BUS, TROLLEYBUS
  );


  public boolean onStreet() {
    return ON_STREET_MODES.contains(this);
  }
}
