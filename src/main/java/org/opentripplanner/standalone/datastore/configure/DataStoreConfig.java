package org.opentripplanner.standalone.datastore.configure;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.opentripplanner.standalone.datastore.file.DefaultDataStore;

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

    private boolean skipTransit = false;
    private File baseDirectory = null;


    public DataStoreConfig() { }


    /* static factory methods, mostly used by tests */

    /**
     * For test only.
     * <p>
     * Use this to get a composite data source, bypassing the {@link OtpDataStore}.
     */
    public static CompositeDataSource compositeSource(File file, FileType type) {
        return DefaultDataStore.compositeSource(file, type);
    }


    /* builder methods */

    /**
     * Use this to build a graph without transit data. The default is to include
     * transit data in the graph build process.
     */
    public DataStoreConfig withSkipTransit(boolean skipTransit) {
        this.skipTransit = skipTransit;
        return this;
    }

    /**
     * The base directory is used by the configuration loader to load all configuration.
     * It is also used by the default(only) {@link OtpDataStore} to load input data from the
     * same directory.
     */
    public DataStoreConfig withBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        return this;
    }


    /**
     * Connect to data source and prepare to retrieve data.
     */
    public OtpDataStore open() {
        JsonNode builderConfig = loadBuilderConfig(baseDirectory);

        // Load the router config JSON to fail fast, but we will only apply it later when
        // a router starts up
        JsonNode routerConfig = loadRouterConfig(baseDirectory);

        // Create the default data store and open it. If you implement your own data store,
        // this is where is should be injected.
        return new DefaultDataStore(baseDirectory, skipTransit, builderConfig, routerConfig).open();
    }
}
