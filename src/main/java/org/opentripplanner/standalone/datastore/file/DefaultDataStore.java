package org.opentripplanner.standalone.datastore.file;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.standalone.config.GraphBuilderParameters;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opentripplanner.standalone.datastore.FileType.CONFIG;
import static org.opentripplanner.standalone.datastore.FileType.DEM;
import static org.opentripplanner.standalone.datastore.FileType.GRAPH;
import static org.opentripplanner.standalone.datastore.FileType.GTFS;
import static org.opentripplanner.standalone.datastore.FileType.NETEX;
import static org.opentripplanner.standalone.datastore.FileType.OSM;
import static org.opentripplanner.standalone.datastore.FileType.PARTIAL_GRAPH;
import static org.opentripplanner.standalone.datastore.FileType.UNRECOGNIZED;
import static org.opentripplanner.standalone.datastore.file.ConfigLoader.isConfigFile;

/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class DefaultDataStore implements OtpDataStore {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDataStore.class);
    private static final String GRAPH_FILENAME = "Graph.obj";
    private static final String BASE_GRAPH_FILENAME = "baseGraph.obj";

    private final File baseDir;
    private final Multimap<FileType, DataSource> inputSources = ArrayListMultimap.create();
    private final DataSource baseGraph;

    private DataSource graph;
    private JsonNode graphBuilderParameters;
    private JsonNode routerConfigParameters;

    public DefaultDataStore(
            File baseDir, boolean skipTransit, JsonNode builderParams, JsonNode routerConfig
    ) {
        this.baseDir = baseDir;
        this.graphBuilderParameters = builderParams;
        this.routerConfigParameters = routerConfig;
        this.baseGraph = newGraphSource(baseDir, BASE_GRAPH_FILENAME);
        String graphFileName = skipTransit ? BASE_GRAPH_FILENAME : GRAPH_FILENAME;
        this.graph = newGraphSource(baseDir, graphFileName);
    }

    private static FileDataSource newGraphSource(File baseDir, String baseGraphFilename) {
        return new FileDataSource(new File(baseDir, baseGraphFilename), GRAPH);
    }

    public static CompositeDataSource compositeSource(File file, FileType type) {
        if(file.isDirectory()) {
            return new DirectoryDataSource(file, type);
        }
        if(file.getName().endsWith(".zip")) {
            return new ZipFileDataSource(file, type);
        }
        throw new IllegalArgumentException(
                "The " + file + " is not recognized as a zip-file or "
                + "directory. Unable to create composite data source for file type " + type + "."
        );
    }

    public OtpDataStore open() {
        initInputSourcesBasedOnBaseDirectory();
        return this;
    }

    @Override
    public String description() {
        return baseDir.toString();
    }

    @Override
    public void close() { /* No need to close anything when accessing local file system. */ }

    @Override
    public boolean hasInputOf(FileType type) {
        return inputSources.containsKey(type);
    }

    @Override
    public Collection<DataSource> listInputFor(FileType type) {
        return inputSources.get(type);
    }

    @Override
    public Collection<CompositeDataSource> listCompositeInputFor(FileType type) {
        if(!type.isCompositeInputDataFile()) {
          // On a local file system the only supported composite file type or archive is zip files.
          // We use zip files for transit data, GTFS and Netex.
          throw new IllegalArgumentException(
                  "Programming error. The " + type + " type is not a directory or zip-file type."
          );
        }
        return inputSources.get(type)
                .stream()
                .map(t -> (CompositeDataSource)t)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Set<FileType> listInputFileTypes() {
        return inputSources.keySet();
    }

    @Override
    public DataSource getBaseGraph() {
        return baseGraph;
    }

    @Override
    public DataSource getGraph() {
        return graph;
    }

    @Override
    public JsonNode graphBuilderParameters() {
        return graphBuilderParameters;
    }

    @Override
    public JsonNode routerConfigParameters() {
        return routerConfigParameters;
    }

    /* private methods */


    private static DataSource source(File file, FileType type) {
        if(type.isCompositeInputDataFile()) {
            return compositeSource(file, type);
        }
        return new FileDataSource(file, type);
    }

    private static FileType resolveFileType(File file, GraphBuilderParameters buildConfig) {
        String name = file.getName();
        if (name.contains("gtfs")) { return GTFS; }
        if (name.contains("netex")) { return NETEX; }
        if (name.endsWith(".pbf")) { return OSM; }
        if (name.endsWith(".osm")) { return OSM; }
        if (name.endsWith(".osm.xml")) { return OSM; }
        if (name.endsWith(".tif") || name.endsWith(".tiff")) {
            // Digital elevation model (elevation raster)
            return DEM;
        }
        if (name.equals(GRAPH_FILENAME)) { return GRAPH; }
        if (name.equals(BASE_GRAPH_FILENAME)) { return PARTIAL_GRAPH; }
        if (isConfigFile(name)) { return CONFIG;}
        return UNRECOGNIZED;
    }

    private void initInputSourcesBasedOnBaseDirectory() {
        inputSources.clear();

        File[] files = baseDir.listFiles();

        if (files == null) {
            LOG.error("'{}' is not a readable directory.", baseDir);
            return;
        }

        LOG.info("Loading input files from {}", description());
        GraphBuilderParameters builderParams = new GraphBuilderParameters(graphBuilderParameters);

        for (File file : files) {
            FileType type = resolveFileType(file, builderParams);

            if (include(type, builderParams)) {
                inputSources.put(type, source(file, type));
            }
            else if(file.isDirectory()) {
                LOG.info("  Skipping directory: {}", file.getName());
            }
            else {
                LOG.info("  Skipping {} file: {}", type, file.getName());
            }
        }
        // Log all included files including config files(already loaded)
        // This is done here, and not in the above loop to list the
        // "Skipping" and "Found" files in 2 separate groups, which make it easier to read.
        for (FileType fileType : FileType.values()) {
            for (DataSource source : inputSources.get(fileType)) {
                LOG.info("  Found {} file: {}", source.type(), source.detailedInfo());
            }
        }
    }

    private static boolean include(FileType type, GraphBuilderParameters parameters) {
        switch (type) {
            // Load transit data if enabled in build config
            case GTFS:
            case NETEX:
                return parameters.transit;
            // Load OpenStreetMap data if enabled in build config
            case OSM:
                return parameters.streets;
            // Include Elevation data, Graphs and config - always
            case DEM:
            case GRAPH:
            case PARTIAL_GRAPH:
            case CONFIG:
                return true;
            // Skip all other files
            case UNRECOGNIZED:
                return false;
        }
        throw new IllegalArgumentException("Unhandled type: " + type);
    }
}
