package org.opentripplanner.index.transmodel;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutorServiceExecutionStrategy;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.RoutingRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TransmodelGraphQLTest extends GtfsTest {

    @Override
    public String getFeedName() {
        return "testagency.zip";
    }

    private TransmodelIndexGraphQLSchema transmodelIndexGraphQLSchema;

    private TransmodelGraphIndex graphIndex;

    @Override
    protected void setUp() {
        super.setUp();
        router.defaultRoutingRequest = new RoutingRequest();
        graphIndex = TransmodelGraphIndexFactory.getTransmodelGraphIndexForRouter(router);
        transmodelIndexGraphQLSchema = new TransmodelIndexGraphQLSchema(router);
    }

    public void testGraphQLAuthority() {
        String query =
                "query Authority {" +
                        "    authority(id: \"agency\"){" +
                        "        name" +
                        "    }" +
                        "}";

        ExecutionResult result = new GraphQL(transmodelIndexGraphQLSchema.indexSchema, new ExecutorServiceExecutionStrategy(graph.index.threadPool)
        ).execute(query);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        assertEquals("Fake Agency", ((Map) data.get("authority")).get("name"));
    }

    public void testTravelsearchSimple() {
        String query =
                "{" +
                        "  trip(" +
                        "    from: {place:\"FEED:A\"}" +
                        "    to: {place:\"FEED:D\"}" +
                        "    numTripPatterns: 3" +
                        "    dateTime: \"2018-01-04T12:51:14.000+0100\"" +
                        "  )" +
                        "  {" +
                        "    tripPatterns {" +
                        "      startTime" +
                        "      duration" +
                        "      walkDistance" +
                        "" +
                        "          legs {" +
                        "          " +
                        "            mode" +
                        "            distance" +
                        "            line {" +
                        "              id" +
                        "              publicCode" +
                        "            }" +
                        "            pointsOnLink {" +
                        "              points" +
                        "              length" +
                        "            }" +
                        "          }" +
                        "    }" +
                        "  }" +
                        "}";

        HashMap<String, Object> result = graphIndex.getGraphQLExecutionResult(query, router, new HashMap<>(), null, 10000, 10000);
        Object tripPatternObj = ((Map) ((Map) result.get("data")).get("trip")).get("tripPatterns");
        assertTrue(tripPatternObj instanceof List);
        List tripPatterns = (List) tripPatternObj;
        assertEquals(3, tripPatterns.size());
    }

    public void testGraphQLIntrospectionQuery() {
        String query = "  query IntrospectionQuery {"
                               + "    __schema {"
                               + "      queryType { name }"
                               + "      mutationType { name }"
                               + "      types {"
                               + "        ...FullType"
                               + "      }"
                               + "      directives {"
                               + "        name"
                               + "        description"
                               + "        args {"
                               + "          ...InputValue"
                               + "        }"
                               + "        onOperation"
                               + "        onFragment"
                               + "        onField"
                               + "      }"
                               + "    }"
                               + "  }"
                               + ""
                               + "  fragment FullType on __Type {"
                               + "    kind"
                               + "    name"
                               + "    description"
                               + "    fields {"
                               + "      name"
                               + "      description"
                               + "      args {"
                               + "        ...InputValue"
                               + "      }"
                               + "      type {"
                               + "        ...TypeRef"
                               + "      }"
                               + "      isDeprecated"
                               + "      deprecationReason"
                               + "    }"
                               + "    inputFields {"
                               + "      ...InputValue"
                               + "    }"
                               + "    interfaces {"
                               + "      ...TypeRef"
                               + "    }"
                               + "    enumValues {"
                               + "      name"
                               + "      description"
                               + "      isDeprecated"
                               + "      deprecationReason"
                               + "    }"
                               + "    possibleTypes {"
                               + "      ...TypeRef"
                               + "    }"
                               + "  }"
                               + ""
                               + "  fragment InputValue on __InputValue {"
                               + "    name"
                               + "    description"
                               + "    type { ...TypeRef }"
                               + "    defaultValue"
                               + "  }"
                               + ""
                               + "  fragment TypeRef on __Type {"
                               + "    kind"
                               + "    name"
                               + "    ofType {"
                               + "      kind"
                               + "      name"
                               + "      ofType {"
                               + "        kind"
                               + "        name"
                               + "        ofType {"
                               + "          kind"
                               + "          name"
                               + "        }"
                               + "      }"
                               + "    }"
                               + "  }";

        ExecutionResult result = new GraphQL(
                                                    transmodelIndexGraphQLSchema.indexSchema, new ExecutorServiceExecutionStrategy(graph.index.threadPool)
        ).execute(query);
        assertTrue(result.getErrors().isEmpty());
    }

    public void testStopWithAdjacentSites() {
        AgencyAndId id = new AgencyAndId("FEED", "V");
        Stop stop = new Stop();
        stop.getAdjacentSites().add("ANOTHER");
        stop.setId(id);

        router.graph.index.stationForId.put(id, stop);

        String query = "{"
                + "  stopPlace(id:\"FEED:V\") {"
                + "    id"
                + "    adjacentSites"
                + "  }"
                + "}";

        HashMap<String, Object> result = graphIndex.getGraphQLExecutionResult(query, router, new HashMap<>(), null, 10000, 10000);

        Object stopObject = ((Map) result.get("data")).get("stopPlace");
        assertNotNull(stopObject);
        Map map = (Map) stopObject;
        List actualAdjacentSites = (List) map.get("adjacentSites");
        assertThat("Number of adjacent sites", actualAdjacentSites.size(), is(1));
        assertThat("Value of first adjacent site", actualAdjacentSites.get(0), is("ANOTHER"));
    }
}
