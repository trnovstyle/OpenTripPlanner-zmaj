package org.opentripplanner.index.transmodel;

import com.jayway.jsonpath.JsonPath;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutorServiceExecutionStrategy;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.routing.core.RoutingRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransmodelGraphQLTest extends GtfsTest {

    @Override
    public String getFeedName() {
        return "testagency.zip";
    }

    private GraphQL graphQL;
    private TransmodelGraphIndex graphIndex;

    @Override
    protected void setUp() {
        super.setUp();
        router.defaultRoutingRequest = new RoutingRequest();
        graphIndex = TransmodelGraphIndexFactory.getTransmodelGraphIndexForRouter(router);
        TransmodelIndexGraphQLSchema transmodelIndexGraphQLSchema = new TransmodelIndexGraphQLSchema(router);
        graphQL = GraphQL.newGraphQL(
                transmodelIndexGraphQLSchema.indexSchema).queryExecutionStrategy(new ExecutorServiceExecutionStrategy(graph.index.threadPool)).build();
    }

    public void testGraphQLAuthority() {
        String query =
                "query Authority {" +
                        "    authority(id: \"agency\"){" +
                        "        name" +
                        "  lines {id}" +
                        "    }" +
                        "}";

        ExecutionResult result = graphQL.execute(query);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        assertEquals("Fake Agency", JsonPath.read(data, "authority.name"));

        assertFalse("Expected at least one line", ((Collection) JsonPath.read(data, "authority.lines")).isEmpty());
    }

    public void testGetQuay() {
        String quayId = "FEED:V";
        String query =
                "query Quay {" +
                        "    quay(id: \"" + quayId + "\") {" +
                        "        id" +
                        "        latitude" +
                        "        longitude" +
                        " estimatedCalls(startTime: \"2018-12-17T11:05:00+0100\") { " +
                        "aimedDepartureTime" +
                        "} } " +
                        "}";

        ExecutionResult result = graphQL.execute(query);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        assertEquals(quayId, JsonPath.read(data, "quay.id"));
        List estimatedCalls = JsonPath.read(data, "quay.estimatedCalls");
        assertFalse("Expected at least one estimated call for quay", estimatedCalls.isEmpty());
    }

    public void testGetQuaysByBBox() {
        String query =
                " { quaysByBbox(minimumLatitude:39, minimumLongitude:-75, maximumLatitude:42, maximumLongitude:-70 ) {id}} ";

        ExecutionResult result = graphQL.execute(query);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        List quaysByBbox = JsonPath.read(data, "quaysByBbox");
        assertFalse("Expected to find at least one quay by bBox", quaysByBbox.isEmpty());
    }


    public void testGetLineById() {
        String lineId = "FEED:1";
        String query =
                " { line(id:\""+lineId+"\" ) {id serviceJourneys {activeDates}}} ";

        ExecutionResult result = graphQL.execute(query);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        assertEquals(lineId, JsonPath.read(data, "line.id"));
        List serviceJourney = JsonPath.read(data, "line.serviceJourneys");
        assertFalse("Expected at least one service journey for line", serviceJourney.isEmpty());
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

        TransmodelApiContext context = new TransmodelApiContext(router, "");
        HashMap<String, Object> result = graphIndex.getGraphQLExecutionResult(query, context, new HashMap<>(), null, 10000, 10000);
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

        ExecutionResult result = graphQL
                                         .execute(query);
        assertTrue(result.getErrors().isEmpty());
    }


}
