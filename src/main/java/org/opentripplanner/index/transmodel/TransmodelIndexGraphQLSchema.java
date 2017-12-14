package org.opentripplanner.index.transmodel;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import graphql.Scalars;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import org.apache.commons.lang3.StringUtils;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.VertexType;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.GraphQlPlanner;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.index.transmodel.mapping.TransportSubmodeMapper;
import org.opentripplanner.index.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.car_park.CarParkService;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.opentripplanner.util.TranslatedString;
import org.opentripplanner.util.model.EncodedPolylineBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

/**
 * Schema definition for the Transmodel GraphQL API.
 *
 * Currently a simplified version of the IndexGraphQLSchema, with gtfs terminology replaced with corresponding terms from Transmodel.
 *
 */
public class TransmodelIndexGraphQLSchema {
    private static final Logger LOG = LoggerFactory.getLogger(TransmodelIndexGraphQLSchema.class);

    private static final String LIST_VALUE_SEPARATOR = ",";

    private static final String GTFS_LIBRARY_ID_SEPARATOR = ":";

    private static GraphQLEnumType wheelchairBoardingEnum = GraphQLEnumType.newEnum()
                                                                    .name("WheelchairBoarding")
                                                                    .value("noInformation", 0, "There is no accessibility information for the stop.")
                                                                    .value("possible", 1, "At least some vehicles at this stop can be boarded by a rider in a wheelchair.")
                                                                    .value("notPossible", 2, "Wheelchair boarding is not possible at this stop.")
                                                                    .build();

    private static GraphQLEnumType bikesAllowedEnum = GraphQLEnumType.newEnum()
                                                              .name("BikesAllowed")
                                                              .value("noInformation", 0, "There is no bike information for the trip.")
                                                              .value("allowed", 1, "The vehicle being used on this particular trip can accommodate at least one bicycle.")
                                                              .value("notAllowed", 2, "No bicycles are allowed on this trip.")
                                                              .build();

    private static GraphQLEnumType realtimeStateEnum = GraphQLEnumType.newEnum()
                                                               .name("RealtimeState")
                                                               .value("scheduled", RealTimeState.SCHEDULED, "The service journey information comes from the regular time table, i.e. no real-time update has been applied.")
                                                               .value("updated", RealTimeState.UPDATED, "The service journey information has been updated, but the journey pattern stayed the same as the journey pattern of the scheduled service journey.")
                                                               .value("canceled", RealTimeState.CANCELED, "The service journey has been canceled by a real-time update.")
                                                               .value("Added", RealTimeState.ADDED, "The service journey has been added using a real-time update, i.e. the service journey was not present in the regular time table.")
                                                               .value("modified", RealTimeState.MODIFIED, "The service journey information has been updated and resulted in a different journey pattern compared to the journey pattern of the scheduled service journey.")
                                                               .build();

    private static GraphQLEnumType vertexTypeEnum = GraphQLEnumType.newEnum()
                                                            .name("VertexType")
                                                            .value("normal", VertexType.NORMAL)
                                                            .value("transit", VertexType.TRANSIT)
                                                            .value("bikePark", VertexType.BIKEPARK)
                                                            .value("bikeShare", VertexType.BIKESHARE)
                                                            .value("parkAndRide", VertexType.PARKANDRIDE)
                                                            .build();

    private static GraphQLEnumType modeEnum = GraphQLEnumType.newEnum()
                                                      .name("Mode")
                                                      .value("air", TraverseMode.AIRPLANE)
                                                      .value("bicycle", TraverseMode.BICYCLE)
                                                      .value("bus", TraverseMode.BUS)
                                                      .value("cableway", TraverseMode.CABLE_CAR)
                                                      .value("car", TraverseMode.CAR)
                                                      .value("water", TraverseMode.FERRY)
                                                      .value("funicular", TraverseMode.FUNICULAR)
                                                      .value("lift", TraverseMode.GONDOLA)
                                                      .value("linkSwitch", TraverseMode.LEG_SWITCH)
                                                      .value("rail", TraverseMode.RAIL)
                                                      .value("metro", TraverseMode.SUBWAY)
                                                      .value("tram", TraverseMode.TRAM)
                                                      .value("transit", TraverseMode.TRANSIT, "Any for of public transportation")
                                                      .value("foot", TraverseMode.WALK)
                                                      .build();


    private static Map<String, TraverseMode> traverseModeMap = modeEnum.getValues().stream().filter(valueDef -> valueDef.getValue() instanceof TraverseMode).collect(Collectors.toMap(GraphQLEnumValueDefinition::getName, valueDef -> (TraverseMode) valueDef.getValue()));

    private static GraphQLEnumType transportModeEnum = GraphQLEnumType.newEnum()
                                                               .name("TransportMode")
                                                               .value("air", TraverseMode.AIRPLANE)
                                                               .value("bus", TraverseMode.BUS)
                                                               .value("cableway", TraverseMode.CABLE_CAR)
                                                               .value("water", TraverseMode.FERRY)
                                                               .value("funicular", TraverseMode.FUNICULAR)
                                                               .value("lift", TraverseMode.GONDOLA)
                                                               .value("rail", TraverseMode.RAIL)
                                                               .value("metro", TraverseMode.SUBWAY)
                                                               .value("tram", TraverseMode.TRAM)
                                                               .value("unknown", "unknown")
                                                               .build();

    private static GraphQLEnumType transportSubmode;

    static {
        GraphQLEnumType.Builder transportSubmodeEnumBuilder = GraphQLEnumType.newEnum().name("TransmodelTransportSubmode");
        Arrays.stream(TransmodelTransportSubmode.values()).forEach(type -> transportSubmodeEnumBuilder.value(type.getValue(), type));
        transportSubmode = transportSubmodeEnumBuilder.build();
    }


    private static GraphQLEnumType filterPlaceTypeEnum = GraphQLEnumType.newEnum()
                                                                 .name("FilterPlaceType")
                                                                 .value("quay", GraphIndex.PlaceType.STOP, "quay")
                                                                 .value("departure", GraphIndex.PlaceType.DEPARTURE_ROW, "Departure")
                                                                 .value("bicycleRent", GraphIndex.PlaceType.BICYCLE_RENT, "Bicycle rent stations")
                                                                 .value("bikePark", GraphIndex.PlaceType.BIKE_PARK, "Bike parks")
                                                                 .value("carPark", GraphIndex.PlaceType.CAR_PARK, "Car parks")
                                                                 .build();

    private static GraphQLEnumType optimisationMethodEnum = GraphQLEnumType.newEnum()
                                                                    .name("OptimisationMethod")
                                                                    .value("quick", OptimizeType.QUICK)
                                                                    .value("safe", OptimizeType.SAFE)
                                                                    .value("flat", OptimizeType.FLAT)
                                                                    .value("greenways", OptimizeType.GREENWAYS)
                                                                    .value("triangle", OptimizeType.TRIANGLE)
                                                                    .value("transfers", OptimizeType.TRANSFERS)
                                                                    .build();

    private static GraphQLEnumType directionTypeEnum = GraphQLEnumType.newEnum()
                                                               .name("DirectionType")
                                                               .value("outbound", 0)
                                                               .value("inbound", 1)
                                                               .value("clockwise", 2)
                                                               .value("anticlockwise", 3)
                                                               .build();

    private GraphQLOutputType noticeType = new GraphQLTypeReference("Notice");

    private GraphQLOutputType organisationType = new GraphQLTypeReference("Organisation");

    private GraphQLOutputType alertType = new GraphQLTypeReference("Alert");

    private GraphQLOutputType bikeRentalStationType = new GraphQLTypeReference("BikeRentalStation");

    private GraphQLOutputType bikeParkType = new GraphQLTypeReference("BikePark");

    private GraphQLOutputType carParkType = new GraphQLTypeReference("CarPark");

    private GraphQLOutputType coordinateType = new GraphQLTypeReference("Coordinates");

    private GraphQLOutputType journeyPatternType = new GraphQLTypeReference("JourneyPattern");

    private GraphQLOutputType lineType = new GraphQLTypeReference("Line");

    private GraphQLOutputType timetabledPassingTimeType = new GraphQLTypeReference("TimetabledPassingTime");

    private GraphQLOutputType estimatedCallType = new GraphQLTypeReference("EstimatedCall");

    private GraphQLOutputType stopPlaceType = new GraphQLTypeReference("StopPlace");

    private GraphQLOutputType quayType = new GraphQLTypeReference("Quay");

    private GraphQLOutputType serviceJourneyType = new GraphQLTypeReference("ServiceJourney");

    private GraphQLOutputType quayAtDistance = new GraphQLTypeReference("QuayAtDistance");

    private GraphQLOutputType translatedStringType = new GraphQLTypeReference("TranslatedString");

    private GraphQLOutputType departureType = new GraphQLTypeReference("Departure");

    private GraphQLOutputType placeAtDistanceType = new GraphQLTypeReference("PlaceAtDistance");

    private GraphQLObjectType queryType;

    private GraphQLOutputType tripType = new GraphQLTypeReference("Trip");

    private TransportSubmodeMapper transportSubmodeMapper = new TransportSubmodeMapper();

    private String fixedAgencyId;

    public GraphQLSchema indexSchema;

    private Relay relay = new Relay();

    private GraphQLInterfaceType nodeInterface = relay.nodeInterface(o -> {
                if (o instanceof GraphIndex.StopAndDistance) {
                    return (GraphQLObjectType) quayAtDistance;
                }
                if (o instanceof Stop) {
                    return (GraphQLObjectType) stopPlaceType;
                }
                if (o instanceof Trip) {
                    return (GraphQLObjectType) serviceJourneyType;
                }
                if (o instanceof Route) {
                    return (GraphQLObjectType) lineType;
                }
                if (o instanceof TripPattern) {
                    return (GraphQLObjectType) journeyPatternType;
                }
                if (o instanceof Agency) {
                    return (GraphQLObjectType) organisationType;
                }
                if (o instanceof AlertPatch) {
                    return (GraphQLObjectType) alertType;
                }
                if (o instanceof BikeRentalStation) {
                    return (GraphQLObjectType) bikeRentalStationType;
                }
                if (o instanceof BikePark) {
                    return (GraphQLObjectType) bikeParkType;
                }
                if (o instanceof CarPark) {
                    return (GraphQLObjectType) carParkType;
                }
                if (o instanceof GraphIndex.DepartureRow) {
                    return (GraphQLObjectType) departureType;
                }
                if (o instanceof GraphIndex.PlaceAndDistance) {
                    return (GraphQLObjectType) placeAtDistanceType;
                }
                return null;
            }
    );

