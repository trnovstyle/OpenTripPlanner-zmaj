package org.opentripplanner.index.transmodel;

import org.opentripplanner.standalone.Router;

import java.util.HashMap;
import java.util.Map;

/**
 * Create reusable TransmodelGraphIndexes only once per router.
 *
 * Should ideally be handled by dependency injection, but done this way to avoid interfering more than necessary
 * with core classes.
 */
public class TransmodelGraphIndexFactory {

    private static Map<String, TransmodelGraphIndex> indexPerRouterId = new HashMap<>();

    public static TransmodelGraphIndex getTransmodelGraphIndexForRouter(Router router) {
        TransmodelGraphIndex graphIndex = indexPerRouterId.get(router.id);
        if (graphIndex == null) {
            graphIndex = new TransmodelGraphIndex(router);
            indexPerRouterId.put(router.id, graphIndex);
        }
        return graphIndex;
    }
}
