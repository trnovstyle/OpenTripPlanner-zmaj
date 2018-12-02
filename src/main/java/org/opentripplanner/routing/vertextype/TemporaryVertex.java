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

package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Vertex;

/**
 * Marker interface for temporary vertices.
 * <p/>
 * Remember to use the {@link #dispose(Vertex)} to delete the temporary vertex
 * from the main graph after use.
 */
public interface TemporaryVertex {
    boolean isEndVertex();

    /**
     * This method traverse the subgraph of temporary vertices, and cuts that subgraph off from the
     * main graph at each point it encounters a non-temporary vertexes. OTP then holds no
     * references to the temporary subgraph and it is garbage collected.
     * <p/>
     * Note! If the {@code vertex} is NOT a TemporaryVertex the method returns. No action taken.
     *
     * @param vertex Vertex part of the temporary part of the graph.
     */
    static void dispose(Vertex vertex) {
        TemporaryVertexDispose.dispose(vertex);
    }
}
