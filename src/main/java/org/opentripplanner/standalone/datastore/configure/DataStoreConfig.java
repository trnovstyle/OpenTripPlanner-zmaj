package org.opentripplanner.standalone.datastore.configure;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.standalone.config.GraphBuilderParameters;
import org.opentripplanner.standalone.config.StorageParameters;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.opentripplanner.standalone.datastore.base.DataSourceRepository;
import org.opentripplanner.standalone.datastore.file.FileDataSourceRepository;
import org.opentripplanner.standalone.datastore.gs.GsDataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.opentripplanner.standalone.datastore.file.ConfigLoader.loadBuilderConfig;
import static org.opentripplanner.standalone.datastore.file.ConfigLoader.loadRouterConfig;

/**
 * This is the global access point to create a data store and create datasource objects(tests). It
 * uses a build pattern to configure the data store before creating it.
 * <p>
 * Note that opening a data store should not download or open any data sources, only fetch meta-data
 * to figure out what data is available. A data source is accessed (lazy) using streams.
 * <p>
 * The only available data store is using the local file system to fetch data, but it is designed so
 * individual forks of OTP can provide their own implementation to fetch data from the cloud, mixed
 * with file access.
 * <p>
 * Implementation details. This class should contain minimal amount of business logic, delegating
 * all tasks to the underlying implementations.
 */
public class DataStoreConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DataStoreConfig.class);

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
        return FileDataSourceRepository.compositeSource(file, type);
    }

    /**
     * Connect to data source and prepare to retrieve data.
     */
    public OtpDataStore open() {
        JsonNode builderConfig = loadBuilderConfig(baseDirectory);

        // Load the router config JSON to fail fast, but we will only apply it later when
        // a router starts up
        JsonNode routerConfig = loadRouterConfig(baseDirectory);

        GraphBuilderParameters config = new GraphBuilderParameters(builderConfig);
        StorageParameters storageConfig = config.storage;

        List<DataSourceRepository> repositories = new ArrayList<>();

        // Adding Google Cloud Storage, if the config file contains URIs with prefix "gs:"
        if (storageConfig.isGoogleCloudStorageEnabled()) {
            LOG.info("Google Cloud Store Repository enabled - GCS resources detected.");
            repositories.add(new GsDataSourceRepository(storageConfig.gsCredentials));
        }
        // The file data storage repository should be last, to allow
        // other repositories to "override" and grab files analyzing the
        // datasource uri passed in
        repositories.add(new FileDataSourceRepository(baseDirectory));

        OtpDataStore store = new OtpDataStore(
                storageConfig,
                builderConfig,
                routerConfig,
                repositories
        );

        store.open();
        return store;
    }
}
