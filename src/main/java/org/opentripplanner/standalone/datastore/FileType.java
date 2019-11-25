package org.opentripplanner.standalone.datastore;

import java.util.EnumSet;

/**
 * Represents the different types of files that might be present in a router / graph build
 * directory. We want to detect even those that are not graph builder inputs so we can effectively
 * warn when unrecognized file types are present. This helps point out when config files have been
 * misnamed (builder-config vs. build-config).
 */
public enum FileType {
  GTFS,
  OSM,
  DEM,
  CONFIG,
  GRAPH,
  PARTIAL_GRAPH,
  NETEX,
  UNRECOGNIZED;

  /**
   * Return {@code true} if the the file is an input data file. This is GTFS, Netex, OpenStreetMap,
   * and elevation data files. Config files and graphs are not considered input data files.
   * <p>
   * At least one input data file must be present to build a graph.
   */
  public boolean isInputDataFile() {
    return EnumSet.of(GTFS, NETEX, OSM, DEM).contains(this);
  }

  /**
   * @return {@code true} if the type is a composite type, like zip-files and directories. Normal
   * file types will return {@code false}.
   */
  public boolean isCompositeInputDataFile() {
    return EnumSet.of(GTFS, NETEX).contains(this);
  }
}
