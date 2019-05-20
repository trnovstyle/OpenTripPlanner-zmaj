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

public class TransferRoutingDistanceTooLong extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Routing distance between stop %s and stop %s is %s times longer than the " +
            "euclidean distance. Street distance: %s, direct distance: %s.";

    public static final String HTMLFMT = "Routing distance between stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s\" (%s)</a> and stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s\" (%s)</a> is %s times longer than " +
            "the euclidean distance. Street distance: %s, direct distance: %s.";

    private final TransitStop origin;
    private final TransitStop destination;
    private final double directDistance;
    private final double streetDistance;
    private final double ratio;

    public TransferRoutingDistanceTooLong(TransitStop origin, TransitStop destination, double directDistance, double streetDistance, double ratio) {
        this.origin = origin;
        this.destination = destination;
        this.directDistance = directDistance;
        this.streetDistance = streetDistance;
        this.ratio = ratio;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, origin, destination, round(ratio), round2(streetDistance) , round2(directDistance));
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, origin.getStop().getLat(), origin.getStop().getLon(),
                origin.getStop().getName(), origin.getStop().getId(), destination.getStop().getLat(),
                destination.getStop().getLon(), destination.getStop().getName(), destination.getStop().getId(),
                round(ratio), round2(streetDistance), round2(directDistance));
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.origin;
    }

    private String round(Double number) {
        return String.format("%.2f", number);
    }

    private String round2(Double number) {
        return String.format("%.0f", number);
    }
}
