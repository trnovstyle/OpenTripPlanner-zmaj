package org.opentripplanner.standalone.datastore.generic;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.opentripplanner.graph_builder.GraphBuilder.BASE_GRAPH_FILENAME;
import static org.opentripplanner.graph_builder.GraphBuilder.GRAPH_FILENAME;
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
public abstract class AbstractDataStore<T> implements OtpDataStore {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataStore.class);

    private final JsonNode graphBuilderParameters;
    private final JsonNode routerConfigParameters;


    private final Multimap<FileType, DataSource> sources = ArrayListMultimap.create();

    public AbstractDataStore(JsonNode builderParams, JsonNode routerConfig) {
        this.graphBuilderParameters = builderParams;
        this.routerConfigParameters = routerConfig;
    }

    /* methods to implement */

    /**
     * Open or connect to storage. This method is called once, and the session is
     * terminated calling the {@link #close()} method.
     */
    protected abstract void openStorage();

    /**
     * List all available entries in the storage.
     */
    protected abstract Iterable<T> listEntries();

    /**
     * Resolve the name to use for a given entry. Should be a short name including extension. The
     * name is used to resolve the {@link FileType}, see {@link AbstractDataStore#resolveFileType(String)}
     * <p>
     * Examples:
     * <p>
     * {@code gtfs.zip, norway.osm.pbf}
     */
    protected abstract String name(T entry);

    /**
     * Create a data source wrapper for the given {@code entryName} and {@code type}.
     * Composite types must extend the {@link CompositeDataSource} interface, even
     * if the return type is just {@link DataSource}.
     */
    protected abstract DataSource createSource(String entryName, FileType type);

    /**
     * Create a data source wrapper for the given {@code entry} and {@code type}.
     * Composite types must extend the {@link CompositeDataSource} interface, even
     * if the return type is just {@link DataSource}.
     */
    protected abstract DataSource createSource(T entry, FileType type);

    /**
     * Open the datasource for reading, retrieving metadata like a ist of all dataSources in the
     * store.
     */
    public OtpDataStore open() {
        openStorage();
        getMetadataAndInitListOfSources();
        return this;
    }

    private void getMetadataAndInitListOfSources() {
        sources.clear();

        Iterable<T> entries = listEntries();

        if (entries == null) {
            LOG.error("'{}' is not a readable input directory.", path());
            return;
        }

        for (T entry : entries) {
            FileType type = resolveFileType(name(entry));
            sources.put(type, createSource(entry, type));
        }
    }

    @Override
    public final DataSource getSource(String sourceName, FileType type) {
        for (DataSource source : listExistingSourcesFor(type)) {
            if(source.name().equals(sourceName)) {
                return source;
            }
        }
        return createSource(sourceName, type);
    }

    @Override
    public final Collection<DataSource> listExistingSourcesFor(FileType type) {
        return sources.get(type);
    }

    @Override
    public final JsonNode graphBuilderParameters() {
        return graphBuilderParameters;
    }

    @Override
    public final JsonNode routerConfigParameters() {
        return routerConfigParameters;
    }


    /* private methods */

    private static FileType resolveFileType(String name) {
        if (name.toLowerCase().contains("gtfs")) { return GTFS; }
        if (name.toLowerCase().contains("netex")) { return NETEX; }
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
}
