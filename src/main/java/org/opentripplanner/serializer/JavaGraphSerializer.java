package org.opentripplanner.serializer;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class JavaGraphSerializer implements GraphSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(JavaGraphSerializer.class);

    private final boolean debugData = true;

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {
        try {
            ObjectInputStream in = new ObjectInputStream(inputStream);
            GraphWrapper graphWrapper = new GraphWrapper();
            graphWrapper.graph = (Graph) in.readObject();
            LOG.debug("Loading edges...");
            graphWrapper.edges = (ArrayList<Edge>) in.readObject();
            LOG.debug("Loading graph builder annotations (if any)");
            graphWrapper.graphBuilderAnnotations = (List<GraphBuilderAnnotation>) in.readObject();

            return graphWrapper;

        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize incoming date", e);
        } catch (ClassNotFoundException ex) {
            LOG.error("Stored graph is incompatible with this version of OTP, please rebuild it.");
            throw new GraphSerializationException("Stored Graph version error", ex);
        }
    }

    @Override
    public void serialize(GraphWrapper graphWrapper, OutputStream outputStream) throws GraphSerializationException {

        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            Graph graph = graphWrapper.graph;

            LOG.debug("Consolidating edges...");
            // this is not space efficient
            List<Edge> edges = new ArrayList<Edge>(graph.countEdges());
            for (Vertex v : graph.getVertices()) {
                // there are assumed to be no edges in an incoming list that are not
                // in an outgoing list
                edges.addAll(v.getOutgoing());
                if (v.getDegreeOut() + v.getDegreeIn() == 0)
                    LOG.debug("vertex {} has no edges, it will not survive serialization.", v);
            }
            LOG.debug("Assigning vertex/edge ID numbers...");
            graph.rebuildVertexAndEdgeIndices();
            LOG.debug("Writing edges...");
            objectOutputStream.writeObject(graph);
            objectOutputStream.writeObject(edges);
            if (debugData) {
                // should we make debug info generation conditional?
                LOG.debug("Writing debug data...");
                objectOutputStream.writeObject(graph.getGraphBuilderAnnotations());
                objectOutputStream.writeObject(graph.getVertexById());
                objectOutputStream.writeObject(graph.getEdgeById());
            } else {
                LOG.debug("Skipping debug data.");
            }
            outputStream.close();
            LOG.info("Graph written.");
        } catch (IOException e) {
            throw new GraphSerializationException("Could not write graph", e);
        }

    }
}
