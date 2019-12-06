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

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.opentripplanner.api.model.RouterInfo;
import org.opentripplanner.api.model.RouterList;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.opentripplanner.api.resource.ServerInfo.Q;

/**
 * This REST API endpoint allows remotely loading, reloading, and evicting graphs on a running server.
 * 
 * A GraphService maintains a mapping between routerIds and specific Graph objects.
 * The HTTP verbs are used as follows to manipulate that mapping:
 * 
 * GET - see the registered routerIds and Graphs, verify whether a particular routerId is registered
 * PUT - create or replace a mapping from a routerId to a Graph loaded from the server filesystem
 * POST - create or replace a mapping from a routerId to a serialized Graph sent in the request
 * DELETE - de-register a routerId, releasing the reference to the associated graph
 * 
 * The HTTP request URLs are of the form /ws/routers/{routerId}, where the routerId is optional. 
 * If a routerId is supplied in the URL, the verb will act upon the mapping for that specific 
 * routerId. If no routerId is given, the verb will act upon all routerIds currently registered.
 * 
 * For example:
 * 
 * GET http://localhost/otp-rest-servlet/ws/routers
 * will retrieve a list of all registered routerId -> Graph mappings and their geographic bounds.
 * 
 * GET http://localhost/otp-rest-servlet/ws/routers/london
 * will return status code 200 and a brief description of the 'london' graph including geographic 
 * bounds, or 404 if the 'london' routerId is not registered.
 * 
 * PUT http://localhost/otp-rest-servlet/ws/routers
 * will reload the graphs for all currently registered routerIds from disk.
 * 
 * PUT http://localhost/otp-rest-servlet/ws/routers/paris
 * will load a Graph from a sub-directory called 'paris' and associate it with the routerId 'paris'.
 * 
 * DELETE http://localhost/otp-rest-servlet/ws/routers/paris
 * will release the Paris Graph and de-register the 'paris' routerId.
 * 
 * DELETE http://localhost/otp-rest-servlet/ws/routers
 * will de-register all currently registered routerIds.
 * 
 * The GET methods are not secured, but all other methods are secured under ROLE_ROUTERS.
 * See documentation for individual methods for additional parameters.
 */
@Path("/routers")
@PermitAll // exceptions on methods
public class Routers {

    private static final Logger LOG = LoggerFactory.getLogger(Routers.class);

    @Context OTPServer otpServer;

    private static boolean flaggedAsReady = false;

    /** 
     * Returns a list of routers and their bounds. 
     * @return a representation of the graphs and their geographic bounds, in JSON or XML depending
     * on the Accept header in the HTTP request.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public RouterList getRouterIds() {
        RouterList routerList = new RouterList();
        for (String id : otpServer.getRouterIds()) {
            RouterInfo routerInfo = getRouterInfo(id);
            if (routerInfo != null) {
                // Router could have been evicted in the meantime
                routerList.routerInfo.add(routerInfo);
            }
        }
        return routerList;
    }

    /**
     * Checks that graphs are ready, and that blocking PollingUpdaters reports as initialized
     */
    @GET @Path("/ready")
    @Produces({ MediaType.TEXT_PLAIN})
    public Response isReady() {
        boolean isRouterReady = false;
        List<String> waitingUpdaters = new ArrayList<>();
        for (String id : otpServer.getRouterIds()) {
            Router router = otpServer.getRouter(id);
            if (router != null) {
                // Router could have been evicted in the meantime
                Graph graph = router.graph;

                isRouterReady = true;
                if (graph.updaterManager != null && graph.updaterManager.getUpdaterList() != null) {
                    for (GraphUpdater updater : graph.updaterManager.getUpdaterList()) {
                        if (updater instanceof PollingGraphUpdater) {
                            if (!((PollingGraphUpdater) updater).isReady()) {
                                waitingUpdaters.add(((PollingGraphUpdater) updater).getType());
                            }
                        }
                    }
                }
            }
        }
        if (!isRouterReady) {
            LOG.info("Graph not ready.");
            throw new WebApplicationException(Response.status(Status.NOT_FOUND)
                    .entity("Graph not ready.\n").type("text/plain")
                    .build());
        }
        if (!waitingUpdaters.isEmpty()) {
            LOG.info("Graph ready, waiting for updaters: {}", waitingUpdaters);
            throw new WebApplicationException(Response.status(Status.NOT_FOUND)
                    .entity("Graph ready, waiting for updaters: " + waitingUpdaters + "\n").type("text/plain")
                    .build());
        }
        if (!flaggedAsReady) {
            flaggedAsReady = true;
            LOG.info("Graph is now ready.");
        }
        return Response.status(Status.OK)
                .entity("Ready.\n").type("text/plain")
                .build();
    }

