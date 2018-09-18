package org.opentripplanner.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ExternalizableSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import gnu.trove.impl.hash.TIntHash;
import org.apache.commons.lang3.NotImplementedException;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static io.protostuff.MapSchema.MessageFactories.HashMap;

public class KryoGraphSerializer implements GraphSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(KryoGraphSerializer.class);
    private final Kryo kryo;

    /**
     * Make this and load level part of the interface?
     */
    private boolean debugData = true;

    public KryoGraphSerializer() {
        this.kryo = makeKryo();
    }

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {
        Input input = new Input(inputStream);
        Kryo kryo = makeKryo();
        Graph graph = (Graph) kryo.readClassAndObject(input);
        LOG.debug("Basic graph info read.");
        if (graph.graphVersionMismatch()) {
            throw new RuntimeException("Graph version mismatch detected.");
        }

        GraphWrapper graphWrapper = new GraphWrapper();
        graphWrapper.graph = graph;
//        if (level == LoadLevel.BASIC) {
//            return graph;
//        }
        // Vertex edge lists are transient to avoid excessive recursion depth during serialization.
        // vertex list is transient because it can be reconstructed from edges.
        LOG.debug("Loading edges...");

        graphWrapper.edges = (ArrayList<Edge>) kryo.readClassAndObject(input);
        LOG.info("Main graph read. |V|={} |E|={}", graph.countVertices(), graph.countEdges());



        if (debugData) {
            graphWrapper.graphBuilderAnnotations = (List<GraphBuilderAnnotation>) kryo.readClassAndObject(input);
            LOG.debug("Debug info read.");
        } else {
            LOG.warn("Did not read debug data");
        }
        return graphWrapper;

    }

    @Override
    public void serialize(GraphWrapper graphWrapper, OutputStream outputStream) {
        Graph graph = graphWrapper.graph;

        LOG.debug("Consolidating edges...");
        Output output = new Output(outputStream);
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
        kryo.writeClassAndObject(output, graph);
        kryo.writeClassAndObject(output, edges);
        if (debugData) {
            // should we make debug info generation conditional?
            LOG.debug("Writing debug data...");
            kryo.writeClassAndObject(output, graph.getGraphBuilderAnnotations());
            kryo.writeClassAndObject(output, graph.getVertexById());
            kryo.writeClassAndObject(output, graph.getEdgeById());
        } else {
            LOG.debug("Skipping debug data.");
        }
        output.close();
        LOG.info("Graph written.");
        // Summarize serialized classes and associated serializers:
        // ((InstanceCountingClassResolver) kryo.getClassResolver()).summarize();
    }

    public static Kryo makeKryo() {
        // For generating a histogram of serialized classes with associated serializers:
        // Kryo kryo = new Kryo(new InstanceCountingClassResolver(), new MapReferenceResolver(), new DefaultStreamFactory());
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        kryo.addDefaultSerializer(TIntHash.class, ExternalizableSerializer.class);
        //kryo.register(TIntArrayList.class, new TIntArrayListSerializer());
        kryo.register(BitSet.class, new JavaSerializer());
        // OBA uses unmodifiable collections, but those classes have package-private visibility. Workaround.
        try {
            Class<?> unmodifiableCollection = Class.forName("java.util.Collections$UnmodifiableCollection");
            kryo.addDefaultSerializer(unmodifiableCollection , UnmodifiableCollectionsSerializer.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new SerializingInstantiatorStrategy()));
        return kryo;
    }
}
