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

package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class TransferCouldNotBeRouted extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Connection between stop %s and stop %s could not be routed. " +
            "Euclidean distance is %s.";

    public static final String HTMLFMT = "Connection between stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> and stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> could not be routed. " +
            "Euclidean distance is %s.";

    private final TransitStop origin;
    private final TransitStop destination;
    private final double directDistance;

    public TransferCouldNotBeRouted(TransitStop origin, TransitStop destination, double directDistance) {
        this.origin = origin;
        this.destination = destination;
        this.directDistance = directDistance;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, origin, destination, round2(directDistance));
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, origin.getStop().getLat(), origin.getStop().getLon(),
                origin.getStop().getName(), origin.getStop().getId(), destination.getStop().getLat(),
                destination.getStop().getLon(), destination.getStop().getName(), destination.getStop().getId(),
                round2(directDistance));
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.origin;
    }

    private String round2(Double number) {
        return String.format("%.0f", number);
    }
}
