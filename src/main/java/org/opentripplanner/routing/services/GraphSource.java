package org.opentripplanner.routing.services;

import org.opentripplanner.standalone.Router;

/**
 * A class responsible of graph creation / ownership.
 * 
 */
public interface GraphSource {
    /**
     * @return The router containing a graph object. Delegates to the Router lifecycle manager the
     *         startup and shutdown of the graph.
     */
    Router getRouter();

    /**
     * Looad the graph from it's source.
     */
    void load();
}
