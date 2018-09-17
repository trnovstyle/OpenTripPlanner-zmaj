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

package org.opentripplanner.routing.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.diff.DiffPrinter;
import org.opentripplanner.common.diff.Difference;
import org.opentripplanner.common.diff.GenericDiffConfig;
import org.opentripplanner.common.diff.GenericObjectDiffer;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.services.notes.StaticStreetNotesSource;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.serializer.GraphSerializerService;
import org.opentripplanner.serializer.GraphWrapper;
import org.opentripplanner.standalone.GraphBuilderParameters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ProtoStuffTest extends TestCase {

    AStar aStar = new AStar();

    private static Graph graph;

    private GenericObjectDiffer genericObjectDiffer = new GenericObjectDiffer();
    private GenericDiffConfig genericDiffConfig = GenericDiffConfig.builder()
            .ignoreFields(Sets.newHashSet("graphBuilderAnnotations", "streetNotesService", "vertexById", "vertices", "turnRestrictions", "stopClusterMode", "timeZone", "_services"))
            .identifiers(Sets.newHashSet("id", "index"))
            .useEqualsBuilder(Sets.newHashSet(TurnRestriction.class, StaticStreetNotesSource.class))
            .build();
    private DiffPrinter diffPrinter = new DiffPrinter();


    @Override
    public void setUp() {

        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_non_final_pojos", "true");
        System.setProperty("protostuff.runtime.allow_null_array_element", "true");

        GraphBuilder graphBuilder = new GraphBuilder();

        List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();

//         OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(new File("norway-latest.osm.pbf"));
        OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(new File(ConstantsForTests.OSLO_MINIMAL_OSM));
        osmProviders.add(osmProvider);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
        DefaultStreetEdgeFactory streetEdgeFactory = new DefaultStreetEdgeFactory();
        osmModule.edgeFactory = streetEdgeFactory;
        osmModule.skipVisibility = true;
        graphBuilder.addModule(osmModule);
        List<GtfsBundle> gtfsBundles = Lists.newArrayList();

//        GtfsBundle gtfsBundle = new GtfsBundle(new File("rb_norway-aggregated-gtfs.zip"));
        GtfsBundle gtfsBundle = new GtfsBundle(new File(ConstantsForTests.PORTLAND_GTFS));
        gtfsBundle.linkStopsToParentStations = true;
        gtfsBundle.parentStationTransfers = true;
        gtfsBundles.add(gtfsBundle);
        GtfsModule gtfsModule = new GtfsModule(gtfsBundles);
        graphBuilder.addModule(gtfsModule);
        graphBuilder.addModule(new StreetLinkerModule());
        graphBuilder.serializeGraph = false;
        graphBuilder.run();

        graph = graphBuilder.getGraph();
        graph.index(new DefaultStreetVertexIndexFactory());
    }

    @Test
    public void testProtoStuff() throws IOException, IllegalAccessException {
        testSerializeDeserialize(GraphSerializerService.PROTOSTUFF);
    }


    @Test
    @Ignore
    public void testKryo() throws IOException, IllegalAccessException {
        testSerializeDeserialize(GraphSerializerService.KRYO);
    }

    public void testSerializeDeserialize(String implementation) throws IOException, IllegalAccessException {

        // Creates graph wrapper object from graph.
        GraphWrapper graphWrapper = new GraphWrapper();
        graphWrapper.edges = new ArrayList<>(graph.getEdges());
        graphWrapper.graph = graph;

        String protoStuffFileName = "graph."+implementation.toLowerCase();
        File protostuffFile = instantiateFileDeleteOnExit(protoStuffFileName);

        GraphSerializerService graphDeserializerService = new GraphSerializerService(implementation);

        graphDeserializerService.serialize(graphWrapper, protostuffFile);

        long serializeBack = System.currentTimeMillis();

        GraphWrapper graphWrapperFromProtostuff = graphDeserializerService.deserialize(protostuffFile);

        System.out.println("Deserialized from protostuff in  " + (System.currentTimeMillis() - serializeBack) + " ms");

        assertNotNull("The deserialized graph wrapper object shall not be null", graphWrapperFromProtostuff);

        assertTrue("Edges must not be empty", graphWrapperFromProtostuff.edges.size() > 0);

        assertNotNull("Graph object itself must not be empty", graphWrapperFromProtostuff.graph);

        System.out.println("Number of edges: " + graphWrapperFromProtostuff.edges.size());

        // This instantiates some of the edges
        // It is related to how protostuff deserializes field instantiated arrays?
        for (Edge e : graphWrapperFromProtostuff.edges) {

            if (e.fromv.incoming == null) {
                e.fromv.incoming = new Edge[0];
            }
            if (e.fromv.outgoing == null) {
                e.fromv.outgoing = new Edge[0];
            }

            if (e.tov.incoming == null) {
                e.tov.incoming = new Edge[0];
            }

            if (e.tov.outgoing == null) {
                e.tov.outgoing = new Edge[0];
            }


            e.fromv.addOutgoing(e);
            e.tov.addIncoming(e);

            graphWrapperFromProtostuff.graph.vertices.put(e.getFromVertex().getLabel(), e.getFromVertex());
            graphWrapperFromProtostuff.graph.vertices.put(e.getToVertex().getLabel(), e.getToVertex());
        }


        graphWrapperFromProtostuff.graph.index(new DefaultStreetVertexIndexFactory());


        System.out.println("Comparing graph object after deserializing it from protostuff");

        // Using the generic object differ is something we do only for testing protostuff to check differences
        List<Difference> differences = genericObjectDiffer.compareObjects(graph, graphWrapperFromProtostuff.graph, genericDiffConfig);
        System.out.println(diffPrinter.diffListToString(differences));
        assertTrue(differences.isEmpty());

        //testKissAndRide(edgeInfoFromProtostuff.graph);

    }

    /**
     * Creates protostuff file. Deletes existing file if present. Deletes on exit is enabled.
     *
     * @param filename
     * @return
     */
    private File instantiateFileDeleteOnExit(String filename) {
        File protostuffFile = new File(filename);
        protostuffFile.deleteOnExit();


        if (protostuffFile.exists()) {
            protostuffFile.delete();
        }
        return protostuffFile;
    }

    public void testKissAndRide(Graph graphToUse) {
        RoutingRequest options = new RoutingRequest();
        options.dateTime = System.currentTimeMillis();
        options.from = new GenericLocation(59.9113032, 10.7489964);
        options.to = new GenericLocation(59.90808, 10.607298);
        options.setNumItineraries(1);
        options.setRoutingContext(graphToUse);
        options.kissAndRide = true;
        options.modes = TraverseModeSet.allModes();
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPaths().get(0);

        // Car leg before transit leg
        boolean carLegSeen = false;
        boolean transitLegSeen = false;
        for (int i = 0; i < path.states.size(); i++) {
            TraverseMode mode = path.states.get(i).getBackMode();
            if (mode != null) {
                assertFalse(transitLegSeen && mode.isDriving());
                if (mode.isDriving()) {
                    carLegSeen = true;
                }
                if (mode.isTransit()) {
                    transitLegSeen = true;
                }
            }
        }
        assertTrue(carLegSeen && transitLegSeen);
    }
}
