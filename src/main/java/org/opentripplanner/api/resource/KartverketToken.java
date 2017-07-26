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

package org.opentripplanner.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import com.amazonaws.util.json.JSONObject;
import org.opentripplanner.api.common.RoutingResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetch Kartverket token from server
 */
@Path("/kartverket_token/")
public class KartverketToken extends RoutingResource {

    private static final Logger LOG = LoggerFactory.getLogger(TileService.class);

    @GET @Produces("text/html")
    public Response tokenGet() throws Exception {
        String token = "newToken";

        return Response.status(200).entity(token).build();

    }

}