package org.opentripplanner.serializer;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;

/**
 * Service for serializing and deserializing Graph objects.
 * Load and save methods are extracted from the {@link Graph}.
 * This implementation allows to switch implementation of GraphSerializer.
 * Having multiple implementations makes it easier to compare speed and size of different implementations.
 */
public class GraphSerializerService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphSerializerService.class);
    public static final String SERIALIZATION_METHOD_PROP = "serialization-method";
    public static final String PROTOSTUFF = "protostuff";
    public static final String KRYO = "kryo";

    /**
     * Load debug data ?
     */
    private boolean debugData = true;

    private final GraphSerializer graphSerializer;


    public GraphSerializerService() {
        this.graphSerializer = getGraphSerializer(System.getProperty(SERIALIZATION_METHOD_PROP));
    }

    public GraphSerializerService(String serializationMethod) {
        this.graphSerializer = getGraphSerializer(serializationMethod);
    }

    public GraphWrapper deserialize(File file) {
        try {
            LOG.info("Reading graph from file: " + file.getAbsolutePath() + " ...");
            return deserialize(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new GraphSerializationException("Cannot read file " + file.getName(), e);
        }
    }

    public GraphWrapper deserialize(InputStream is) {
        long started = System.currentTimeMillis();
        GraphWrapper graphWrapper = graphSerializer.deserialize(is);
        long spent = System.currentTimeMillis() - started;

        LOG.info("Deserialized graph using: {} in {} ms", graphSerializer.getClass().getSimpleName(), spent);

        return graphWrapper;
    }

    public void serialize(Graph graph, OutputStream outputStream) {
        GraphWrapper graphWrapper = new GraphWrapper();
        graphWrapper.graph = graph;
        serialize(graphWrapper, outputStream);
    }
    public void serialize(Graph graph, File file) {
        GraphWrapper graphWrapper = new GraphWrapper();
        graphWrapper.graph = graph;
        serialize(graphWrapper, file);
    }

    public void serialize(GraphWrapper graphWrapper, File file) {
        try {
            LOG.debug("Writing graph to file {} using {}", file.getName(), graphSerializer.getClass().getName());
            LOG.info("Writing graph " + file.getAbsolutePath() + " ...");
            serialize(graphWrapper, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new GraphSerializationException("Cannot read file " + file.getName(), e);
        }
    }

    public void serialize(GraphWrapper graphWrapper, OutputStream outputStream) {
        LOG.info("Serializing graph. Main graph size: |V|={} |E|={}", graphWrapper.graph.countVertices(), graphWrapper.graph.countEdges());
        long started = System.currentTimeMillis();
        graphSerializer.serialize(graphWrapper, outputStream);
        long spent = System.currentTimeMillis() - started;
        LOG.info("Graph serialized in {} ms", spent);
    }

    public Graph load(File file, Graph.LoadLevel level) {
        GraphWrapper graphWrapper = deserialize(file);
        return load(graphWrapper, level, new DefaultStreetVertexIndexFactory());
    }

    public Graph load(InputStream inputStream, Graph.LoadLevel level) {
        GraphWrapper graphWrapper = deserialize(inputStream);
        return load(graphWrapper, level, new DefaultStreetVertexIndexFactory());
    }

    public Graph load(InputStream inputStream, Graph.LoadLevel level, StreetVertexIndexFactory streetVertexIndexFactory) {
        GraphWrapper graphWrapper = deserialize(inputStream);
        return load(graphWrapper, level, streetVertexIndexFactory);
    }

    private Graph load(GraphWrapper graphWrapper, Graph.LoadLevel level, StreetVertexIndexFactory indexFactory) {

        // Because some fields are marked as transient
        Graph deserializedGraph = graphWrapper.graph;
        List<Edge> edges = graphWrapper.edges;
        List<GraphBuilderAnnotation> graphBuilderAnnotations = graphWrapper.graphBuilderAnnotations;

        LOG.debug("Basic graph info read.");
        if (deserializedGraph.graphVersionMismatch())
            throw new RuntimeException("Graph version mismatch detected.");
        if (level == Graph.LoadLevel.BASIC)
            return deserializedGraph;
        // vertex edge lists are transient to avoid excessive recursion depth
        // vertex list is transient because it can be reconstructed from edges

        deserializedGraph.vertices = new HashMap<>();

        for (Edge e : edges) {
            deserializedGraph.vertices.put(e.getFromVertex().getLabel(), e.getFromVertex());
            deserializedGraph.vertices.put(e.getToVertex().getLabel(), e.getToVertex());
        }

        LOG.info("Main graph read. |V|={} |E|={}", deserializedGraph.countVertices(), deserializedGraph.countEdges());
        deserializedGraph.index(indexFactory);

        if (level == Graph.LoadLevel.FULL) {
            return deserializedGraph;
        }

        if (debugData) {
            deserializedGraph.getGraphBuilderAnnotations().addAll(graphBuilderAnnotations);
            LOG.debug("Debug info read.");
        } else {
            LOG.warn("Graph file does not contain debug data.");
        }
        return deserializedGraph;


    }

    private static GraphSerializer getGraphSerializer(String serializationMethod) {

        GraphSerializer graphSerializer;
        if (PROTOSTUFF.equals(serializationMethod)) {
            graphSerializer = new ProtostuffGraphSerializer();
        } else if (KRYO.equals(serializationMethod)) {
            graphSerializer = new KryoGraphSerializer();
        } else {
            LOG.debug("Defaulting to java graph deserializer");
            graphSerializer = new JavaGraphSerializer();
        }
        LOG.debug("Using the following deserializer for graph loading: {}", graphSerializer.getClass().getSimpleName());
        return graphSerializer;
    }
}
