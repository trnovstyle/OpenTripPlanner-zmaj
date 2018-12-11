/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.analyst.request;

import com.google.common.collect.Iterables;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.OsmVertex;

import java.util.*;

public class SampleFactory implements SampleSource {

    public SampleFactory(Graph graph) {
        this.graph = graph;
        this.setSearchRadiusM(500);
    }

    private Graph graph;

    private double searchRadiusM;
    private double searchRadiusLat;

    /** When are two vertices considered equidistant and the origin should be moved slightly to avoid numerical issues? */
    private final double EPSILON = 1e-10;

    private void setSearchRadiusM(double radiusMeters) {
        this.searchRadiusM = radiusMeters;
        this.searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(searchRadiusM);
    }

    @Override
    /** implements SampleSource interface */
    public Sample getSample(double lon, double lat) {
        Coordinate c = new Coordinate(lon, lat);
        // query always returns a (possibly empty) list, but never null
        Envelope env = new Envelope(c);
        // find scaling factor for equirectangular projection
        double xscale = Math.cos(c.y * Math.PI / 180);
        env.expandBy(searchRadiusLat / xscale, searchRadiusLat);
        @SuppressWarnings("unchecked")
        Collection<Vertex> vertices = graph.streetIndex.getVerticesForEnvelope(env);

        // make sure things are in the radius
        final TObjectDoubleMap<Vertex> distances = new TObjectDoubleHashMap<>();

        for (Vertex v : vertices) {
            if (!(v instanceof OsmVertex)) continue;

            // figure ersatz distance
            double dx = (lon - v.getLon()) * xscale;
            double dy = lat - v.getLat();
            distances.put(v, dx * dx + dy * dy);
        }


        List<Vertex> sorted = new ArrayList<>();

        for (Vertex input : vertices) {
            if (!(input instanceof OsmVertex &&
                    distances.get(input) < searchRadiusLat * searchRadiusLat))
                continue;

            for (StreetEdge e : Iterables.filter(input.getOutgoing(), StreetEdge.class)) {
                if (e.canTraverse(new TraverseModeSet(TraverseMode.WALK))) {
                    sorted.add(input);
                    break;
                }
            }
        }

        // sort list by distance
        sorted.sort((o1, o2) -> {
            double d1 = distances.get(o1);
            double d2 = distances.get(o2);
            return Double.compare(d1, d2);
        });

        Vertex v0, v1;

        if (sorted.isEmpty())
            return null;
        else if (sorted.size() <= 2) {
            v0 = sorted.get(0);
            v1 = sorted.size() > 1 ? sorted.get(1) : null;

        }
        else {
            int vxi = 0;

            // Group them by distance
            Vertex[] vx = new Vertex[2];

            ArrayList<Vertex> grouped = new ArrayList<>();

            // here's the idea: accumulate vertices by distance, waiting until we find a gap
            // of at least EPSILON. Once we've done that, break ties using labels (which are OSM IDs).
            for (int i = 0; i < sorted.size(); i++) {
                if (vxi >= 2) break;

                if (grouped.isEmpty()) {
                    grouped.add(sorted.get(i));
                }
                else {
                    double dlast = distances.get(sorted.get(i - 1));
                    double dthis = distances.get(sorted.get(i));
                    if (dthis - dlast < EPSILON) {
                        grouped.add(sorted.get(i));
                    } else {
                        // we have a distinct group of vertices
                        // sort them by OSM IDs
                        // this seems like it would be slow but keep in mind that it will only do any work
                        // when there are multiple members of a group, which is relatively rare.
                        grouped.sort(Comparator.comparing(Vertex::getLabel));

                        // then loop over the list until it's empty or we've found two vertices
                        int gi = 0;
                        while (vxi < 2 && gi < grouped.size()) {
                            vx[vxi++] = grouped.get(gi++);
                        }

                        // get ready for the next group
                        grouped.clear();
                    }
                }
            }
            v0 = vx[0];
            v1 = vx[1];
        }

        double d0 = v0 != null ? SphericalDistanceLibrary.distance(v0.getLat(),  v0.getLon(), lat, lon) : 0;
        double d1 = v1 != null ? SphericalDistanceLibrary.distance(v1.getLat(),  v1.getLon(), lat, lon) : 0;
        return new Sample(v0, (int) d0, v1, (int) d1);
    }
}
