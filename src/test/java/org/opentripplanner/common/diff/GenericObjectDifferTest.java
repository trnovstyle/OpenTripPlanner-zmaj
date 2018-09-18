package org.opentripplanner.common.diff;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.notes.StaticStreetNotesSource;

import java.util.List;

public class GenericObjectDifferTest {

    private GenericObjectDiffer genericObjectDiffer = new GenericObjectDiffer();

    private GenericDiffConfig genericDiffConfig = GenericDiffConfig.builder()
            .ignoreFields(Sets.newHashSet("graphBuilderAnnotations", "streetNotesService", "vertexById"))
            .identifiers(Sets.newHashSet("id", "index"))
            .useEqualsBuilder(Sets.newHashSet(TurnRestriction.class, StaticStreetNotesSource.class, Vertex.class))
            .build();

    private DiffPrinter diffPrinter = new DiffPrinter();

    @Test
    public void testDiff() throws IllegalAccessException {

        Graph graph = new Graph();
        Graph graph2 = new Graph();

        List<Difference> differences = genericObjectDiffer.compareObjects(graph, graph2, genericDiffConfig);

        System.out.println(diffPrinter.diffListToString(differences));
    }

}