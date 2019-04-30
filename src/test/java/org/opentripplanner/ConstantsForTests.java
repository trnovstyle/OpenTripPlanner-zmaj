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

package org.opentripplanner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.edgetype.factory.TransferGraphLinker;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarService;
import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarServiceData;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

public class ConstantsForTests {

    public static final String CALTRAIN_GTFS = "src/test/resources/caltrain_gtfs.zip";

    public static final String PORTLAND_GTFS = "src/test/resources/google_transit.zip";

    public static final String KCM_GTFS = "src/test/resources/kcm_gtfs.zip";
    
    public static final String FAKE_GTFS = "src/test/resources/testagency.zip";

    public static final String FARE_COMPONENT_GTFS = "src/test/resources/farecomponent_gtfs.zip";

    public static final String VERMONT_GTFS = "src/test/resources/vermont/ruralcommunity-flex-vt-us.zip";

    public static final String VERMONT_OSM = "src/test/resources/vermont/vermont-rct.osm.pbf";

    private static ConstantsForTests instance = null;

    private Graph portlandGraph = null;

    private GtfsContext portlandContext = null;

    private Graph vermontGraph = null;

    private ConstantsForTests() {

    }

    public static ConstantsForTests getInstance() {
        if (instance == null) {
            instance = new ConstantsForTests();
        }
        return instance;
    }

    public GtfsContext getPortlandContext() {
        if (portlandGraph == null) {
            setupPortland();
        }
        return portlandContext;
    }

    public Graph getPortlandGraph() {
        if (portlandGraph == null) {
            setupPortland();
        }
        return portlandGraph;
    }

    private void setupPortland() {
        try {
            portlandGraph = new Graph();
            portlandContext = contextBuilder(ConstantsForTests.PORTLAND_GTFS)
                    .withGraphBuilderAnnotationsAndDeduplicator(portlandGraph)
                    .build();
            PatternHopFactory factory = new PatternHopFactory(portlandContext);
            factory.run(portlandGraph);
            TransferGraphLinker linker = new TransferGraphLinker(portlandGraph);
            linker.run();
            // TODO: eliminate GTFSContext
            // this is now making a duplicate calendarservicedata but it's oh so practical
            portlandGraph.putService(
                    CalendarServiceData.class,
                    createCalendarServiceData(portlandContext.getTransitBuilder())
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        StreetLinkerModule ttsnm = new StreetLinkerModule();
        ttsnm.buildGraph(portlandGraph, new HashMap<Class<?>, Object>());
    }

    public Graph getVermontGraph() {
        if (vermontGraph == null) {
            try {
                vermontGraph = new Graph();

                OpenStreetMapModule osmModule = new OpenStreetMapModule();
                AnyFileBasedOpenStreetMapProviderImpl provider = new AnyFileBasedOpenStreetMapProviderImpl();
                osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
                provider.setPath(new File(ConstantsForTests.VERMONT_OSM));
                osmModule.setProvider(provider);
                osmModule.buildGraph(vermontGraph, new HashMap<>());

                GtfsContext vermontGtfsContext = contextBuilder(ConstantsForTests.VERMONT_GTFS)
                        .withGraphBuilderAnnotationsAndDeduplicator(vermontGraph)
                        .build();

                PatternHopFactory factory = new PatternHopFactory(vermontGtfsContext);
                factory.run(vermontGraph);

                CalendarServiceData csd = createCalendarServiceData(vermontGtfsContext.getTransitBuilder());
                vermontGraph.putService(CalendarServiceData.class, csd);
                vermontGraph.updateTransitFeedValidity(csd);
                vermontGraph.hasTransit = true;

                vermontGraph.putService(
                        CalendarService.class,
                        createCalendarService(vermontGtfsContext.getTransitBuilder())
                );


                new DirectTransferGenerator(2000).buildGraph(vermontGraph, new HashMap<>());

                new StreetLinkerModule().buildGraph(vermontGraph, new HashMap<>());

                vermontGraph.index(new DefaultStreetVertexIndexFactory());

                vermontGraph.setUseFlexService(true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        return vermontGraph;
    }

    public static Graph buildGraph(String path) {
        Graph graph = new Graph();
        GtfsContext context;
        try {
            context = contextBuilder(path).build();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        PatternHopFactory factory = new PatternHopFactory(context);
        factory.run(graph);
        graph.putService(
                CalendarServiceData.class,
                createCalendarServiceData(context.getTransitBuilder())
        );
        return graph;
    }

}
