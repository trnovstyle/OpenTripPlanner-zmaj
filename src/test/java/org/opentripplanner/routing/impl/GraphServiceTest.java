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
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.io.IOException;


public class GraphServiceTest extends TestCase {
    private Graph emptyGraph;
    private Graph smallGraph;

    @Override
    protected void setUp() {

        // Create an empty graph and it's serialized form
        emptyGraph = new Graph();

        // Create a small graph with 2 vertices and one edge and it's serialized form
        smallGraph = new Graph();
        StreetVertex v1 = new IntersectionVertex(smallGraph, "v1", 0, 0);
        StreetVertex v2 = new IntersectionVertex(smallGraph, "v2", 0, 0.1);
        new StreetEdge(v1, v2, null, "v1v2", 11000, StreetTraversalPermission.PEDESTRIAN, false);
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
}
