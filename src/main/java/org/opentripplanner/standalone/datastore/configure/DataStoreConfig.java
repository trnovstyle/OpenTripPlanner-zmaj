package org.opentripplanner.standalone.datastore.configure;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.standalone.config.GraphBuilderParameters;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.opentripplanner.standalone.datastore.file.DefaultDataStore;
import org.opentripplanner.standalone.datastore.generic.AbstractDataStore;

import java.io.File;

import static org.opentripplanner.standalone.datastore.file.ConfigLoader.loadBuilderConfig;
import static org.opentripplanner.standalone.datastore.file.ConfigLoader.loadRouterConfig;

/**
 * This is the global access point to create a data store and create datasource objects(tests). It
 * uses a build pattern to configure the data store before creating it.
 * <p>
 * Note that opening a data store should not download or open any data sources, only fetch
 * meta-data to figure out what data is available. A data source is accessed (lazy) using streams.
 * <p>
 * The only available data store is using the local file system to fetch data, but it is designed
 * so individual forks of OTP can provide their own implementation to fetch data from the cloud,
 * mixed with file access.
 * <p>
 * Implementation details. This class should contain minimal amount of business logic, delegating
 * all tasks to the underlying implementations.
 */
public class DataStoreConfig {

    private final File baseDirectory;

    /**
     * @param baseDirectory is used by the configuration loader to load all configuration files. It
     *                      is also used by the default {@link OtpDataStore} to load data from the
     *                      same directory.
     */
    public DataStoreConfig(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    /* static factory methods, mostly used by tests */

    /**
     * For test only.
     * <p>
     * Use this to get a composite data source, bypassing the {@link OtpDataStore}.
     */
    public static CompositeDataSource compositeSource(File file, FileType type) {
        return DefaultDataStore.compositeSource(file, type);
    }

    /**
     * Connect to data source and prepare to retrieve data.
     */
    public OtpDataStore open() {
        JsonNode builderConfig = loadBuilderConfig(baseDirectory);

        // Load the router config JSON to fail fast, but we will only apply it later when
        // a router starts up
        JsonNode routerConfig = loadRouterConfig(baseDirectory);

        AbstractDataStore store = createDataStore(builderConfig, routerConfig);

        store.open();

        return store;
    }

    private AbstractDataStore createDataStore(JsonNode builderConfig, JsonNode routerConfig) {
        GraphBuilderParameters config = new GraphBuilderParameters(builderConfig);
        // GoogleCloudStorageParameters gcsConfig = config.googleCloudStorage;

        // If you implement your own data store, this is how to inject it.
        // if (GcsDataStore.isEnabled(gcsConfig)) {
        //     return new GcsDataStore(gcsConfig, builderConfig, routerConfig);
        // }
        // Create the default data store
        return new DefaultDataStore(baseDirectory, builderConfig, routerConfig);
    }
}
