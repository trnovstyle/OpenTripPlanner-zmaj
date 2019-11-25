package org.opentripplanner.graph_builder;

import com.google.common.collect.Lists;
import org.opentripplanner.standalone.config.GraphBuilderParameters;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.opentripplanner.standalone.datastore.configure.DataStoreConfig;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.*;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.graph_builder.module.map.BusRouteStreetMatcher;
import org.opentripplanner.graph_builder.module.ned.DegreeGridNEDTileSource;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.openstreetmap.impl.BinaryFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.config.S3BucketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.opentripplanner.standalone.datastore.FileType.DEM;
import static org.opentripplanner.standalone.datastore.FileType.GTFS;
import static org.opentripplanner.standalone.datastore.FileType.NETEX;
import static org.opentripplanner.standalone.datastore.FileType.OSM;

/**
 * This makes a Graph out of various inputs like GTFS and OSM.
 * It is modular: GraphBuilderModules are placed in a list and run in sequence.
 */
public class GraphBuilder implements Runnable {

    private static Logger LOG = LoggerFactory.getLogger(GraphBuilder.class);

    private List<GraphBuilderModule> graphBuilderModules = new ArrayList<>();

    private OtpDataStore dataStore;

    private Graph graph = new Graph();

    /** Should the graph be serialized to disk after being created or not? */
    private boolean serializeGraph = true;

    private GraphBuilder(OtpDataStore dataStore) {
        this.dataStore = dataStore;
    }

    private void addModule(GraphBuilderModule loader) {
        graphBuilderModules.add(loader);
    }

    private Graph loadBaseGraph() {
        try {
            DataSource graphSource = dataStore.getBaseGraph();
            graph = Graph.load(graphSource.asInputStream(), graphSource.path());
            return graph;
        } catch (Exception e) {
            throw new RuntimeException("error loading base graph: ", e);
        }
    }

    public Graph getGraph() {
        return graph;
    }

