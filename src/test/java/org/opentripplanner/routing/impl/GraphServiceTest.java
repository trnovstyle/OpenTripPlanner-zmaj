package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junit.framework.TestCase;
import org.junit.Test;
import org.opentripplanner.graph_builder.module.EmbedConfig;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class GraphServiceTest extends TestCase {
    private static final String GRAPH_FILENAME = "Graph.obj";

    File basePath;

    Graph emptyGraph;

    Graph smallGraph;

    byte[] emptyGraphData;

    byte[] smallGraphData;

    @Override
    protected void setUp() throws IOException {
        // Ensure a dummy disk location exists
        basePath = new File("test_graphs");
        if (!basePath.exists())
            basePath.mkdir();

        // Create an empty graph and it's serialized form
        emptyGraph = new Graph();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        emptyGraph.save(baos, basePath.getPath());
        emptyGraphData = baos.toByteArray();

        // Create a small graph with 2 vertices and one edge and it's serialized form
        smallGraph = new Graph();
        StreetVertex v1 = new IntersectionVertex(smallGraph, "v1", 0, 0);
        StreetVertex v2 = new IntersectionVertex(smallGraph, "v2", 0, 0.1);
        new StreetEdge(v1, v2, null, "v1v2", 11000, StreetTraversalPermission.PEDESTRIAN, false);
        baos = new ByteArrayOutputStream();
        smallGraph.save(baos, basePath.getPath());
        smallGraphData = baos.toByteArray();
    }

    @Override
    protected void tearDown() throws FileNotFoundException {
        deleteRecursive(basePath);
        basePath = null;
    }

    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists())
            throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

    @Test
    public final void testGraphServiceMemory() {

        GraphService graphService = new GraphService();
        graphService.registerGraph("A", new MemoryGraphSource("A", emptyGraph));
        assertEquals(1, graphService.getRouterIds().size());

        Graph graph = graphService.getRouter("A").graph;
        assertNotNull(graph);
        assertEquals(emptyGraph, graph);
        assertEquals("A", emptyGraph.routerId);

        try {
            graph = graphService.getRouter("inexistant").graph;
            assertTrue(false); // Should not be there
        } catch (GraphNotFoundException e) {
        }

        graphService.setDefaultRouterId("A");
        graph = graphService.getRouter().graph;

        assertEquals(emptyGraph, graph);

        graphService.registerGraph("B", new MemoryGraphSource("B", smallGraph));
        assertEquals(2, graphService.getRouterIds().size());

        graph = graphService.getRouter("B").graph;
        assertNotNull(graph);
        assertEquals(smallGraph, graph);
        assertEquals("B", graph.routerId);
    }

    @Test
    public final void testGraphServiceFile() throws IOException {
        new File(basePath, "A").mkdirs();

        // Create a GraphService and a GraphSourceFactory
        GraphService graphService = new GraphService();

        save("A", new ByteArrayInputStream(emptyGraphData));

        // Check if the graph has been saved
        assertTrue(new File(new File(basePath, "A"), GRAPH_FILENAME).canRead());

        // Register this empty graph, reloading it from disk
        graphService.registerGraph("A",
                createGraphSource("A"));

        // Check if the loaded graph is the one we saved earlier
        Graph graph = graphService.getRouter("A").graph;
        assertNotNull(graph);
        assertEquals("A", graph.routerId);
        assertEquals(0, graph.getVertices().size());
        assertEquals(1, graphService.getRouterIds().size());
    }

    @Test
    public final void testGraphServiceMemoryRouterConfig () throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode buildConfig = MissingNode.getInstance();
        ObjectNode routerConfig = mapper.createObjectNode();
        routerConfig.put("timeout", 8);
        EmbedConfig embedConfig = new EmbedConfig(buildConfig, routerConfig);
        embedConfig.buildGraph(emptyGraph, null);

        GraphService graphService = new GraphService();
        graphService.registerGraph("A", new MemoryGraphSource("A", emptyGraph));
        assertEquals(1, graphService.getRouterIds().size());

        Graph graph = graphService.getRouter("A").graph;
        assertNotNull(graph);
        assertEquals(emptyGraph, graph);
        assertEquals("A", emptyGraph.routerId);

        JsonNode graphRouterConfig = mapper.readTree(graph.routerConfig);
        assertEquals(graphRouterConfig, routerConfig);
        assertEquals(graphRouterConfig.get("timeout"), routerConfig.get("timeout"));
    }

    private GraphSource createGraphSource(String routerId) {
        return InputStreamGraphSource.newFileGraphSource(routerId, basePath(routerId));
    }

    /**
     * Used for testing.
     */
    private void save(String routerId, InputStream is) {
        try(OutputStream out = new FileOutputStream(new File(basePath(routerId), GRAPH_FILENAME))) {
            is.transferTo(out);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex.getLocalizedMessage(), ex);
        }
    }

    private File basePath(String routerId) {
        return new File(basePath, routerId);
    }
}