    private GraphQLInterfaceType placeInterface = GraphQLInterfaceType.newInterface()
                                                          .name("PlaceInterface")
                                                          .description("Interface for places, i.e. quays, stop places, parks")
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("id")
                                                                         .type(new GraphQLNonNull(Scalars.GraphQLID))
                                                                         .build())
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("lat")
                                                                         .type(Scalars.GraphQLFloat)
                                                                         .build())
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("lon")
                                                                         .type(Scalars.GraphQLFloat)
                                                                         .build())
                                                          .typeResolver(o -> {
                                                                      if (o instanceof Stop) {
                                                                          return (GraphQLObjectType) stopPlaceType;
                                                                      }
                                                                      if (o instanceof GraphIndex.DepartureRow) {
                                                                          return (GraphQLObjectType) departureType;
                                                                      }
                                                                      if (o instanceof BikeRentalStation) {
                                                                          return (GraphQLObjectType) bikeRentalStationType;
                                                                      }
                                                                      if (o instanceof BikePark) {
                                                                          return (GraphQLObjectType) bikeParkType;
                                                                      }
                                                                      if (o instanceof CarPark) {
                                                                          return (GraphQLObjectType) carParkType;
                                                                      }
                                                                      return null;
                                                                  }
                                                          ).build();

    private Agency getAgency(GraphIndex index, String agencyId) {
        //xxx what if there are duplicate agency ids?
        //now we return the first
        for (Map<String, Agency> feedAgencies : index.agenciesForFeedId.values()) {
            if (feedAgencies.get(agencyId) != null) {
                return feedAgencies.get(agencyId);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public TransmodelIndexGraphQLSchema(GraphIndex index) {
        createPlanType(index);
        String fixedAgencyIdPropValue = System.getProperty("transmodel.graphql.api.agency.id");
        if (!StringUtils.isEmpty(fixedAgencyIdPropValue)) {
            fixedAgencyId = fixedAgencyIdPropValue;
            LOG.info("Starting Transmodel GraphQL Schema with fixed AgencyID:'" + fixedAgencyId +
                             "'. All AgencyAndIds in API will be assumed to belong to this agency.");
        }

        GraphQLInputObjectType coordinateInputType = GraphQLInputObjectType.newInputObject()
                                                             .name("InputCoordinates")
                                                             .field(GraphQLInputObjectField.newInputObjectField()
                                                                            .name("lat")
                                                                            .description("The latitude of the place.")
                                                                            .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                                                                            .build())
                                                             .field(GraphQLInputObjectField.newInputObjectField()
                                                                            .name("lon")
                                                                            .description("The longitude of the place.")
                                                                            .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                                                                            .build())
                                                             .field(GraphQLInputObjectField.newInputObjectField()
                                                                            .name("address")
                                                                            .description("The name of the place.")
                                                                            .type(Scalars.GraphQLString)
                                                                            .build())
                                                             .build();

        GraphQLInputObjectType preferredInputType = GraphQLInputObjectType.newInputObject()
                                                            .name("InputPreferred")
                                                            .field(GraphQLInputObjectField.newInputObjectField()
                                                                           .name("lines")
                                                                           .description("Set of preferred lines by user.")
                                                                           .type(new GraphQLList(Scalars.GraphQLString))
                                                                           .build())
                                                            .field(GraphQLInputObjectField.newInputObjectField()
                                                                           .name("organisations")
                                                                           .description("Set of preferred organisations by user.")
                                                                           .type(new GraphQLList(Scalars.GraphQLString))
                                                                           .build())
                                                            .field(GraphQLInputObjectField.newInputObjectField()
                                                                           .name("otherThanPreferredLinesPenalty")
                                                                           .description("Penalty added for using every line that is not preferred if user set any line as preferred. We return number of seconds that we are willing to wait for preferred line.")
                                                                           .type(Scalars.GraphQLInt)
                                                                           .build())
                                                            .build();

        GraphQLInputObjectType unpreferredInputType = GraphQLInputObjectType.newInputObject()
                                                              .name("InputUnpreferred")
                                                              .field(GraphQLInputObjectField.newInputObjectField()
                                                                             .name("lines")
                                                                             .description("Set of unpreferred lines for given user.")
                                                                             .type(new GraphQLList(Scalars.GraphQLString))
                                                                             .build())
                                                              .field(GraphQLInputObjectField.newInputObjectField()
                                                                             .name("organisations")
                                                                             .description("Set of unpreferred organisations for given user.")
                                                                             .type(new GraphQLList(Scalars.GraphQLString))
                                                                             .build())
                                                              .build();

        GraphQLInputObjectType bannedInputType = GraphQLInputObjectType.newInputObject()
                                                         .name("InputBanned")
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("lines")
                                                                        .description("Do not use certain named lines")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("organisations")
                                                                        .description("Do not use certain named organisations")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         // TODO trip ids (serviceJourneys) are expected on format AgencyId:trip-id[:stop ordinal:stop ordinal..] and thus will not work with serviceJourney ids containing ':'.
                                                         // Need to subclass GraphQLPlanner if this field is to be supported
//                                                         .field(GraphQLInputObjectField.newInputObjectField()
//                                                                        .name("serviceJourneys")
//                                                                        .description("Do not use certain named serviceJourneys")
//                                                                        .type(new GraphQLList(Scalars.GraphQLString))
//                                                                        .build())
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("quay")
                                                                        .description("Do not use certain quays. See for more information the bannedStops property in the RoutingResource class.")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("quaysHard")
                                                                        .description("Do not use certain quays. See for more information the bannedStopsHard property in the RoutingResource class.")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         .build();


        GraphQLFieldDefinition tripFieldType = GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("trip")
                                                       .description("Execute travel search for a trip between two locations. Returns trip patterns describing suggested alternatives for the trip.")
                                                       .type(tripType)
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("date")
                                                                         .type(Scalars.GraphQLString)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("time")
                                                                         .type(Scalars.GraphQLString)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("from")
                                                                         .description("The start location")
                                                                         .type(coordinateInputType)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("to")
                                                                         .description("The end location")
                                                                         .type(coordinateInputType)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("fromPlace")
                                                                         .type(Scalars.GraphQLString)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("toPlace")
                                                                         .type(Scalars.GraphQLString)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("wheelchair")
                                                                         .description("Whether the trip must be wheelchair accessible.")
                                                                         .type(Scalars.GraphQLBoolean)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("numTripPatterns")
                                                                         .description("The maximum number of trip patterns to return.")
                                                                         .defaultValue(5)
                                                                         .type(Scalars.GraphQLInt)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("walkSpeed")
                                                                         .description("The maximum walk speed along streets, in meters per second")
                                                                         .type(Scalars.GraphQLFloat)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("bikeSpeed")
                                                                         .description("The maximum bike speed along streets, in meters per second")
                                                                         .type(Scalars.GraphQLFloat)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("optimisationMethod")
                                                                         .description("The set of characteristics that the user wants to optimise for -- defaults to 'quick', or optimise for transit time.")
                                                                         .type(optimisationMethodEnum)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("arriveBy")
                                                                         .description("Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.")
                                                                         .type(Scalars.GraphQLBoolean)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("viaPlaces")
                                                                         .description("An ordered list of intermediate locations to be visited.")
                                                                         .type(new GraphQLList(coordinateInputType))
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("preferred")
                                                                         .description("Parameters for indicating operators or lines that preferably should be used in trip patters.")
                                                                         .type(preferredInputType)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("unpreferred")
                                                                         .description("Parameters for indicating operators or lines that preferably should not be used in trip patters")
                                                                         .type(unpreferredInputType)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("banned")
                                                                         .description("Banned")
                                                                         .description("Parameters for indicating operators, lines or quays not be used in the trip patterns")
                                                                         .type(bannedInputType)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("transferPenalty")
                                                                         .description("An extra penalty added on transfers (i.e. all boardings except the first one). The transferPenalty is used when a user requests even less transfers. In the latter case, we don't actually optimise for fewest transfers, as this can lead to absurd results. Consider a trip in New York from Grand Army Plaza (the one in Brooklyn) to Kalustyan's at noon. The true lowest transfers route is to wait until midnight, when the 4 train runs local the whole way. The actual fastest route is the 2/3 to the 4/5 at Nevins to the 6 at Union Square, which takes half an hour. Even someone optimise for fewest transfers doesn't want to wait until midnight. Maybe they would be willing to walk to 7th Ave and take the Q to Union Square, then transfer to the 6. If this takes less than optimise_transfer_penalty seconds, then that's what we'll return.")
                                                                         .type(Scalars.GraphQLInt)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("modes")
                                                                         .description("The set of modes that a user is willing to use. Defaults to foot | transit.")
                                                                         .type(new GraphQLList(Scalars.GraphQLString))
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("allowBikeRental")
                                                                         .description("Is bike rental allowed?")
                                                                         .type(Scalars.GraphQLBoolean)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("boardSlack")
                                                                         .description("Invariant: boardSlack + alightSlack <= transferSlack.")
                                                                         .type(Scalars.GraphQLInt)
                                                                         .build()) // TODO remove?
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("alightSlack")
                                                                         .description("Invariant: boardSlack + alightSlack <= transferSlack.")
                                                                         .type(Scalars.GraphQLInt)
                                                                         .build()) // TODO remove?
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("minTransferTime")
                                                                         .description("A global minimum transfer time (in seconds) that specifies the minimum amount of time that must pass between exiting one transit vehicle and boarding another. This time is in addition to time it might take to walk between transit stops. This time should also be overridden by specific transfer timing information in transfers.txt")
                                                                         .type(Scalars.GraphQLInt)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("nonpreferredTransferPenalty")
                                                                         .description("Penalty for using a non-preferred transfer")
                                                                         .type(Scalars.GraphQLInt)
                                                                         .build()) // TODO gard sjekker
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("maxTransfers")
                                                                         .description("Maximum number of transfers")
                                                                         .type(Scalars.GraphQLInt)
                                                                         .build())
                                                       .argument(GraphQLArgument.newArgument()
                                                                         .name("ignoreRealtimeUpdates")
                                                                         .description("When true, realtime updates are ignored during this search.")
                                                                         .type(Scalars.GraphQLBoolean)
                                                                         .build())
                                                       .dataFetcher(environment -> {

                                                           environment.getArguments().put("modes", mapListOfModes(environment.getArgument("modes")));

                                                           // TODO using AgencyAndId separator instead of GtfsLibrary because GraphqlPlanner expects this. Should be cleaned up.
                                                           environment.getArguments().put("startTransitStopId", prepareAgencyAndId(environment.getArgument("startTransitQuayId")));
                                                           environment.getArguments().put("startTransitTripId", prepareAgencyAndId(environment.getArgument("startTransitServiceJourneyId")));

                                                           Map<String, Object> unpreferred = environment.getArgument("unpreferred");
                                                           if (unpreferred != null) {
                                                               // Using double underscore as separator as expected format is agency_routeName_routeId, and we are only supplying routeId
                                                               unpreferred.put("routes", prepareListOfAgencyAndId((List<String>) unpreferred.get("lines"), "__"));
                                                               unpreferred.put("agencies", unpreferred.get("organisations"));
                                                           }
                                                           Map<String, Object> preferred = environment.getArgument("preferred");
                                                           if (preferred != null) {
                                                               // Using double underscore as separator as expected format is agency_routeName_routeId, and we are only supplying routeId
                                                               preferred.put("routes", prepareListOfAgencyAndId((List<String>) preferred.get("lines"), "__"));
                                                               preferred.put("agencies", preferred.get("organisations"));
                                                               preferred.put("otherThanPreferredRoutesPenalty", preferred.get("otherThanPreferredLinesPenalty"));
                                                           }

                                                           Map<String, Object> banned = environment.getArgument("banned");
                                                           if (banned != null) {
                                                               // Using double underscore as separator as expected format is agency_routeName_routeId, and we are only supplying routeId
                                                               banned.put("routes", prepareListOfAgencyAndId((List<String>) banned.get("lines"), "__"));
                                                               banned.put("agencies", banned.get("organisations"));
                                                               banned.put("trips", prepareListOfAgencyAndId((List<String>) banned.get("serviceJourneys")));
                                                               banned.put("stops", prepareListOfAgencyAndId((List<String>) banned.get("quay")));
                                                               banned.put("stopsHard", prepareListOfAgencyAndId((List<String>) banned.get("quaysHard")));
                                                           }

                                                           environment.getArguments().put("fromPlace", preparePlaceRef(environment.getArgument("fromPlace")));
                                                           environment.getArguments().put("toPlace", preparePlaceRef(environment.getArgument("toPlace")));
                                                           return new GraphQlPlanner(index).plan(environment);
                                                       })
                                                       .build();

        noticeType = GraphQLObjectType.newObject()
                             .name("Notice")
                             .field(GraphQLFieldDefinition.newFieldDefinition()
                                            .name("id")
                                            .type(Scalars.GraphQLString)
                                            .dataFetcher(
                                                    environment -> ((Notice) environment.getSource()).getId())
                                            .build())
                             .field(GraphQLFieldDefinition.newFieldDefinition()
                                            .name("text")
                                            .type(Scalars.GraphQLString)
                                            .dataFetcher(
                                                    environment -> ((Notice) environment.getSource()).getText())
                                            .build())
                             .field(GraphQLFieldDefinition.newFieldDefinition()
                                            .name("publicCode")
                                            .type(Scalars.GraphQLString)
                                            .dataFetcher(
                                                    environment -> ((Notice) environment.getSource()).getPublicCode())
                                            .build())
                             .build();

        translatedStringType = GraphQLObjectType.newObject()
                                       .name("TranslatedString")
                                       .description("Text with language")
                                       .field(GraphQLFieldDefinition.newFieldDefinition()
                                                      .name("text")
                                                      .type(Scalars.GraphQLString)
                                                      .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getValue())
                                                      .build())
                                       .field(GraphQLFieldDefinition.newFieldDefinition()
                                                      .name("language")
                                                      .type(Scalars.GraphQLString)
                                                      .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getKey())
                                                      .build())
                                       .build();

        alertType = GraphQLObjectType.newObject()
                            .name("Alert")
                            .withInterface(nodeInterface)
                            .description("Simple alert")
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("id")
                                           .type(new GraphQLNonNull(Scalars.GraphQLID))
                                           .dataFetcher(environment -> relay.toGlobalId(
                                                   alertType.getName(), ((AlertPatch) environment.getSource()).getId()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("organisation")
                                           .type(organisationType)
                                           .dataFetcher(environment -> getAgency(index, ((AlertPatch) environment.getSource()).getAgency()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("line")
                                           .type(lineType)
                                           .dataFetcher(environment -> index.routeForId.get(((AlertPatch) environment.getSource()).getRoute()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("serviceJourney")
                                           .type(serviceJourneyType)
                                           .dataFetcher(environment -> index.tripForId.get(((AlertPatch) environment.getSource()).getTrip()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("quay")
                                           .type(quayType)
                                           .dataFetcher(environment -> index.stopForId.get(((AlertPatch) environment.getSource()).getStop()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("journeyPatterns")
                                           .description("Get all journey patterns for this alert")
                                           .type(new GraphQLList(journeyPatternType))
                                           .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getTripPatterns())
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("alertHeaderText")
                                           .type(Scalars.GraphQLString)
                                           .description("Header of alert if it exists")
                                           .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertHeaderText)
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("alertHeaderTextTranslations")
                                           .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(translatedStringType))))
                                           .description("Headers of alert in all different translations available notnull")
                                           .dataFetcher(environment -> {
                                               AlertPatch alertPatch = environment.getSource();
                                               Alert alert = alertPatch.getAlert();
                                               if (alert.alertHeaderText instanceof TranslatedString) {
                                                   return ((TranslatedString) alert.alertHeaderText).getTranslations();
                                               } else {
                                                   return emptyList();
                                               }
                                           })
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("alertDescriptionText")
                                           .type(new GraphQLNonNull(Scalars.GraphQLString))
                                           .description("Long description of alert notnull")
                                           .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertDescriptionText)
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("alertDescriptionTextTranslations")
                                           .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(translatedStringType))))
                                           .description("Long descriptions of alert in all different translations available notnull")
                                           .dataFetcher(environment -> {
                                               AlertPatch alertPatch = environment.getSource();
                                               Alert alert = alertPatch.getAlert();
                                               if (alert.alertDescriptionText instanceof TranslatedString) {
                                                   return ((TranslatedString) alert.alertDescriptionText).getTranslations();
                                               } else {
                                                   return emptyList();
                                               }
                                           })
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("alertUrl")
                                           .type(Scalars.GraphQLString)
                                           .description("Url with more information")
                                           .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertUrl)
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("effectiveStartDate")
                                           .type(Scalars.GraphQLLong)
                                           .description("When this alert comes into effect")
                                           .dataFetcher(environment -> {
                                               Alert alert = ((AlertPatch) environment.getSource()).getAlert();
                                               return alert.effectiveStartDate != null ? alert.effectiveStartDate.getTime() / 1000 : null;
                                           })
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("effectiveEndDate")
                                           .type(Scalars.GraphQLLong)
                                           .description("When this alert is not in effect anymore")
                                           .dataFetcher(environment -> {
                                               Alert alert = ((AlertPatch) environment.getSource()).getAlert();
                                               return alert.effectiveEndDate != null ? alert.effectiveEndDate.getTime() / 1000 : null;
                                           })
                                           .build())
                            .build();


        quayAtDistance = GraphQLObjectType.newObject()
                                 .name("QuayAtDistance")
                                 .withInterface(nodeInterface)
                                 .field(GraphQLFieldDefinition.newFieldDefinition()
                                                .name("id")
                                                .type(new GraphQLNonNull(Scalars.GraphQLID))
                                                .dataFetcher(environment -> relay.toGlobalId(quayAtDistance.getName(),
                                                        Integer.toString(((GraphIndex.StopAndDistance) environment.getSource()).distance) + ";" +
                                                                toIdString(((GraphIndex.StopAndDistance) environment.getSource()).stop.getId())))
                                                .build())
                                 .field(GraphQLFieldDefinition.newFieldDefinition()
                                                .name("quay")
                                                .type(quayType)
                                                .dataFetcher(environment -> ((GraphIndex.StopAndDistance) environment.getSource()).stop)
                                                .build())
                                 .field(GraphQLFieldDefinition.newFieldDefinition()
                                                .name("distance")
                                                .type(Scalars.GraphQLInt)
                                                .dataFetcher(environment -> ((GraphIndex.StopAndDistance) environment.getSource()).distance)
                                                .build())
                                 .build();

        departureType = GraphQLObjectType.newObject()
                                .name("Departure")
                                .withInterface(nodeInterface)
                                .withInterface(placeInterface)
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("id")
                                               .type(new GraphQLNonNull(Scalars.GraphQLID))
                                               .dataFetcher(environment -> relay.toGlobalId(departureType.getName(), ((GraphIndex.DepartureRow) environment.getSource()).id))
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("quay")
                                               .type(quayType)
                                               .dataFetcher(environment -> ((GraphIndex.DepartureRow) environment.getSource()).stop)
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("lat")
                                               .type(Scalars.GraphQLFloat)
                                               .dataFetcher(environment -> ((GraphIndex.DepartureRow) environment.getSource()).stop.getLat())
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("lon")
                                               .type(Scalars.GraphQLFloat)
                                               .dataFetcher(environment -> ((GraphIndex.DepartureRow) environment.getSource()).stop.getLon())
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("journeyPattern")
                                               .type(journeyPatternType)
                                               .dataFetcher(environment -> ((GraphIndex.DepartureRow) environment.getSource()).pattern)
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("estimatedCalls")
                                               .type(new GraphQLList(estimatedCallType))
                                               .argument(GraphQLArgument.newArgument()
                                                                 .name("startTime")
                                                                 .description("What is the start time for the times. Default is to use current time. (0)")
                                                                 .type(Scalars.GraphQLLong)
                                                                 .defaultValue(0L) // Default value is current time
                                                                 .build())
                                               .argument(GraphQLArgument.newArgument()
                                                                 .name("timeRange")
                                                                 .description("How many seconds ahead to search for departures. Default is one day.")
                                                                 .type(Scalars.GraphQLInt)
                                                                 .defaultValue(24 * 60 * 60)
                                                                 .build())

                                               .argument(GraphQLArgument.newArgument()
                                                                 .name("omitNonBoarding")
                                                                 .type(Scalars.GraphQLBoolean)
                                                                 .defaultValue(false)
                                                                 .build())
                                               .dataFetcher(environment -> {
                                                   GraphIndex.DepartureRow departureRow = environment.getSource();
                                                   long startTime = environment.getArgument("startTime");
                                                   int timeRange = environment.getArgument("timeRange");
                                                   int maxDepartures = environment.getArgument("numberOfDepartures");
                                                   boolean omitNonBoarding = environment.getArgument("omitNonBoarding");
                                                   return departureRow.getStoptimes(index, startTime, timeRange, maxDepartures, omitNonBoarding);
                                               })
                                               .build())
                                .build();


        placeAtDistanceType = GraphQLObjectType.newObject()
                                      .name("placeAtDistance")
                                      .withInterface(nodeInterface)
                                      .field(GraphQLFieldDefinition.newFieldDefinition()
                                                     .name("id")
                                                     .type(new GraphQLNonNull(Scalars.GraphQLID))
                                                     .dataFetcher(environment -> {
                                                         Object place = ((GraphIndex.PlaceAndDistance) environment.getSource()).place;
                                                         return relay.toGlobalId(placeAtDistanceType.getName(),
                                                                 Integer.toString(((GraphIndex.PlaceAndDistance) environment.getSource()).distance) + ";" +
                                                                         placeInterface.getTypeResolver()
                                                                                 .getType(place)
                                                                                 .getFieldDefinition("id")
                                                                                 .getDataFetcher()
                                                                                 .get(new DataFetchingEnvironmentImpl(place, null, null,
                                                                                                                             null, null, placeAtDistanceType, null))

                                                         );
                                                     })
                                                     .build())
                                      .field(GraphQLFieldDefinition.newFieldDefinition()
                                                     .name("place")
                                                     .type(placeInterface)
                                                     .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).place)
                                                     .build())
                                      .field(GraphQLFieldDefinition.newFieldDefinition()
                                                     .name("distance")
                                                     .type(Scalars.GraphQLInt)
                                                     .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).distance)
                                                     .build())
                                      .build();

        stopPlaceType = GraphQLObjectType.newObject()
                                .name("StopPlace")
                                .withInterface(nodeInterface)
                                .withInterface(placeInterface)
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("id")
                                               .type(new GraphQLNonNull(Scalars.GraphQLID))
                                               .dataFetcher(environment ->
                                                                    toIdString(((Stop) environment.getSource()).getId()))
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("name")
                                               .type(new GraphQLNonNull(Scalars.GraphQLString))
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("lat")
                                               .type(Scalars.GraphQLFloat)
                                               .dataFetcher(environment -> (((Stop) environment.getSource()).getLat()))
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("lon")
                                               .type(Scalars.GraphQLFloat)
                                               .dataFetcher(environment -> (((Stop) environment.getSource()).getLon()))
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("code")
                                               .type(Scalars.GraphQLString)
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("description")
                                               .type(Scalars.GraphQLString)
                                               .dataFetcher(environment -> (((Stop) environment.getSource()).getDesc()))
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("zoneId")
                                               .type(Scalars.GraphQLString)
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("url")
                                               .type(Scalars.GraphQLString)
                                               .build())

                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("wheelchairBoarding")
                                               .type(wheelchairBoardingEnum)
                                               .build())

                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("timezone")
                                               .type(Scalars.GraphQLString)
                                               .build())
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("transportMode")
                                               .type(transportModeEnum)
                                               .dataFetcher(environment -> mapVehicleTypeToTraverseMode(((Stop) environment.getSource()).getVehicleType()))
                                               .build())
                                // TODO stopPlaceType?

                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("quays")
                                               .description("Returns all quays that are children of this stop place")
                                               .type(new GraphQLList(stopPlaceType))
                                               .dataFetcher(environment -> index.stopsForParentStation.get(((Stop) environment.getSource()).getId()))
                                               .build())

                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                               .name("estimatedCalls")
                                               .type(new GraphQLList(estimatedCallType))
                                               .argument(GraphQLArgument.newArgument()
                                                                 .name("startTime")
                                                                 .type(Scalars.GraphQLLong)
                                                                 .defaultValue(0L) // Default value is current time
                                                                 .build())
                                               .argument(GraphQLArgument.newArgument()
                                                                 .name("timeRange")
                                                                 .type(Scalars.GraphQLInt)
                                                                 .defaultValue(24 * 60 * 60)
                                                                 .build())
                                               .argument(GraphQLArgument.newArgument()
                                                                 .name("numberOfDepartures")
                                                                 .type(Scalars.GraphQLInt)
                                                                 .defaultValue(5)
                                                                 .build())
                                               .argument(GraphQLArgument.newArgument()
                                                                 .name("omitNonBoarding")
                                                                 .type(Scalars.GraphQLBoolean)
                                                                 .defaultValue(false)
                                                                 .build())
                                               .dataFetcher(environment -> {
                                                   boolean omitNonBoarding = environment.getArgument("omitNonBoarding");
                                                   Stop stop = environment.getSource();
                                                   if (stop.getLocationType() != 1) {
                                                       // Not a stop place
                                                       return null;
                                                   }
                                                   return index.stopsForParentStation
                                                                  .get(stop.getId())
                                                                  .stream()
                                                                  .flatMap(singleStop ->
                                                                                   index.stopTimesForStop(singleStop,
                                                                                           environment.getArgument("startTime"),
                                                                                           environment.getArgument("timeRange"),
                                                                                           environment.getArgument("numberOfDepartures"),
                                                                                           omitNonBoarding)
                                                                                           .stream())
                                                                  .flatMap(stoptimesWithPattern -> stoptimesWithPattern.times.stream())
                                                                  .sorted(Comparator.comparing(t -> t.serviceDay + t.realtimeDeparture))
                                                                  .distinct()
                                                                  .limit((long) (int) environment.getArgument("numberOfDepartures"))
                                                                  .collect(Collectors.toList());
                                               })
                                               .build())
                                .build();

        quayType = GraphQLObjectType.newObject()
                           .name("Quay")
                           .withInterface(nodeInterface)
                           .withInterface(placeInterface)
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("id")
                                          .type(new GraphQLNonNull(Scalars.GraphQLID))
                                          .dataFetcher(environment ->
                                                               toIdString(((Stop) environment.getSource()).getId()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("name")
                                          .type(new GraphQLNonNull(Scalars.GraphQLString))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("lat")
                                          .type(Scalars.GraphQLFloat)
                                          .dataFetcher(environment -> (((Stop) environment.getSource()).getLat()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("lon")
                                          .type(Scalars.GraphQLFloat)
                                          .dataFetcher(environment -> (((Stop) environment.getSource()).getLon()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("code")
                                          .type(Scalars.GraphQLString)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("description")
                                          .type(Scalars.GraphQLString)
                                          .dataFetcher(environment -> (((Stop) environment.getSource()).getDesc()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("zoneId")
                                          .type(Scalars.GraphQLString)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("url")
                                          .type(Scalars.GraphQLString)
                                          .build())

                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("stopPlace")
                                          .type(stopPlaceType)
                                          .dataFetcher(environment -> ((Stop) environment.getSource()).getParentStation() != null ?
                                                                              index.stationForId.get(new AgencyAndId(
                                                                                                                            ((Stop) environment.getSource()).getId().getAgencyId(),
                                                                                                                            ((Stop) environment.getSource()).getParentStation())) : null)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("wheelchairBoarding")
                                          .type(wheelchairBoardingEnum)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("direction")
                                          .type(Scalars.GraphQLString)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("timezone")
                                          .type(Scalars.GraphQLString)
                                          .build())

                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("platformCode")
                                          .type(Scalars.GraphQLString)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("lines")
                                          .type(new GraphQLList(new GraphQLNonNull(lineType)))
                                          .dataFetcher(environment -> index.patternsForStop
                                                                              .get(environment.getSource())
                                                                              .stream()
                                                                              .map(pattern -> pattern.route)
                                                                              .distinct()
                                                                              .collect(Collectors.toList()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("journeyPatterns")
                                          .type(new GraphQLList(journeyPatternType))
                                          .dataFetcher(environment -> index.patternsForStop.get(environment.getSource()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("transfers")               //TODO: add max distance as parameter?
                                          .type(new GraphQLList(quayAtDistance))
                                          .dataFetcher(environment -> index.stopVertexForStop
                                                                              .get(environment.getSource())
                                                                              .getOutgoing()
                                                                              .stream()
                                                                              .filter(edge -> edge instanceof SimpleTransfer)
                                                                              .map(edge -> new ImmutableMap.Builder<String, Object>()
                                                                                                   .put("quay", ((TransitVertex) edge.getToVertex()).getStop())
                                                                                                   .put("distance", edge.getDistance())
                                                                                                   .build())
                                                                              .collect(Collectors.toList()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("estimatedCalls")
                                          .type(new GraphQLList(estimatedCallType))
                                          .argument(GraphQLArgument.newArgument()
                                                            .name("startTime")
                                                            .type(Scalars.GraphQLLong)
                                                            .defaultValue(0L) // Default value is current time
                                                            .build())
                                          .argument(GraphQLArgument.newArgument()
                                                            .name("timeRange")
                                                            .type(Scalars.GraphQLInt)
                                                            .defaultValue(24 * 60 * 60)
                                                            .build())
                                          .argument(GraphQLArgument.newArgument()
                                                            .name("numberOfDepartures")
                                                            .type(Scalars.GraphQLInt)
                                                            .defaultValue(5)
                                                            .build())
                                          .argument(GraphQLArgument.newArgument()
                                                            .name("omitNonBoarding")
                                                            .type(Scalars.GraphQLBoolean)
                                                            .defaultValue(false)
                                                            .build())
                                          .dataFetcher(environment -> {
                                              boolean omitNonBoarding = environment.getArgument("omitNonBoarding");
                                              Stop stop = environment.getSource();

                                              return index.stopTimesForStop(
                                                      environment.getSource(),
                                                      environment.getArgument("startTime"),
                                                      environment.getArgument("timeRange"),
                                                      environment.getArgument("numberOfDepartures"),
                                                      omitNonBoarding
                                              ).stream()
                                                             .flatMap(stoptimesWithPattern -> stoptimesWithPattern.times.stream())
                                                             .sorted(Comparator.comparing(t -> t.serviceDay + t.realtimeDeparture))
                                                             .distinct()
                                                             .limit((long) (int) environment.getArgument("numberOfDepartures"))
                                                             .collect(Collectors.toList());
                                          })
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("alerts")
                                          .description("Get all alerts active for the quay")
                                          .type(new GraphQLList(alertType))
                                          .dataFetcher(dataFetchingEnvironment -> index.getAlertsForStop(
                                                  dataFetchingEnvironment.getSource()))
                                          .build())
                           .build();

        timetabledPassingTimeType = GraphQLObjectType.newObject()
                                            .name("TimetabledPassingTime")
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("quay")
                                                           .type(quayType)
                                                           .dataFetcher(environment -> index.stopForId
                                                                                               .get(((TripTimeShort) environment.getSource()).stopId))
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("arrivalTime")
                                                           .type(Scalars.GraphQLInt)
                                                           .dataFetcher(
                                                                   environment -> ((TripTimeShort) environment.getSource()).scheduledArrival)
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("departureTime")
                                                           .type(Scalars.GraphQLInt)
                                                           .dataFetcher(
                                                                   environment -> ((TripTimeShort) environment.getSource()).scheduledDeparture)
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("timingPoint")
                                                           .type(Scalars.GraphQLBoolean)
                                                           .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).timepoint)
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("forBoarding")
                                                           .type(Scalars.GraphQLBoolean)
                                                           .dataFetcher(environment -> index.patternForTrip
                                                                                               .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                                                                                               .getBoardType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE)
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("forAlighting")
                                                           .type(Scalars.GraphQLBoolean)
                                                           .dataFetcher(environment -> index.patternForTrip
                                                                                               .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                                                                                               .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE)
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("operatingDay")
                                                           .type(Scalars.GraphQLLong)
                                                           .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).serviceDay)
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("serviceJourney")
                                                           .type(serviceJourneyType)
                                                           .dataFetcher(environment -> index.tripForId
                                                                                               .get(((TripTimeShort) environment.getSource()).tripId))
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("destinationDisplay")
                                                           .type(Scalars.GraphQLString)
                                                           .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).headsign)
                                                           .build())
                                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                                           .name("notices")
                                                           .type(new GraphQLList(noticeType))
                                                           .dataFetcher(environment -> {
                                                               TripTimeShort tripTimeShort = environment.getSource();
                                                               return index.getNoticesForElement(tripTimeShort.stopTimeId);
                                                           })
                                                           .build())
                                            .build();

        estimatedCallType = GraphQLObjectType.newObject()
                                    .name("EstimatedCall")
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("quay")
                                                   .type(quayType)
                                                   .dataFetcher(environment -> index.stopForId
                                                                                       .get(((TripTimeShort) environment.getSource()).stopId))
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("aimedArrivalTime")
                                                   .type(Scalars.GraphQLInt)
                                                   .dataFetcher(
                                                           environment -> ((TripTimeShort) environment.getSource()).scheduledArrival)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("expectedArrivalTime")
                                                   .type(Scalars.GraphQLInt)
                                                   .dataFetcher(
                                                           environment -> ((TripTimeShort) environment.getSource()).realtimeArrival)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("aimedDepartureTime")
                                                   .type(Scalars.GraphQLInt)
                                                   .dataFetcher(
                                                           environment -> ((TripTimeShort) environment.getSource()).scheduledDeparture)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("expectedDepartureTime")
                                                   .type(Scalars.GraphQLInt)
                                                   .dataFetcher(
                                                           environment -> ((TripTimeShort) environment.getSource()).realtimeDeparture)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("timingPoint")
                                                   .type(Scalars.GraphQLBoolean)
                                                   .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).timepoint)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("realtime")
                                                   .type(Scalars.GraphQLBoolean)
                                                   .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).realtime)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("realtimeState")
                                                   .type(realtimeStateEnum)
                                                   .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).realtimeState)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("forBoarding")
                                                   .type(Scalars.GraphQLBoolean)
                                                   .dataFetcher(environment -> index.patternForTrip
                                                                                       .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                                                                                       .getBoardType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("forAlighting")
                                                   .type(Scalars.GraphQLBoolean)
                                                   .dataFetcher(environment -> index.patternForTrip
                                                                                       .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                                                                                       .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("operatingDay")
                                                   .type(Scalars.GraphQLLong)
                                                   .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).serviceDay)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("serviceJourney")
                                                   .type(serviceJourneyType)
                                                   .dataFetcher(environment -> index.tripForId
                                                                                       .get(((TripTimeShort) environment.getSource()).tripId))
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("destinationDisplay")
                                                   .type(Scalars.GraphQLString)
                                                   .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).headsign)
                                                   .build())
                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                   .name("notices")
                                                   .type(new GraphQLList(noticeType))
                                                   .dataFetcher(environment -> {
                                                       TripTimeShort tripTimeShort = environment.getSource();
                                                       return index.getNoticesForElement(tripTimeShort.stopTimeId);
                                                   })
                                                   .build())
                                    .build();

        serviceJourneyType = GraphQLObjectType.newObject()
                                     .name("ServiceJourney")
                                     .withInterface(nodeInterface)
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("id")
                                                    .type(new GraphQLNonNull(Scalars.GraphQLID))
                                                    .dataFetcher(environment ->
                                                                         toIdString(((Trip) environment.getSource()).getId()))
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("line")
                                                    .type(new GraphQLNonNull(lineType))
                                                    .dataFetcher(environment -> ((Trip) environment.getSource()).getRoute())
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("activeDates")
                                                    .type(new GraphQLList(Scalars.GraphQLString))
                                                    .dataFetcher(environment -> index.graph.getCalendarService()
                                                                                        .getServiceDatesForServiceId((((Trip) environment.getSource()).getServiceId()))
                                                                                        .stream()
                                                                                        .map(ServiceDate::getAsString)
                                                                                        .collect(Collectors.toList())
                                                    )
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("publicCode")
                                                    .type(Scalars.GraphQLString)
                                                    .dataFetcher(environment -> (((Trip) environment.getSource()).getTripShortName()))
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("linePublicCode")
                                                    .type(Scalars.GraphQLString)
                                                    .dataFetcher(environment -> (((Trip) environment.getSource()).getRouteShortName()))
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("directionType")
                                                    .type(directionTypeEnum)
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("wheelchairAccessible")
                                                    .type(wheelchairBoardingEnum)
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("bikesAllowed")
                                                    .type(bikesAllowedEnum)
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("journeyPattern")
                                                    .type(journeyPatternType)
                                                    .dataFetcher(
                                                            environment -> index.patternForTrip.get(environment.getSource()))
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("quays")
                                                    .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
                                                    .dataFetcher(environment -> index.patternForTrip
                                                                                        .get(environment.getSource()).getStops())
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("timetabledPassingTimes")
                                                    .type(new GraphQLList(timetabledPassingTimeType))
                                                    .description("Returns scheduled passing times only - without realtime-updates, for realtime-data use 'estimatedCalls'")
                                                    .dataFetcher(environment -> TripTimeShort.fromTripTimes(
                                                            index.patternForTrip.get((Trip) environment.getSource()).scheduledTimetable,
                                                            environment.getSource()))
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("estimatedCalls")
                                                    .type(new GraphQLList(estimatedCallType))
                                                    .description("Returns scheduled passingTimes updated with realtime-updates")
                                                    .argument(GraphQLArgument.newArgument()
                                                                      .name("operatingDay")
                                                                      .type(Scalars.GraphQLString)
                                                                      .description("Operating day to get estimated calls for. Format:  \"YYYYMMDD\"")
                                                                      .defaultValue(null)
                                                                      .build())
                                                    .dataFetcher(environment -> {
                                                        try {
                                                            final Trip trip = environment.getSource();

                                                            final String argServiceDay = cleanupServiceDayArgument(environment.getArgument("operatingDay"));
                                                            final ServiceDate serviceDate = argServiceDay != null
                                                                                                    ? ServiceDate.parseString(argServiceDay) : new ServiceDate();
                                                            final ServiceDay serviceDay = new ServiceDay(index.graph, serviceDate,
                                                                                                                index.graph.getCalendarService(), trip.getRoute().getAgency().getId());
                                                            TimetableSnapshotSource timetableSnapshotSource = index.graph.timetableSnapshotSource;
                                                            Timetable timetable = null;
                                                            if (timetableSnapshotSource != null) {
                                                                TimetableSnapshot timetableSnapshot = timetableSnapshotSource.getTimetableSnapshot();
                                                                if (timetableSnapshot != null) {
                                                                    // Check if realtime-data is available for trip
                                                                    TripPattern pattern = timetableSnapshot.getLastAddedTripPattern(timetableSnapshotSource.getFeedId(), trip.getId().getId(), serviceDate);
                                                                    if (pattern == null) {
                                                                        pattern = index.patternForTrip.get(trip);
                                                                    }
                                                                    timetable = timetableSnapshot.resolve(pattern, serviceDate);
                                                                }
                                                            }
                                                            if (timetable == null) {
                                                                timetable = index.patternForTrip.get(trip).scheduledTimetable;
                                                            }
                                                            return TripTimeShort.fromTripTimes(timetable, trip, serviceDay);
                                                        } catch (ParseException e) {
                                                            return null; // Invalid date format
                                                        }
                                                    })
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("geometry")
                                                    .type(new GraphQLList(new GraphQLList(Scalars.GraphQLFloat))) //TODO: Should be geometry
                                                    .dataFetcher(environment -> {
                                                                LineString geometry = index.patternForTrip
                                                                                              .get(environment.getSource())
                                                                                              .geometry;
                                                                if (geometry == null) {
                                                                    return null;
                                                                }
                                                                return Arrays.stream(geometry.getCoordinateSequence().toCoordinateArray())
                                                                               .map(coordinate -> Arrays.asList(coordinate.x, coordinate.y))
                                                                               .collect(Collectors.toList());
                                                            }
                                                    )
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("alerts")
                                                    .description("Get all alerts active for the service journey")
                                                    .type(new GraphQLList(alertType))
                                                    .dataFetcher(dataFetchingEnvironment -> index.getAlertsForTrip(
                                                            dataFetchingEnvironment.getSource()))
                                                    .build())
                                     .build();

        coordinateType = GraphQLObjectType.newObject()
                                 .name("Coordinates")
                                 .field(GraphQLFieldDefinition.newFieldDefinition()
                                                .name("lat")
                                                .type(Scalars.GraphQLFloat)
                                                .dataFetcher(
                                                        environment -> ((Coordinate) environment.getSource()).y)
                                                .build())
                                 .field(GraphQLFieldDefinition.newFieldDefinition()
                                                .name("lon")
                                                .type(Scalars.GraphQLFloat)
                                                .dataFetcher(
                                                        environment -> ((Coordinate) environment.getSource()).x)
                                                .build())
                                 .build();

        journeyPatternType = GraphQLObjectType.newObject()
                                     .name("JourneyPattern")
                                     .withInterface(nodeInterface)
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("id")
                                                    .type(new GraphQLNonNull(Scalars.GraphQLID))
                                                    .dataFetcher(environment -> relay.toGlobalId(
                                                            journeyPatternType.getName(), ((TripPattern) environment.getSource()).code))
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("line")
                                                    .type(new GraphQLNonNull(lineType))
                                                    .dataFetcher(environment -> ((TripPattern) environment.getSource()).route)
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("directionType")
                                                    .type(directionTypeEnum)
                                                    .dataFetcher(environment -> ((TripPattern) environment.getSource()).directionId)
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("name")
                                                    .type(Scalars.GraphQLString)
                                                    .dataFetcher(environment -> ((TripPattern) environment.getSource()).name)
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("destinationDisplay")
                                                    .type(Scalars.GraphQLString)
                                                    .dataFetcher(environment -> ((TripPattern) environment.getSource()).getDirection())
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("serviceJourneys")
                                                    .type(new GraphQLList(new GraphQLNonNull(serviceJourneyType)))
                                                    .dataFetcher(environment -> ((TripPattern) environment.getSource()).getTrips())
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("serviceJourneysForDate")
                                                    .argument(GraphQLArgument.newArgument()
                                                                      .name("operatingDay")
                                                                      .type(Scalars.GraphQLString)
                                                                      .build())
                                                    .type(new GraphQLList(new GraphQLNonNull(serviceJourneyType)))
                                                    .dataFetcher(environment -> {
                                                        try {
                                                            BitSet services = index.servicesRunning(
                                                                    ServiceDate.parseString(cleanupServiceDayArgument(environment.getArgument("operatingDay")))
                                                            );
                                                            return ((TripPattern) environment.getSource()).scheduledTimetable.tripTimes
                                                                           .stream()
                                                                           .filter(times -> services.get(times.serviceCode))
                                                                           .map(times -> times.trip)
                                                                           .collect(Collectors.toList());
                                                        } catch (ParseException e) {
                                                            return null; // Invalid date format
                                                        }
                                                    })
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("quays")
                                                    .type(new GraphQLList(new GraphQLNonNull(quayType)))
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("geometry")
                                                    .type(new GraphQLList(coordinateType))
                                                    .dataFetcher(environment -> {
                                                        LineString geometry = ((TripPattern) environment.getSource()).geometry;
                                                        if (geometry == null) {
                                                            return null;
                                                        } else {
                                                            return Arrays.asList(geometry.getCoordinates());
                                                        }
                                                    })
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("alerts")
                                                    .description("Get all alerts active for the journey pattern")
                                                    .type(new GraphQLList(alertType))
                                                    .dataFetcher(dataFetchingEnvironment -> index.getAlertsForPattern(
                                                            dataFetchingEnvironment.getSource()))
                                                    .build())
                                     .field(GraphQLFieldDefinition.newFieldDefinition()
                                                    .name("notices")
                                                    .type(new GraphQLList(noticeType))
                                                    .dataFetcher(environment -> {
                                                        TripPattern tripPattern = environment.getSource();
                                                        return index.getNoticesForElement(tripPattern.id);
                                                    })
                                                    .build())
                                     .build();


        lineType = GraphQLObjectType.newObject()
                           .name("Line")
                           .withInterface(nodeInterface)
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("id")
                                          .type(new GraphQLNonNull(Scalars.GraphQLID))
                                          .dataFetcher(environment ->
                                                               toIdString(((Route) environment.getSource()).getId()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("operator")
                                          .type(organisationType)
                                          .dataFetcher(environment -> (((Route) environment.getSource()).getAgency()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("publicCode")
                                          .type(Scalars.GraphQLString)
                                          .dataFetcher(environment -> (((Route) environment.getSource()).getShortName()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("name")
                                          .type(Scalars.GraphQLString)
                                          .dataFetcher(environment -> (((Route) environment.getSource()).getLongName()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("transportMode")
                                          .type(transportModeEnum)
                                          .dataFetcher(environment -> GtfsLibrary.getTraverseMode(
                                                  environment.getSource()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("transportSubmode")
                                          .type(transportSubmode)
                                          .dataFetcher(environment -> transportSubmodeMapper.toTransmodel(((Route) environment.getSource()).getType()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("description")
                                          .type(Scalars.GraphQLString)
                                          .dataFetcher(environment -> ((Route) environment.getSource()).getDesc())
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("url")
                                          .type(Scalars.GraphQLString)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("colourName")
                                          .type(Scalars.GraphQLString)
                                          .dataFetcher(environment -> ((Route) environment.getSource()).getColor())
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("textColourName")
                                          .type(Scalars.GraphQLString)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("bikesAllowed")
                                          .type(bikesAllowedEnum)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("journeyPatterns")
                                          .type(new GraphQLList(journeyPatternType))
                                          .dataFetcher(environment -> index.patternsForRoute
                                                                              .get(environment.getSource()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("quays")
                                          .type(new GraphQLList(quayType))
                                          .dataFetcher(environment -> index.patternsForRoute
                                                                              .get(environment.getSource())
                                                                              .stream()
                                                                              .map(TripPattern::getStops)
                                                                              .flatMap(Collection::stream)
                                                                              .distinct()
                                                                              .collect(Collectors.toList()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("serviceJourneys")
                                          .type(new GraphQLList(serviceJourneyType))
                                          .dataFetcher(environment -> index.patternsForRoute
                                                                              .get(environment.getSource())
                                                                              .stream()
                                                                              .map(TripPattern::getTrips)
                                                                              .flatMap(Collection::stream)
                                                                              .distinct()
                                                                              .collect(Collectors.toList()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("notices")
                                          .type(new GraphQLList(noticeType))
                                          .dataFetcher(environment -> {
                                              Route route = environment.getSource();
                                              return index.getNoticesForElement(route.getId());
                                          })
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("alerts")
                                          .description("Get all alerts active for the line")
                                          .type(new GraphQLList(alertType))
                                          .dataFetcher(dataFetchingEnvironment -> index.getAlertsForRoute(
                                                  dataFetchingEnvironment.getSource()))
                                          .build())
                           .build();

        organisationType = GraphQLObjectType.newObject()
                                   .name("Organisation")
                                   .description("Organisation in the graph")
                                   .withInterface(nodeInterface)
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("id")
                                                  .description("Organisation id")
                                                  .type(new GraphQLNonNull(Scalars.GraphQLID))
                                                  .dataFetcher(environment -> ((Agency) environment.getSource()).getId())
                                                  .build())
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("name")
                                                  .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                  .build())
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("url")
                                                  .type(Scalars.GraphQLString)
                                                  .build())
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("timezone")
                                                  .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                  .build())
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("lang")
                                                  .type(Scalars.GraphQLString)
                                                  .build())
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("phone")
                                                  .type(Scalars.GraphQLString)
                                                  .build())
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("fareUrl")
                                                  .type(Scalars.GraphQLString)
                                                  .build())
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("lines")
                                                  .type(new GraphQLList(lineType))
                                                  .dataFetcher(environment -> index.routeForId.values()
                                                                                      .stream()
                                                                                      .filter(route -> route.getAgency() == environment.getSource())
                                                                                      .collect(Collectors.toList()))
                                                  .build())
                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                  .name("alerts")
                                                  .description("Get all alerts active for the organisation")
                                                  .type(new GraphQLList(alertType))
                                                  .dataFetcher(dataFetchingEnvironment -> index.getAlertsForAgency(
                                                          dataFetchingEnvironment.getSource()))
                                                  .build())
                                   .build();

        bikeRentalStationType = GraphQLObjectType.newObject()
                                        .name("BikeRentalStation")
                                        .withInterface(nodeInterface)
                                        .withInterface(placeInterface)
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("id")
                                                       .type(new GraphQLNonNull(Scalars.GraphQLID))
                                                       .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).id)
                                                       .build())
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("name")
                                                       .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                       .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).getName())
                                                       .build())
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("bikesAvailable")
                                                       .type(Scalars.GraphQLInt)
                                                       .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).bikesAvailable)
                                                       .build())
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("spacesAvailable")
                                                       .type(Scalars.GraphQLInt)
                                                       .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).spacesAvailable)
                                                       .build())
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("realtime")
                                                       .type(Scalars.GraphQLBoolean)
                                                       .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).realTimeData)
                                                       .build())
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("allowDropoff")
                                                       .type(Scalars.GraphQLBoolean)
                                                       .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).allowDropoff)
                                                       .build())
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("networks")
                                                       .type(new GraphQLList(Scalars.GraphQLString))
                                                       .dataFetcher(environment -> new ArrayList<>(((BikeRentalStation) environment.getSource()).networks))
                                                       .build())
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("lon")
                                                       .type(Scalars.GraphQLFloat)
                                                       .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).x)
                                                       .build())
                                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                                       .name("lat")
                                                       .type(Scalars.GraphQLFloat)
                                                       .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).y)
                                                       .build())
                                        .build();

        bikeParkType = GraphQLObjectType.newObject()
                               .name("BikePark")
                               .withInterface(nodeInterface)
                               .withInterface(placeInterface)
                               .field(GraphQLFieldDefinition.newFieldDefinition()
                                              .name("id")
                                              .type(new GraphQLNonNull(Scalars.GraphQLID))
                                              .dataFetcher(environment -> ((BikePark) environment.getSource()).id)
                                              .build())
                               .field(GraphQLFieldDefinition.newFieldDefinition()
                                              .name("name")
                                              .type(new GraphQLNonNull(Scalars.GraphQLString))
                                              .dataFetcher(environment -> ((BikePark) environment.getSource()).name)
                                              .build())
                               .field(GraphQLFieldDefinition.newFieldDefinition()
                                              .name("spacesAvailable")
                                              .type(Scalars.GraphQLInt)
                                              .dataFetcher(environment -> ((BikePark) environment.getSource()).spacesAvailable)
                                              .build())
                               .field(GraphQLFieldDefinition.newFieldDefinition()
                                              .name("realtime")
                                              .type(Scalars.GraphQLBoolean)
                                              .dataFetcher(environment -> ((BikePark) environment.getSource()).realTimeData)
                                              .build())
                               .field(GraphQLFieldDefinition.newFieldDefinition()
                                              .name("lon")
                                              .type(Scalars.GraphQLFloat)
                                              .dataFetcher(environment -> ((BikePark) environment.getSource()).x)
                                              .build())
                               .field(GraphQLFieldDefinition.newFieldDefinition()
                                              .name("lat")
                                              .type(Scalars.GraphQLFloat)
                                              .dataFetcher(environment -> ((BikePark) environment.getSource()).y)
                                              .build())
                               .build();

        carParkType = GraphQLObjectType.newObject()
                              .name("CarPark")
                              .withInterface(nodeInterface)
                              .withInterface(placeInterface)
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("id")
                                             .type(new GraphQLNonNull(Scalars.GraphQLID))
                                             .dataFetcher(environment -> ((CarPark) environment.getSource()).id)
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("name")
                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                             .dataFetcher(environment -> ((CarPark) environment.getSource()).name)
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("maxCapacity")
                                             .type(Scalars.GraphQLInt)
                                             .dataFetcher(environment -> ((CarPark) environment.getSource()).maxCapacity)
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("spacesAvailable")
                                             .type(Scalars.GraphQLInt)
                                             .dataFetcher(environment -> ((CarPark) environment.getSource()).spacesAvailable)
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("realtime")
                                             .type(Scalars.GraphQLBoolean)
                                             .dataFetcher(environment -> ((CarPark) environment.getSource()).realTimeData)
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("lon")
                                             .type(Scalars.GraphQLFloat)
                                             .dataFetcher(environment -> ((CarPark) environment.getSource()).x)
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("lat")
                                             .type(Scalars.GraphQLFloat)
                                             .dataFetcher(environment -> ((CarPark) environment.getSource()).y)
                                             .build())
                              .build();

        GraphQLInputObjectType filterInputType = GraphQLInputObjectType.newInputObject()
                                                         .name("InputFilters")
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("quays")
                                                                        .description("Quays to include by id.")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("lines")
                                                                        .description("Lines to include by id.")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("bikeRentalStations")
                                                                        .description("Bike rentals to include by id.")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("bikeParks")
                                                                        .description("Bike parks to include by id.")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         .field(GraphQLInputObjectField.newInputObjectField()
                                                                        .name("carParks")
                                                                        .description("Car parks to include by id.")
                                                                        .type(new GraphQLList(Scalars.GraphQLString))
                                                                        .build())
                                                         .build();


        queryType = GraphQLObjectType.newObject()
                            .name("QueryType")
                            .field(tripFieldType)
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("stopPlace")
                                           .description("Get a single stopPlace based on its id)")
                                           .type(stopPlaceType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> index.stationForId
                                                                               .get(fromIdString(environment.getArgument("id"))))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("stopPlaces")
                                           .description("Get all stopPlaces)")
                                           .type(new GraphQLList(stopPlaceType))
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("ids")
                                                             .type(new GraphQLList(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> {
                                               if ((environment.getArgument("ids") instanceof List)) {
                                                   return ((List<String>) environment.getArgument("ids"))
                                                                  .stream()
                                                                  .map(id -> index.stationForId.get(GtfsLibrary.convertIdFromString(id)))
                                                                  .collect(Collectors.toList());
                                               }
                                               return new ArrayList<>(index.stationForId.values());
                                           })
                                           .dataFetcher(environment -> new ArrayList<>(index.stationForId.values()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("quay")
                                           .description("Get a single quay based on its id)")
                                           .type(quayType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> index.stopForId
                                                                               .get(fromIdString(environment.getArgument("id"))))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("quays")
                                           .description("Get all quays")
                                           .type(new GraphQLList(quayType))
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("ids")
                                                             .type(new GraphQLList(Scalars.GraphQLString))
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("name")
                                                             .type(Scalars.GraphQLString)
                                                             .build())
                                           .dataFetcher(environment -> {
                                               if ((environment.getArgument("ids") instanceof List)) {
                                                   if (environment.getArguments().entrySet()
                                                               .stream()
                                                               .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                                                               .collect(Collectors.toList())
                                                               .size() != 1) {
                                                       throw new IllegalArgumentException("Unable to combine other filters with ids");
                                                   }
                                                   return ((List<String>) environment.getArgument("ids"))
                                                                  .stream()
                                                                  .map(id -> index.stopForId.get(fromIdString(id)))
                                                                  .collect(Collectors.toList());
                                               }
                                               Stream<Stop> stream;
                                               if (environment.getArgument("name") == null) {
                                                   stream = index.stopForId.values().stream();
                                               } else {
                                                   stream = index.getLuceneIndex().query(environment.getArgument("name"), true, true, false, false)
                                                                    .stream()
                                                                    .map(result -> index.stopForId.get(fromIdString(result.id)));
                                               }
                                               return stream.collect(Collectors.toList());
                                           })
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("quaysByBbox")
                                           .description("Get all quays within the specified bounding box")
                                           .type(new GraphQLList(quayType))
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("minLat")
                                                             .type(Scalars.GraphQLFloat)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("minLon")
                                                             .type(Scalars.GraphQLFloat)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("maxLat")
                                                             .type(Scalars.GraphQLFloat)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("maxLon")
                                                             .type(Scalars.GraphQLFloat)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("organisation")
                                                             .type(Scalars.GraphQLString)
                                                             .build())
                                           .dataFetcher(environment -> index.graph.streetIndex
                                                                               .getTransitStopForEnvelope(new Envelope(
                                                                                                                              new Coordinate(environment.getArgument("minLon"),
                                                                                                                                                    environment.getArgument("minLat")),
                                                                                                                              new Coordinate(environment.getArgument("maxLon"),
                                                                                                                                                    environment.getArgument("maxLat"))))
                                                                               .stream()
                                                                               .map(TransitVertex::getStop)
                                                                               .filter(stop -> environment.getArgument("organisation") == null || stop.getId()
                                                                                                                                                          .getAgencyId().equalsIgnoreCase(environment.getArgument("organisation")))
                                                                               .collect(Collectors.toList()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("quaysByRadius")
                                           .description(
                                                   "Get all quays within the specified radius from a location. The returned type has two fields quay and distance")
                                           .type(relay.connectionType("quaytDistance",
                                                   relay.edgeType("quayAtDistance", quayAtDistance, null, new ArrayList<>()),
                                                   new ArrayList<>()))
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("lat")
                                                             .description("Latitude of the location")
                                                             .type(Scalars.GraphQLFloat)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("lon")
                                                             .description("Longitude of the location")
                                                             .type(Scalars.GraphQLFloat)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("radius")
                                                             .description("Radius (in meters) to search for from the specified location")
                                                             .type(Scalars.GraphQLInt)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("organisation")
                                                             .type(Scalars.GraphQLString)
                                                             .build())
                                           .argument(relay.getConnectionFieldArguments())
                                           .dataFetcher(environment -> {
                                               List<GraphIndex.StopAndDistance> stops;
                                               try {
                                                   stops = index.findClosestStopsByWalking(
                                                           environment.getArgument("lat"),
                                                           environment.getArgument("lon"),
                                                           environment.getArgument("radius"))
                                                                   .stream()
                                                                   .filter(stopAndDistance -> environment.getArgument("organisation") == null ||
                                                                                                      stopAndDistance.stop.getId().getAgencyId()
                                                                                                              .equalsIgnoreCase(environment.getArgument("organisation")))
                                                                   .sorted(Comparator.comparing(s -> s.distance))
                                                                   .collect(Collectors.toList());
                                               } catch (VertexNotFoundException e) {
                                                   stops = Collections.emptyList();
                                               }

                                               return new SimpleListConnection(stops).get(environment);
                                           })
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("nearest")
                                           .description(
                                                   "Get all places (quays, stop places, car parks etc. with coordinates) within the specified radius from a location. The returned type has two fields place and distance. The search is done by walking so the distance is according to the network of walkables.")
                                           .type(relay.connectionType("placeAtDistance",
                                                   relay.edgeType("placeAtDistance", placeAtDistanceType, null, new ArrayList<>()),
                                                   new ArrayList<>()))
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("lat")
                                                             .description("Latitude of the location")
                                                             .type(Scalars.GraphQLFloat)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("lon")
                                                             .description("Longitude of the location")
                                                             .type(Scalars.GraphQLFloat)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("maxDistance")
                                                             .description("Maximum distance (in meters) to search for from the specified location. Default is 2000m.")
                                                             .defaultValue(2000)
                                                             .type(Scalars.GraphQLInt)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("maxResults")
                                                             .description("Maximum number of results. Search is stopped when this limit is reached. Default is 20.")
                                                             .defaultValue(20)
                                                             .type(Scalars.GraphQLInt)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("filterByPlaceTypes")
                                                             .description("Only include places that imply this type. i.e. mode for quay, stop place etc. Also BICYCLE_RENT for bike rental stations.")
                                                             .type(new GraphQLList(filterPlaceTypeEnum))
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("filterByModes")
                                                             .description("Only include places that include this mode. Only checked for places with mode i.e. quays, departures.")
                                                             .type(new GraphQLList(modeEnum))
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("filterByIds")
                                                             .description("Only include places that match one of the given ids.")
                                                             .type(filterInputType)
                                                             .build())
                                           .argument(relay.getConnectionFieldArguments())
                                           .dataFetcher(environment -> {
                                               List<AgencyAndId> filterByStops = null;
                                               List<AgencyAndId> filterByRoutes = null;
                                               List<String> filterByBikeRentalStations = null;
                                               List<String> filterByBikeParks = null;
                                               List<String> filterByCarParks = null;
                                               @SuppressWarnings("rawtypes")
                                               Map filterByIds = environment.getArgument("filterByIds");
                                               if (filterByIds != null) {
                                                   filterByStops = toIdList(((List<String>) filterByIds.get("quays")));
                                                   filterByRoutes = toIdList(((List<String>) filterByIds.get("lines")));
                                                   filterByBikeRentalStations = filterByIds.get("bikeRentalStations") != null ? (List<String>) filterByIds.get("bikeRentalStations") : Collections.emptyList();
                                                   filterByBikeParks = filterByIds.get("bikeParks") != null ? (List<String>) filterByIds.get("bikeParks") : Collections.emptyList();
                                                   filterByCarParks = filterByIds.get("carParks") != null ? (List<String>) filterByIds.get("carParks") : Collections.emptyList();
                                               }

                                               List<TraverseMode> filterByTransportModes = environment.getArgument("filterByTransportModes");
                                               List<GraphIndex.PlaceType> filterByPlaceTypes = environment.getArgument("filterByPlaceTypes");

                                               List<GraphIndex.PlaceAndDistance> places;
                                               try {
                                                   places = index.findClosestPlacesByWalking(
                                                           environment.getArgument("lat"),
                                                           environment.getArgument("lon"),
                                                           environment.getArgument("maxDistance"),
                                                           environment.getArgument("maxResults"),
                                                           filterByTransportModes,
                                                           filterByPlaceTypes,
                                                           filterByStops,
                                                           filterByRoutes,
                                                           filterByBikeRentalStations,
                                                           filterByBikeParks,
                                                           filterByCarParks
                                                   )
                                                                    .stream()
                                                                    .collect(Collectors.toList());
                                               } catch (VertexNotFoundException e) {
                                                   places = Collections.emptyList();
                                               }

                                               return new SimpleListConnection(places).get(environment);
                                           })
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("departure")
                                           .description("Get a single departure based on its id")
                                           .type(departureType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> GraphIndex.DepartureRow.fromId(index, environment.getArgument("id")))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("organisation")
                                           .description("Get a single organisation based on ID")
                                           .type(organisationType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment ->
                                                                index.getAgencyWithoutFeedId(environment.getArgument("id")))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("organisations")
                                           .description("Get all organisations")
                                           .type(new GraphQLList(organisationType))
                                           .dataFetcher(environment -> new ArrayList<>(index.getAllAgencies()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("line")
                                           .description("Get a single line based on its id")
                                           .type(lineType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> index.routeForId
                                                                               .get(fromIdString(environment.getArgument("id"))))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("lines")
                                           .description("Get all lines")
                                           .type(new GraphQLList(lineType))
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("ids")
                                                             .type(new GraphQLList(Scalars.GraphQLString))
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("name")
                                                             .type(Scalars.GraphQLString)
                                                             .build())
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("transportModes")
                                                             .type(Scalars.GraphQLString)
                                                             .build())
                                           .dataFetcher(environment -> {
                                               if ((environment.getArgument("ids") instanceof List)) {
                                                   if (environment.getArguments().entrySet()
                                                               .stream()
                                                               .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                                                               .collect(Collectors.toList())
                                                               .size() != 1) {
                                                       throw new IllegalArgumentException("Unable to combine other filters with ids");
                                                   }
                                                   return ((List<String>) environment.getArgument("ids"))
                                                                  .stream()
                                                                  .map(id -> index.routeForId.get(fromIdString(id)))
                                                                  .collect(Collectors.toList());
                                               }
                                               Stream<Route> stream = index.routeForId.values().stream();
                                               if (environment.getArgument("name") != null) {
                                                   stream = stream
                                                                    .filter(route -> route.getShortName() != null)
                                                                    .filter(route -> route.getShortName().toLowerCase().startsWith(
                                                                            ((String) environment.getArgument("name")).toLowerCase())
                                                                    );
                                               }
                                               if (environment.getArgument("transportModes") != null) {
                                                   Set<TraverseMode> modes = new QualifiedModeSet(
                                                                                                         environment.getArgument("transportModes")).qModes
                                                                                     .stream()
                                                                                     .map(qualifiedMode -> qualifiedMode.mode)
                                                                                     .filter(TraverseMode::isTransit)
                                                                                     .collect(Collectors.toSet());
                                                   stream = stream
                                                                    .filter(route ->
                                                                                    modes.contains(GtfsLibrary.getTraverseMode(route)));
                                               }
                                               return stream.collect(Collectors.toList());
                                           })
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("serviceJourney")
                                           .description("Get a single service journey based on its id")
                                           .type(serviceJourneyType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> index.tripForId
                                                                               .get(fromIdString(environment.getArgument("id"))))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("serviceJourneys")
                                           .description("Get all service journeys")
                                           .type(new GraphQLList(serviceJourneyType))
                                           .dataFetcher(environment -> new ArrayList<>(index.tripForId.values()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("bikeRentalStations")
                                           .type(new GraphQLList(bikeRentalStationType))
                                           .dataFetcher(dataFetchingEnvironment -> new ArrayList<>(index.graph.getService(BikeRentalStationService.class).getBikeRentalStations()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("bikeRentalStation")
                                           .type(bikeRentalStationType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> index.graph.getService(BikeRentalStationService.class)
                                                                               .getBikeRentalStations()
                                                                               .stream()
                                                                               .filter(bikeRentalStation -> bikeRentalStation.id.equals(environment.getArgument("id")))
                                                                               .findFirst()
                                                                               .orElse(null))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("bikeParks")
                                           .type(new GraphQLList(bikeParkType))
                                           .dataFetcher(dataFetchingEnvironment -> new ArrayList<>(index.graph.getService(BikeRentalStationService.class).getBikeParks()))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("bikePark")
                                           .type(bikeParkType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> index.graph.getService(BikeRentalStationService.class)
                                                                               .getBikeParks()
                                                                               .stream()
                                                                               .filter(bikePark -> bikePark.id.equals(environment.getArgument("id")))
                                                                               .findFirst()
                                                                               .orElse(null))
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("carParks")
                                           .type(new GraphQLList(carParkType))
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("ids")
                                                             .type(new GraphQLList(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> {
                                               if ((environment.getArgument("ids") instanceof List)) {
                                                   Map<String, CarPark> carParks = index.graph.getService(CarParkService.class).getCarParkById();
                                                   return ((List<String>) environment.getArgument("ids"))
                                                                  .stream()
                                                                  .map(carParks::get)
                                                                  .collect(Collectors.toList());
                                               }
                                               return new ArrayList<>(index.graph.getService(CarParkService.class).getCarParks());
                                           })
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("carPark")
                                           .type(carParkType)
                                           .argument(GraphQLArgument.newArgument()
                                                             .name("id")
                                                             .type(new GraphQLNonNull(Scalars.GraphQLString))
                                                             .build())
                                           .dataFetcher(environment -> index.graph.getService(CarParkService.class)
                                                                               .getCarParks()
                                                                               .stream()
                                                                               .filter(carPark -> carPark.id.equals(environment.getArgument("id")))
                                                                               .findFirst()
                                                                               .orElse(null))
                                           .build())

                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("notices")
                                           .type(new GraphQLList(noticeType))
                                           .dataFetcher(environment -> index.getNoticeMap().values())
                                           .build())
                            .field(GraphQLFieldDefinition.newFieldDefinition()
                                           .name("alerts")
                                           .description("Get all alerts active in the graph")
                                           .type(new GraphQLList(alertType))
                                           .dataFetcher(dataFetchingEnvironment -> index.getAlerts())
                                           .build())
                            .build();

        Set<GraphQLType> dictionary = new HashSet<>();
        dictionary.add(placeInterface);

        indexSchema = GraphQLSchema.newSchema()
                              .query(queryType)
                              .build(dictionary);
    }

    //Supporting serviceDay format to be the same as date-format - for consistency
    private String cleanupServiceDayArgument(String serviceDayArgument) {
        if (serviceDayArgument != null) {
            serviceDayArgument = serviceDayArgument.replace("-", "");
        }
        return serviceDayArgument;
    }

    private List<AgencyAndId> toIdList(List<String> ids) {
        if (ids == null) return Collections.emptyList();
        return ids.stream().map(id -> fromIdString(id)).collect(Collectors.toList());
    }

    private void createPlanType(GraphIndex index) {
        final GraphQLObjectType linkGeometryType = GraphQLObjectType.newObject()
                                                           .name("PointsOnLink")
                                                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                          .name("length")
                                                                          .description("The number of points in the string")
                                                                          .type(Scalars.GraphQLInt)
                                                                          .dataFetcher(environment -> ((EncodedPolylineBean) environment.getSource()).getLength())
                                                                          .build())
                                                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                          .name("points")
                                                                          .description("The encoded points of the polyline.")
                                                                          .type(Scalars.GraphQLString)
                                                                          .dataFetcher(environment -> ((EncodedPolylineBean) environment.getSource()).getPoints())
                                                                          .build())
                                                           .build();

        final GraphQLObjectType placeType = GraphQLObjectType.newObject()
                                                    .name("Place")
                                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                   .name("name")
                                                                   .description("For transit quays, the name of the quay. For points of interest, the name of the POI.")
                                                                   .type(Scalars.GraphQLString)
                                                                   .dataFetcher(environment -> ((Place) environment.getSource()).name)
                                                                   .build())
                                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                   .name("vertexType")
                                                                   .description("Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit quay) Mostly used for better localization of bike sharing and P+R station names")
                                                                   .type(vertexTypeEnum)
                                                                   .dataFetcher(environment -> ((Place) environment.getSource()).vertexType)
                                                                   .build())
                                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                   .name("lat")
                                                                   .description("The latitude of the place.")
                                                                   .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                                                                   .dataFetcher(environment -> ((Place) environment.getSource()).lat)
                                                                   .build())
                                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                   .name("lon")
                                                                   .description("The longitude of the place.")
                                                                   .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                                                                   .dataFetcher(environment -> ((Place) environment.getSource()).lon)
                                                                   .build())
                                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                   .name("quay")
                                                                   .description("The quay related to the place.")
                                                                   .type(quayType)
                                                                   .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.TRANSIT) ? index.stopForId.get(((Place) environment.getSource()).stopId) : null)
                                                                   .build())
                                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                   .name("bikeRentalStation")
                                                                   .type(bikeRentalStationType)
                                                                   .description("The bike rental station related to the place")
                                                                   .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKESHARE) ?
                                                                                                       index.graph.getService(BikeRentalStationService.class)
                                                                                                               .getBikeRentalStations()
                                                                                                               .stream()
                                                                                                               .filter(bikeRentalStation -> bikeRentalStation.id.equals(((Place) environment.getSource()).bikeShareId))
                                                                                                               .findFirst()
                                                                                                               .orElse(null)
                                                                                                       : null)
                                                                   .build())
                                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                   .name("bikePark")
                                                                   .type(bikeParkType)
                                                                   .description("The bike parking related to the place")
                                                                   .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKEPARK) ?
                                                                                                       index.graph.getService(BikeRentalStationService.class)
                                                                                                               .getBikeParks()
                                                                                                               .stream()
                                                                                                               .filter(bikePark -> bikePark.id.equals(((Place) environment.getSource()).bikeParkId))
                                                                                                               .findFirst()
                                                                                                               .orElse(null)
                                                                                                       : null)
                                                                   .build())
                                                    .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                   .name("carPark")
                                                                   .type(carParkType)
                                                                   .description("The car parking related to the place")
                                                                   .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.PARKANDRIDE) ?
                                                                                                       index.graph.getService(CarParkService.class)
                                                                                                               .getCarParks()
                                                                                                               .stream()
                                                                                                               .filter(carPark -> carPark.id.equals(((Place) environment.getSource()).carParkId))
                                                                                                               .findFirst()
                                                                                                               .orElse(null)
                                                                                                       : null)
                                                                   .build())
                                                    .build();

        final GraphQLObjectType linkType = GraphQLObjectType.newObject()
                                                   .name("Link")
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("startTime")
                                                                  .description("The date and time this link begins.")
                                                                  .type(Scalars.GraphQLLong)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).startTime.getTime().getTime())
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("endTime")
                                                                  .description("The date and time this link ends.")
                                                                  .type(Scalars.GraphQLLong)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).endTime.getTime().getTime())
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("mode")
                                                                  .description("The mode of transport or access (e.g., foot) used when traversing this link.")
                                                                  .type(modeEnum)
                                                                  .dataFetcher(environment -> Enum.valueOf(TraverseMode.class, ((Leg) environment.getSource()).mode))
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("transportSubmode")
                                                                  .description("The transport sub mode (e.g., localBus or expressBus) used when traversing this link. Null if link is not a ride")
                                                                  .type(transportSubmode)
                                                                  .dataFetcher(environment -> transportSubmodeMapper.toTransmodel(((Leg) environment.getSource()).routeType))
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("duration")
                                                                  .description("The links's duration in seconds")
                                                                  .type(Scalars.GraphQLFloat)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).getDuration())
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("pointsOnLink")
                                                                  .description("The links's geometry.")
                                                                  .type(linkGeometryType)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).legGeometry)
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("operator")
                                                                  .description("For ride links, the transit organisation that operates the service used for this links. For non-ride links, null.")
                                                                  .type(organisationType)
                                                                  .dataFetcher(environment -> getAgency(index, ((Leg) environment.getSource()).agencyId))
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("realTime")
                                                                  .description("Whether there is real-time data about this link")
                                                                  .type(Scalars.GraphQLBoolean)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).realTime)
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("distance")
                                                                  .description("The distance traveled while traversing the link in meters.")
                                                                  .type(Scalars.GraphQLFloat)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).distance)
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("ride")
                                                                  .description("Whether this link is a ride link or not.")
                                                                  .type(Scalars.GraphQLBoolean)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).isTransitLeg())
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("rentedBike")
                                                                  .description("Whether this link is with a rented bike.")
                                                                  .type(Scalars.GraphQLBoolean)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).rentedBike)
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("from")
                                                                  .description("The Place where the link originates.")
                                                                  .type(new GraphQLNonNull(placeType))
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).from)
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("to")
                                                                  .description("The Place where the link ends.")
                                                                  .type(new GraphQLNonNull(placeType))
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).to)
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("line")
                                                                  .description("For ride links, the line. For non-ride links, null.")
                                                                  .type(lineType)
                                                                  .dataFetcher(environment -> index.routeForId.get(((Leg) environment.getSource()).routeId))
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("serviceJourney")
                                                                  .description("For ride links, the service journey. For non-ride links, null.")
                                                                  .type(serviceJourneyType)
                                                                  .dataFetcher(environment -> index.tripForId.get(((Leg) environment.getSource()).tripId))
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("intermediateQuays")
                                                                  .description("For ride links, intermediate quays between the Place where the link originates and the Place where the link ends. For non-ride links, null.")
                                                                  .type(new GraphQLList(quayType))
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).stop.stream()
                                                                                                      .filter(place -> place.stopId != null)
                                                                                                      .map(placeWithStop -> index.stopForId.get(placeWithStop.stopId))
                                                                                                      .filter(Objects::nonNull)
                                                                                                      .collect(Collectors.toList()))
                                                                  .build())
                                                   .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                  .name("intermediatePlace")
                                                                  .description("Do we continue from a specified intermediate place")
                                                                  .type(Scalars.GraphQLBoolean)
                                                                  .dataFetcher(environment -> ((Leg) environment.getSource()).intermediatePlace)
                                                                  .build())
                                                   .build();

        final GraphQLObjectType tripPatternType = GraphQLObjectType.newObject()
                                                          .name("TripPattern")
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("startTime")
                                                                         .description("Time that the trip departs.")
                                                                         .type(Scalars.GraphQLLong)
                                                                         .dataFetcher(environment -> ((Itinerary) environment.getSource()).startTime.getTime().getTime())
                                                                         .build())
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("endTime")
                                                                         .description("Time that the trip arrives.")
                                                                         .type(Scalars.GraphQLLong)
                                                                         .dataFetcher(environment -> ((Itinerary) environment.getSource()).endTime.getTime().getTime())
                                                                         .build())
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("duration")
                                                                         .description("Duration of the trip, in seconds.")
                                                                         .type(Scalars.GraphQLLong)
                                                                         .dataFetcher(environment -> ((Itinerary) environment.getSource()).duration)
                                                                         .build())
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("waitingTime")
                                                                         .description("How much time is spent waiting for transit to arrive, in seconds.")
                                                                         .type(Scalars.GraphQLLong)
                                                                         .dataFetcher(environment -> ((Itinerary) environment.getSource()).waitingTime)
                                                                         .build())
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("walkTime")
                                                                         .description("How much time is spent walking, in seconds.")
                                                                         .type(Scalars.GraphQLLong)
                                                                         .dataFetcher(environment -> ((Itinerary) environment.getSource()).walkTime)
                                                                         .build())
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("walkDistance")
                                                                         .description("How far the user has to walk, in meters.")
                                                                         .type(Scalars.GraphQLFloat)
                                                                         .dataFetcher(environment -> ((Itinerary) environment.getSource()).walkDistance)
                                                                         .build())
                                                          .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                         .name("links")
                                                                         .description("A list of Links. Each Link is either a walking (cycling, car) portion of the trip, or a ride link on a particular vehicle. So a trip where the use walks to the Q train, transfers to the 6, then walks to their destination, has four links.")
                                                                         .type(new GraphQLNonNull(new GraphQLList(linkType)))
                                                                         .dataFetcher(environment -> ((Itinerary) environment.getSource()).legs)
                                                                         .build())
                                                          .build();

        tripType = GraphQLObjectType.newObject()
                           .name("Trip")
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("date")
                                          .description("The time and date of travel")
                                          .type(Scalars.GraphQLLong)
                                          .dataFetcher(environment -> ((TripPlan) ((Map) environment.getSource()).get("plan")).date.getTime())
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("from")
                                          .description("The origin")
                                          .type(new GraphQLNonNull(placeType))
                                          .dataFetcher(environment -> ((TripPlan) ((Map) environment.getSource()).get("plan")).from)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("to")
                                          .description("The destination")
                                          .type(new GraphQLNonNull(placeType))
                                          .dataFetcher(environment -> ((TripPlan) ((Map) environment.getSource()).get("plan")).to)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("tripPatterns")
                                          .description("A list of possible trip patterns")
                                          .type(new GraphQLNonNull(new GraphQLList(tripPatternType)))
                                          .dataFetcher(environment -> ((TripPlan) ((Map) environment.getSource()).get("plan")).itinerary)
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("messageEnums")
                                          .description("A list of possible error messages as enum")
                                          .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                                          .dataFetcher(environment -> ((List<Message>) ((Map) environment.getSource()).get("messages"))
                                                                              .stream().map(Enum::name).collect(Collectors.toList()))
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("messageStrings")
                                          .description("A list of possible error messages in cleartext")
                                          .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                                          .dataFetcher(environment -> ((List<Message>) ((Map) environment.getSource()).get("messages"))
                                                                              .stream()
                                                                              .map(message -> message.get(ResourceBundleSingleton.INSTANCE.getLocale(
                                                                                      environment.getArgument("locale"))))
                                                                              .collect(Collectors.toList())
                                          )
                                          .build())
                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                          .name("debugOutput")
                                          .description("Information about the timings for the trip generation")
                                          .type(new GraphQLNonNull(GraphQLObjectType.newObject()
                                                                           .name("debugOutput")
                                                                           .field(GraphQLFieldDefinition.newFieldDefinition()
                                                                                          .name("totalTime")
                                                                                          .type(Scalars.GraphQLLong)
                                                                                          .build())
                                                                           .build()))
                                          .dataFetcher(environment -> (((Map) environment.getSource()).get("debugOutput")))
                                          .build())
                           .build();
    }

    private String toIdString(AgencyAndId agencyAndId) {
        if (fixedAgencyId != null) {
            return agencyAndId.getId();
        }
        return GtfsLibrary.convertIdToString(agencyAndId);
    }

    private AgencyAndId fromIdString(String id) {
        if (fixedAgencyId != null) {
            return new AgencyAndId(fixedAgencyId, id);
        }
        return GtfsLibrary.convertIdFromString(id);
    }


    /**
     * Add agency id prefix to vertexIds if fixed agency is set.
     */
    private String preparePlaceRef(String input) {
        if (fixedAgencyId != null && input != null) {
            GenericLocation location = GenericLocation.fromOldStyleString(input);

            if (location.hasVertexId()) {
                String prefixedPlace = prepareAgencyAndId(location.place, GTFS_LIBRARY_ID_SEPARATOR);
                return new GenericLocation(location.name, prefixedPlace).toString();
            }

        }
        return input;
    }

    private String prepareListOfAgencyAndId(List<String> ids) {
        return mapCollectionOfValues(ids, this::prepareAgencyAndId);
    }

    private String prepareListOfAgencyAndId(List<String> ids, String separator) {
        return mapCollectionOfValues(ids, value -> prepareAgencyAndId(value, separator));
    }

    private String prepareAgencyAndId(String id) {
        return prepareAgencyAndId(id, null);
    }

    private String prepareAgencyAndId(String id, String separator) {
        if (fixedAgencyId != null && id != null) {
            return separator == null
                           ? fixedAgencyId + AgencyAndId.ID_SEPARATOR + id
                           : fixedAgencyId + separator + id;
        }
        return id;
    }

    /**
     * Convert a comma separated list of transmodel mode values into a corresponding comma separated list of otp modes.
     */
    private String mapListOfModes(List<String> transmodelModes) {
        return mapCollectionOfValues(transmodelModes, this::mapMode);
    }


    private String mapCollectionOfValues(Collection<String> values, Function<String, String> mapElementFunction) {
        if (values == null) {
            return null;
        }
        List<String> otpModelModes = values.stream().map(value -> mapElementFunction.apply(value)).collect(Collectors.toList());

        return Joiner.on(LIST_VALUE_SEPARATOR).join(otpModelModes);
    }

    private String mapMode(String transmodelMode) {
        TraverseMode traverseMode = traverseModeMap.get(transmodelMode.trim());
        if (traverseMode == null) {
            throw new IllegalArgumentException("Mode not supported: " + transmodelMode);
        }
        return traverseMode.name();
    }

    // Create a dummy route to be able to reuse GtfsLibrary functionality
    private Object mapVehicleTypeToTraverseMode(int vehicleType) {
        Route dummyRoute = new Route();
        dummyRoute.setType(vehicleType);
        try {
            return GtfsLibrary.getTraverseMode(dummyRoute);
        } catch (IllegalArgumentException iae) {
            return "unknown";
        }
    }
}
