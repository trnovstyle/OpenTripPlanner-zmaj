package org.opentripplanner.analyst.core;

import org.opentripplanner.routing.graph.Vertex;

public class Sample {

    public final int d0, d1; // TODO change from times to distances.

    public final Vertex v0, v1;

    public Sample(Vertex v0, int d0, Vertex v1, int d1) {
        this.v0 = v0;
        this.d0 = d0;
        this.v1 = v1;
        this.d1 = d1;
    }

    public String toString() {
        return String.format("Sample: %s at %d meters or %s at %d meters\n", v0, d0, v1, d1);
    }

}