    public void run() {
        try {
            /* Record how long it takes to build the graph, purely for informational purposes. */
            long startTime = System.currentTimeMillis();

            if (serializeGraph) {
                // Abort building a graph if the file can not be saved
                if (!dataStore.getGraph().isWritable()) {
                    throw new RuntimeException(
                            "Cannot create or overwrite graph at: "
                                    + dataStore.getGraph().path()
                    );
                }
            }

            // Check all graph builder inputs, and fail fast to avoid waiting until the build
            // process advances.
            for (GraphBuilderModule builder : graphBuilderModules) {
                builder.checkInputs();
            }

            HashMap<Class<?>, Object> extra = new HashMap<Class<?>, Object>();
            for (GraphBuilderModule load : graphBuilderModules)
                load.buildGraph(graph, extra);

            graph.summarizeBuilderAnnotations();
            if (serializeGraph) {
                try {
                    graph.save(dataStore.getGraph().asOutputStream(), dataStore.description());
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            } else {
                LOG.info("Not saving graph to disk, as requested.");
            }

            long endTime = System.currentTimeMillis();
            LOG.info(String.format("Graph building took %.1f minutes.", (endTime - startTime) / 1000 / 60.0));
        }
        catch (RuntimeException e){
            // Loading data failed, notify source
            throw e;
        }
    }

    /**
     * Factory method to create and configure a GraphBuilder with all the appropriate modules to
     * build a graph from the given data source and configuration directory.
     *
     * Using command line options: {@code params.inMemory} {@code params.preFlight} and
     * {@code params.build directory}.
     */
    public static GraphBuilder forDirectory(CommandLineParameters params, File dir) {
        try {
            LOG.info("Wiring up and configuring graph builder task.");
            LOG.info("Searching for graph builder input files in {}", dir);

            OtpDataStore dataStore = new DataStoreConfig()
                    .withSkipTransit(params.skipTransit)
                    .withBaseDirectory(dir)
                    .open();

            if (!checkInputFileTypesIsOk(dataStore.listInputFileTypes(), dataStore.description())) {
                return null;
            }
            GraphBuilderParameters builderParams = new GraphBuilderParameters(
                    dataStore.graphBuilderParameters()
            );

            GraphBuilder graphBuilder = new GraphBuilder(dataStore);
            //graphBuilder.statusFile.setInProgress();

            if (params.loadBaseGraph) {
                Graph graph = graphBuilder.loadBaseGraph();
                graph.index(new DefaultStreetVertexIndexFactory());
            }

            if (!params.loadBaseGraph) {
                if (dataStore.hasInputOf(FileType.OSM)) {
                    List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();
                    for (DataSource osmSource : dataStore.listInputFor(FileType.OSM)) {
                        osmProviders.add(new BinaryFileBasedOpenStreetMapProviderImpl(osmSource));
                    }
                    OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
                    DefaultStreetEdgeFactory streetEdgeFactory = new DefaultStreetEdgeFactory();
                    streetEdgeFactory.useElevationData = dataStore.hasInputOf(DEM);
                    osmModule.edgeFactory = streetEdgeFactory;
                    osmModule.customNamer = builderParams.customNamer;
                    osmModule.setDefaultWayPropertySetSource(builderParams.wayPropertySet);
                    osmModule.skipVisibility = !builderParams.areaVisibility;
                    osmModule.platformEntriesLinking = builderParams.platformEntriesLinking;
                    osmModule.staticBikeRental = builderParams.staticBikeRental;
                    osmModule.staticBikeParkAndRide = builderParams.staticBikeParkAndRide;
                    osmModule.staticParkAndRide = builderParams.staticParkAndRide;
                    osmModule.banDiscouragedWalking = builderParams.banDiscouragedWalking;
                    osmModule.banDiscouragedBiking = builderParams.banDiscouragedBiking;
                    graphBuilder.addModule(osmModule);
                    PruneFloatingIslands pruneFloatingIslands = new PruneFloatingIslands();
                    pruneFloatingIslands.setPruningThresholdIslandWithoutStops(builderParams.pruningThresholdIslandWithoutStops);
                    pruneFloatingIslands.setPruningThresholdIslandWithStops(builderParams.pruningThresholdIslandWithStops);
                    graphBuilder.addModule(pruneFloatingIslands);
                }
                // Load elevation data and apply it to the streets.
                // We want to do run this module after loading the OSM street network but before
                // finding transfers.
                if (builderParams.elevationBucket != null) {
                    // Download the elevation tiles from an Amazon S3 bucket
                    S3BucketConfig bucketConfig = builderParams.elevationBucket;
                    File cacheDirectory = new File(params.cacheDirectory, "ned");
                    DegreeGridNEDTileSource awsTileSource = new DegreeGridNEDTileSource();
                    awsTileSource = new DegreeGridNEDTileSource();
                    awsTileSource.awsAccessKey = bucketConfig.accessKey;
                    awsTileSource.awsSecretKey = bucketConfig.secretKey;
                    awsTileSource.awsBucketName = bucketConfig.bucketName;
                    NEDGridCoverageFactoryImpl gcf = new NEDGridCoverageFactoryImpl(cacheDirectory);
                    gcf.tileSource = awsTileSource;
                    GraphBuilderModule elevationBuilder = new ElevationModule(
                            gcf, builderParams.distanceBetweenElevationSamples);
                    graphBuilder.addModule(elevationBuilder);
                } else if (builderParams.fetchElevationUS) {
                    // Download the elevation tiles from the official web service
                    File cacheDirectory = new File(params.cacheDirectory, "ned");
                    ElevationGridCoverageFactory gcf = new NEDGridCoverageFactoryImpl(cacheDirectory);
                    GraphBuilderModule elevationBuilder = new ElevationModule(
                            gcf, builderParams.distanceBetweenElevationSamples);
                    graphBuilder.addModule(elevationBuilder);
                } else if (dataStore.hasInputOf(DEM)) {
                    // Load the elevation from a file in the graph inputs directory
                    for (DataSource demSource : dataStore.listInputFor(DEM)) {
                        ElevationGridCoverageFactory gcf = new GeotiffGridCoverageFactoryImpl(demSource);
                        GraphBuilderModule elevationBuilder = new ElevationModule(
                                gcf, builderParams.distanceBetweenElevationSamples
                        );
                        graphBuilder.addModule(elevationBuilder);
                    }
                }
            }

            if (!params.skipTransit) {
                if (dataStore.hasInputOf(GTFS)) {
                    List<GtfsBundle> gtfsBundles = new ArrayList<>();
                    for (CompositeDataSource gtfsSource : dataStore.listCompositeInputFor(GTFS)) {
                        GtfsBundle gtfsBundle = new GtfsBundle(gtfsSource);
                        gtfsBundle.setTransfersTxtDefinesStationPaths(builderParams.useTransfersTxt);
                        if (builderParams.parentStopLinking) {
                            gtfsBundle.linkStopsToParentStations = true;
                        }
                        gtfsBundle.parentStationTransfers = builderParams.stationTransfers;
                        gtfsBundle.subwayAccessTime = (int) (builderParams.subwayAccessTime * 60);
                        gtfsBundle.maxInterlineDistance = builderParams.maxInterlineDistance;
                        gtfsBundles.add(gtfsBundle);
                    }
                    GtfsModule gtfsModule = new GtfsModule(gtfsBundles);
                    gtfsModule.setFareServiceFactory(builderParams.fareServiceFactory);
                    graphBuilder.addModule(gtfsModule);
                    if (dataStore.hasInputOf(OSM)) {
                        if (builderParams.matchBusRoutesToStreets) {
                            graphBuilder.addModule(new BusRouteStreetMatcher());
                        }
                        graphBuilder.addModule(new TransitToTaggedStopsModule());
                    }
                } else if (dataStore.hasInputOf(NETEX)) {
                    List<NetexBundle> netexBundles = new ArrayList<>();
                    for (CompositeDataSource netexSource : dataStore.listCompositeInputFor(NETEX)) {
                        NetexBundle netexBundle = new NetexBundle(netexSource, builderParams);
                        netexBundles.add(netexBundle);
                    }
                    NetexModule netexModule = new NetexModule(dir, netexBundles);
                    graphBuilder.addModule(netexModule);
                    if (dataStore.hasInputOf(OSM)) {
                        if (builderParams.matchBusRoutesToStreets) {
                            graphBuilder.addModule(new BusRouteStreetMatcher());
                        }
                        graphBuilder.addModule(new TransitToTaggedStopsModule());
                    }
                }
                // This module is outside the hasGTFS conditional block because it also links things like bike rental
                // which need to be handled even when there's no transit.
                StreetLinkerModule streetLinkerModule = new StreetLinkerModule();
                streetLinkerModule.setAddExtraEdgesToAreas(builderParams.extraEdgesStopPlatformLink);
                graphBuilder.addModule(streetLinkerModule);
                if (dataStore.hasInputOf(GTFS) || dataStore.hasInputOf(NETEX)) {
                    if (builderParams.analyzeTransfers) {
                        graphBuilder.addModule(new DirectTransferAnalyzer(builderParams.maxTransferDistance));
                    } else {
                        LOG.info("Analyze transfers skipped");
                    }
                    // The stops can be linked to each other once they are already linked to the street network.
                    if (!builderParams.useTransfersTxt) {
                        // This module will use streets or straight line distance depending on whether OSM data is found in the graph.
                        graphBuilder.addModule(new DirectTransferGenerator(builderParams.maxTransferDistance));
                    }
                }
            }
            graphBuilder.addModule(
                    new EmbedConfig(
                            dataStore.graphBuilderParameters(),
                            dataStore.routerConfigParameters()
                    )
            );

            if (builderParams.htmlAnnotations) {
                graphBuilder.addModule(
                        new AnnotationsToHTML(
                                params.build,
                                builderParams.maxHtmlAnnotationsPerFile)
                );
            }
            graphBuilder.serializeGraph = (!params.inMemory) || params.preFlight;
            return graphBuilder;
        }
        catch (RuntimeException e) {
            try {
                // TODO TGR - graphBuilder.statusFile.setFailed();
            }
            catch (Exception e2) {
                LOG.error(e.getLocalizedMessage(), e2);
            }
            throw e;
        }
    }

    private static boolean checkInputFileTypesIsOk(Set<FileType> fileTypes, String source) {
        if(fileTypes.stream().noneMatch(FileType::isInputDataFile)) {
            LOG.error("No input files found, unable to build graph. Source: {}", source);
            return false;
        }
        return true;
    }
}

