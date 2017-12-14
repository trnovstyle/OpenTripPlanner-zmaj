package org.opentripplanner.index.transmodel;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutorServiceExecutionStrategy;
import org.opentripplanner.GtfsTest;

import java.util.Map;

public class TransmodelGraphQLTest extends GtfsTest {

    @Override
    public String getFeedName() {
        return "testagency.zip";
    }

    private TransmodelIndexGraphQLSchema transmodelIndexGraphQLSchema;

    @Override
    protected void setUp() {
        super.setUp();
        transmodelIndexGraphQLSchema = new TransmodelIndexGraphQLSchema(graph.index);
    }

    public void testGraphQLSimple() {
        String query =
                "query Organisation{" +
                        "    organisation(id: \"agency\"){" +
                        "        name" +
                        "    }" +
                        "}";

        ExecutionResult result = new GraphQL(transmodelIndexGraphQLSchema.indexSchema, new ExecutorServiceExecutionStrategy(graph.index.threadPool)
        ).execute(query);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        assertEquals("Fake Agency", ((Map) data.get("organisation")).get("name"));
    }

    public void testGraphQLIntrospectionQuery() {
        String query = "  query IntrospectionQuery {\n"
                               + "    __schema {\n"
                               + "      queryType { name }\n"
                               + "      mutationType { name }\n"
                               + "      types {\n"
                               + "        ...FullType\n"
                               + "      }\n"
                               + "      directives {\n"
                               + "        name\n"
                               + "        description\n"
                               + "        args {\n"
                               + "          ...InputValue\n"
                               + "        }\n"
                               + "        onOperation\n"
                               + "        onFragment\n"
                               + "        onField\n"
                               + "      }\n"
                               + "    }\n"
                               + "  }\n"
                               + "\n"
                               + "  fragment FullType on __Type {\n"
                               + "    kind\n"
                               + "    name\n"
                               + "    description\n"
                               + "    fields {\n"
                               + "      name\n"
                               + "      description\n"
                               + "      args {\n"
                               + "        ...InputValue\n"
                               + "      }\n"
                               + "      type {\n"
                               + "        ...TypeRef\n"
                               + "      }\n"
                               + "      isDeprecated\n"
                               + "      deprecationReason\n"
                               + "    }\n"
                               + "    inputFields {\n"
                               + "      ...InputValue\n"
                               + "    }\n"
                               + "    interfaces {\n"
                               + "      ...TypeRef\n"
                               + "    }\n"
                               + "    enumValues {\n"
                               + "      name\n"
                               + "      description\n"
                               + "      isDeprecated\n"
                               + "      deprecationReason\n"
                               + "    }\n"
                               + "    possibleTypes {\n"
                               + "      ...TypeRef\n"
                               + "    }\n"
                               + "  }\n"
                               + "\n"
                               + "  fragment InputValue on __InputValue {\n"
                               + "    name\n"
                               + "    description\n"
                               + "    type { ...TypeRef }\n"
                               + "    defaultValue\n"
                               + "  }\n"
                               + "\n"
                               + "  fragment TypeRef on __Type {\n"
                               + "    kind\n"
                               + "    name\n"
                               + "    ofType {\n"
                               + "      kind\n"
                               + "      name\n"
                               + "      ofType {\n"
                               + "        kind\n"
                               + "        name\n"
                               + "        ofType {\n"
                               + "          kind\n"
                               + "          name\n"
                               + "        }\n"
                               + "      }\n"
                               + "    }\n"
                               + "  }";

        ExecutionResult result = new GraphQL(
                                                    transmodelIndexGraphQLSchema.indexSchema, new ExecutorServiceExecutionStrategy(graph.index.threadPool)
        ).execute(query);
        assertTrue(result.getErrors().isEmpty());
    }

}