    /** 
     * Returns the bounds for a specific routerId, or verifies whether it is registered. 
     * @returns status code 200 if the routerId is registered, otherwise a 404.
     */
    @GET @Path("{routerId}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public RouterInfo getGraphId(@PathParam("routerId") String routerId) {
        // factor out build one entry
        RouterInfo routerInfo = getRouterInfo(routerId);
        if (routerInfo == null)
            throw new WebApplicationException(Response.status(Status.NOT_FOUND)
                    .entity("Graph id '" + routerId + "' not registered.\n").type("text/plain")
                    .build());
        return routerInfo;
    }
    
    private RouterInfo getRouterInfo(String routerId) {
        try {
            Router router = otpServer.getRouter(routerId);
            //new router is created here instead of loaded from router
            //since routerId here isn't always the same as routerId when Router was created
            //at least this happens in RoutersTest
            return new RouterInfo(routerId, router.graph);
        } catch (GraphNotFoundException e) {
            return null;
        }
    }

    /** 
     * Reload the graphs for all registered routerIds from disk.
     */
    @RolesAllowed({ "ROUTERS" })
    @PUT @Produces({ MediaType.APPLICATION_JSON })
    public Response reloadGraphs(@QueryParam("path") String path,
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict,
            @QueryParam("force") @DefaultValue("true") boolean force) {
        LOG.debug("DISABLED - Attempting to reload all graphs");
        return Response.status(Status.BAD_REQUEST).entity(
                "This endpoint is not supported any more"
        ).build();
    }

    /** 
     * Load the graph for the specified routerId from disk.
     * @param preEvict before reloading each graph, evict the existing graph. This will prevent 
     * memory usage from increasing during the reload, but routing will be unavailable on this 
     * routerId for the duration of the operation.
     */
    @RolesAllowed({ "ROUTERS" })
    @PUT @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    public Response putGraphId(@PathParam("routerId") String routerId,
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict) {
        LOG.debug("DISABLED - Attempting to load graph '{}' from server's local filesystem.", routerId);
        return Response.status(Status.BAD_REQUEST).entity(
                "This endpoint is not supported any more"
        ).build();
    }

    /** 
     * Deserialize a graph sent with the HTTP request as POST data, associating it with the given 
     * routerId.
     */
    @RolesAllowed({ "ROUTERS" })
    @POST @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response postGraphOverWire (
            @PathParam("routerId") String routerId, 
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict, 
            InputStream is) {
        LOG.debug("DISABLED - deserializing graph from POST data stream...");
        return Response.status(Status.BAD_REQUEST).entity(
                "This endpoint is not supported any more"
        ).build();
    }
    
    /**
     * Build a graph from data in the ZIP file posted over the wire, associating it with the given router ID.
     * This method will be selected when the Content-Type is application/zip.
     */
    @RolesAllowed({ "ROUTERS" })
    @POST @Path("{routerId}") @Consumes({"application/zip"})
    @Produces({ MediaType.TEXT_PLAIN })
    public Response buildGraphOverWire (
            @PathParam("routerId") String routerId,
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict,
            InputStream input) {
        LOG.debug("DISABLED - build graph from POST data stream...");
        return Response.status(Status.BAD_REQUEST).entity(
                "This endpoint is not supported any more"
        ).build();
    }
    
    /** 
     * Save the graph data, but don't load it in memory. The file location is based on routerId.
     * If the graph already exists, the graph will be overwritten.
     */
    @RolesAllowed({ "ROUTERS" })
    @POST @Path("/save") @Produces({ MediaType.TEXT_PLAIN })
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response saveGraphOverWire (
            @QueryParam("routerId") String routerId,
            InputStream is
    ) {
        LOG.debug("DISABLED - save graph from POST data stream...");
        return Response.status(Status.BAD_REQUEST).entity(
                "This endpoint is not supported any more"
        ).build();
    }

    /** De-register all registered routerIds, evicting them from memory. */
    @RolesAllowed({ "ROUTERS" })
    @DELETE @Produces({ MediaType.TEXT_PLAIN })
    public Response deleteAll() {
        LOG.debug("DISABLED - Delete all graphs");
        return Response.status(Status.BAD_REQUEST).entity(
                "This endpoint is not supported any more"
        ).build();
    }

    /** 
     * De-register a specific routerId, evicting the associated graph from memory. 
     * @return status code 200 if the routerId was de-registered, 
     * 404 if the routerId was not registered. 
     */
    @RolesAllowed({ "ROUTERS" })
    @DELETE @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    public Response deleteGraphId(@PathParam("routerId") String routerId) {
        LOG.debug("DISABLED - Delete graph {}", routerId);
        return Response.status(Status.BAD_REQUEST).entity(
                "This endpoint is not supported any more"
        ).build();
    }
}
