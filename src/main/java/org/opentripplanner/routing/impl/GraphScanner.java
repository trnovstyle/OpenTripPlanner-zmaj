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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scan for graphs under the base directory and auto-register them.
 */
public class GraphScanner {

    private static final Logger LOG = LoggerFactory.getLogger(GraphScanner.class);

    /** Where to look for graphs. Defaults to 'graphs' under the OTP server base path. */
    public File basePath = null;

    /** A list of routerIds to automatically register and load at startup */
    public List<String> autoRegister;

    /** The default router, none by default */
    public String defaultRouterId = null;

    /** The GraphService where register graphs to */
    private GraphService graphService;

    public GraphScanner(GraphService graphService, File basePath) {
        this.graphService = graphService;
        this.basePath = basePath;
    }

    /**
     * Based on the autoRegister list, automatically register all routerIds for which we can find a
     * graph file in a subdirectory of the resourceBase path. Also register and load the graph for
     * the defaultRouterId and warn if no routerIds are registered.
     */
    public void startup() {
        Set<String> routerIds = new HashSet<String>();
        if (autoRegister != null)
            routerIds.addAll(autoRegister);
        if (defaultRouterId != null) {
            graphService.setDefaultRouterId(defaultRouterId);
            routerIds.add(defaultRouterId);
        }
        if (!routerIds.isEmpty()) {
            LOG.info("Attempting to automatically register routerIds {}", autoRegister);
            LOG.info("Graph files will be sought in paths relative to {}", basePath);
            for (String routerId : routerIds) {
                InputStreamGraphSource graphSource = InputStreamGraphSource.newFileGraphSource(
                        routerId, getBasePath(routerId));
                graphService.registerGraph(routerId, graphSource);
            }
        } else {
            LOG.info("No list of routerIds was provided for automatic registration.");
        }
    }

    private File getBasePath(String routerId) {
        return new File(basePath, routerId);
    }
}
