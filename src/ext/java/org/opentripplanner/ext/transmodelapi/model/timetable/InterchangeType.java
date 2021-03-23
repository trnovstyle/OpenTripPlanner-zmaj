package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.model.transfer.Transfer;

public class InterchangeType {

  public static GraphQLObjectType create(
      GraphQLOutputType lineType, GraphQLOutputType serviceJourneyType
  ) {
    return GraphQLObjectType.newObject()
        .name("Interchange")
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("staySeated")
                .description("Time that the trip departs. NOT IMPLEMENTED")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> false)
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("guaranteed")
                .description("Time that the trip departs. NOT IMPLEMENTED")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> false)
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("FromLine")
                .deprecate("This is the same as using the `FromServiceJourney { line }` field.")
                .type(lineType)
                .dataFetcher(environment -> ((Transfer) environment.getSource()).getFrom().getTrip().getRoute())
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("ToLine")
                .deprecate("This is the same as using the `ToServiceJourney { line }` field.")
                .type(lineType)
                .dataFetcher(environment -> ((Transfer) environment.getSource()).getTo().getTrip().getRoute())
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("FromServiceJourney")
                .type(serviceJourneyType)
                .dataFetcher(environment -> ((Transfer) environment.getSource()).getFrom().getTrip())
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("ToServiceJourney")
                .type(serviceJourneyType)
                .dataFetcher(environment -> ((Transfer) environment.getSource()).getTo().getTrip())
                .build())
        .build();
  }
}
