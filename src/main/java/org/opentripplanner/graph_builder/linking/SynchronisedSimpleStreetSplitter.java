package org.opentripplanner.graph_builder.linking;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Wrapper class around SimpleStreetSplitter making it safe to share one instance for updaters.
 *
 * SimpleStreetSplitter is commented with "Be aware that only one SimpleStreetSplitter should be
 * active on a graph at any given time.". This was not respected by bikeRental / bikePark / carPark updaters which were initializing one splitter per updater and using them in parallel.
 *
 * Building splitter is also slow, so we should avoid doing this more than once.
 */
public class SynchronisedSimpleStreetSplitter {

    private Object lock = "lock";

    private SimpleStreetSplitter simpleStreetSplitter;

    public SynchronisedSimpleStreetSplitter(Graph graph) {
        this.simpleStreetSplitter = new SimpleStreetSplitter(graph);
    }


    public boolean link(Vertex vertex) {
        synchronized (lock) {
            return simpleStreetSplitter.link(vertex);
        }
    }

    public boolean link(Vertex vertex, TraverseMode traverseMode, RoutingRequest options) {
        synchronized (lock) {
            return simpleStreetSplitter.link(vertex, traverseMode, options);
        }
    }

}
