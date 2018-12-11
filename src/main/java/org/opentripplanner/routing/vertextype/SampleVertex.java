package org.opentripplanner.routing.vertextype;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A vertex representing a Sample in the Analyst sense -- a temporary and nondestructive linkage of
 * a single geographic point into the street network.
 */
public class SampleVertex extends StreetVertex implements TemporaryVertex  {
    private static final long serialVersionUID = 1L;

    /** The index is used to give each instance a unique label. */
    private static AtomicInteger LABEL_INDEX_COUNTER = new AtomicInteger(0);

    public SampleVertex (Coordinate c) {
        // calling constructor with null graph means this vertex is temporary
        super(null, "sample-" + LABEL_INDEX_COUNTER.incrementAndGet(), c, null);
    }

    @Override
    public boolean isEndVertex() {
        return false;
    }

    @Override
    public void addIncoming(Edge e) {
        if (!(e instanceof TemporaryEdge)) {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary sample vertex.");
        }

        super.addIncoming(e);
    }

    @Override
    public void addOutgoing(Edge e) {
        if (!(e instanceof TemporaryEdge)) {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary sample vertex.");
        }

        super.addOutgoing(e);
    }
}
