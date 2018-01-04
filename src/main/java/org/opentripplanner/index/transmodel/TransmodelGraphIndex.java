package org.opentripplanner.index.transmodel;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.opentripplanner.index.IndexGraphQLSchema;
import org.opentripplanner.index.ResourceConstrainedExecutorServiceExecutionStrategy;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransmodelGraphIndex {

    public final GraphQLSchema indexSchema;

    public final ExecutorService threadPool;

    public TransmodelGraphIndex(Router router) {
        threadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("GraphQLExecutor-" + router.id + "-%d")
                        .build()
        );

        indexSchema = new TransmodelIndexGraphQLSchema(router).indexSchema;
    }

    public HashMap<String, Object> getGraphQLExecutionResult(String query, Router router,
                                                                    Map<String, Object> variables, String operationName, int timeout, long maxResolves) {
        GraphQL graphQL = new GraphQL(
                                             indexSchema,
                                             new ResourceConstrainedExecutorServiceExecutionStrategy(threadPool, timeout, TimeUnit.MILLISECONDS, maxResolves)
        );

        if (variables == null) {
            variables = new HashMap<>();
        }

        ExecutionResult executionResult = graphQL.execute(query, operationName, router, variables);
        HashMap<String, Object> content = new HashMap<>();
        if (!executionResult.getErrors().isEmpty()) {
            content.put("errors",
                    executionResult
                            .getErrors()
                            .stream()
                            .map(error -> {
                                if (error instanceof ExceptionWhileDataFetching) {
                                    HashMap<String, Object> response = new HashMap<String, Object>();
                                    response.put("message", error.getMessage());
                                    response.put("locations", error.getLocations());
                                    response.put("errorType", error.getErrorType());
                                    // Convert stack trace to propr format
                                    Stream<StackTraceElement> stack = Arrays.stream(((ExceptionWhileDataFetching) error).getException().getStackTrace());
                                    response.put("stack", stack.map(StackTraceElement::toString).collect(Collectors.toList()));
                                    return response;
                                } else {
                                    return error;
                                }
                            })
                            .collect(Collectors.toList()));
        }
        if (executionResult.getData() != null) {
            content.put("data", executionResult.getData());
        }
        return content;
    }

    public Response getGraphQLResponse(String query, Router router, Map<String, Object> variables, String operationName, int timeout, long maxResolves) {
        Response.ResponseBuilder res = Response.status(Response.Status.OK);
        HashMap<String, Object> content = getGraphQLExecutionResult(query, router, variables,
                operationName, timeout, maxResolves);
        if (content.get("errors") != null) {
            // TODO: Put correct error code, eg. 400 for syntax error
            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return res.entity(content).build();
    }

}
