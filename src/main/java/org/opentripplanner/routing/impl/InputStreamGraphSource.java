/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.opentripplanner.standalone.datastore.configure.DataStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * The primary implementation of the GraphSource interface. The graph is loaded from a serialized
 * graph from a given source.
 */
public class InputStreamGraphSource implements GraphSource {
    private static final Logger LOG = LoggerFactory.getLogger(InputStreamGraphSource.class);

    /**
     * Delay before starting to load a graph after the last modification time. In case of writing,
     * we expect graph last modification time to be updated at at least that frequency. If not, you
     * can either increase this value, or use an atomic move when copying the file.
     * */
    private Router router;

    private String routerId;

    private OtpDataStore store;

    /**
     * @return A GraphSource loading graph from the file system under a base path.
     */
    public static InputStreamGraphSource newFileGraphSource(String routerId, File path) {
        return new InputStreamGraphSource(
                routerId,
                new DataStoreConfig(path).open()
        );
    }

    private InputStreamGraphSource(String routerId, OtpDataStore store) {
        this.routerId = routerId;
        this.store = store;
    }

    @Override
    public Router getRouter() {
        // We synchronize on pre-evict mutex in case we are in the middle of reloading in pre-evict
        // mode. In that case we must make the client wait until the new graph is loaded, because
        // the old one is gone to the GC. Performance hit should be low as getGraph() is not called
        // often.
        return router;
    }

    @Override
    public void load() {
        // We synchronize on 'this' to prevent multiple reloads from being called at the same time
        synchronized (this) {
            router = loadGraph();
        }
    }

    /**
     * Do the actual operation of graph loading. Load configuration if present, and startup the
     * router with the help of the router lifecycle manager.
     */
    private Router loadGraph() {
        final Graph newGraph;
        DataSource graph = store.getSource(GraphBuilder.GRAPH_FILENAME, FileType.GRAPH);

        if(!graph.exist()) {
            LOG.warn("Graph file not found for routerId '{}': {}", routerId, store.path());
            return null;
        }

        try (InputStream is = graph.asInputStream()) {
            LOG.info("Loading graph...");
            try {
                newGraph = Graph.load(is);
            } catch (Exception ex) {
                LOG.error("Exception while loading graph '{}'.", routerId, ex);
                return null;
            }

            newGraph.routerId = (routerId);
        } catch (IOException e) {
            LOG.warn("Graph file readable for routerId '{}': {}", routerId, store.path(), e);
            return null;
        }

        // Even if a config file is not present on disk one could be bundled inside.
        JsonNode config = store.routerConfigParameters();
        Router newRouter = new Router(routerId, newGraph);
        newRouter.startup(config);
        return newRouter;
    }
}
