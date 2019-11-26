package org.opentripplanner.standalone.datastore;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.standalone.config.StorageParameters;
import org.opentripplanner.standalone.datastore.base.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.opentripplanner.graph_builder.GraphBuilder.BASE_GRAPH_FILENAME;
import static org.opentripplanner.graph_builder.GraphBuilder.GRAPH_FILENAME;
import static org.opentripplanner.standalone.datastore.FileType.CONFIG;
import static org.opentripplanner.standalone.datastore.FileType.DEM;
import static org.opentripplanner.standalone.datastore.FileType.GRAPH;
import static org.opentripplanner.standalone.datastore.FileType.GTFS;
import static org.opentripplanner.standalone.datastore.FileType.NETEX;
import static org.opentripplanner.standalone.datastore.FileType.OSM;
import static org.opentripplanner.standalone.datastore.FileType.REPORT;
import static org.opentripplanner.standalone.datastore.FileType.UNKNOWN;

/**
 * The responsibility of this class is to provide access to all data sources OTP uses like the
 * graph, including OSM data and transit data. The default is to use the the local disk, but other
 * "providers/repositories" can be implemented to access files in the cloud (as an example).
 * <p>
 * This class provide an abstraction layer for accessing OTP data input and output sources.In a
 * cloud ecosystem you might find it easier to access the data directly from the cloud storage,
 * rather than first copy the data into your node local disk, and then copy the build graph back
 * into cloud storage after building it. Depending on the source this might also offer enhanced
 * performance.
 * <p>
 * Use the {@link org.opentripplanner.standalone.datastore.configure.DataStoreConfig} to obtain a
 * new instance of this class.
 * <p>
 * This class implement {@link Closeable} because it might need to access a remote resource. It
 * might keep a connection to the underlying service until the source is read. Make sure the {@link
 * #close()} method is called after all data is read. After the {@link #close()} method is called
 * the other methods behavior is undefined. This also propagate to all {@link DataSource} children.
 */
public class OtpDataStore implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(OtpDataStore.class);


    private static final String OTP_STATUS_IN_PROGRESS_FILE = "otp-status.inProgress";
    private static final String ANNOTATION_REPORT_DIR = "report";

    private final List<String> repositoryDescriptions = new ArrayList<>();
    private final StorageParameters parameters;
    private final JsonNode graphBuilderParameters;
    private final JsonNode routerConfigParameters;
    private final List<DataSourceRepository> repositories;
    private final Multimap<FileType, DataSource> sources = ArrayListMultimap.create();

    /* Named resources available for both reading and writing. */
    private DataSource baseGraph;
    private DataSource graph;
    private DataSource otpStatus;
    private CompositeDataSource buildReport;

    public OtpDataStore(
            StorageParameters parameters,
            JsonNode builderParams,
            JsonNode routerConfig,
            List<DataSourceRepository> repositories
    ) {
        this.repositoryDescriptions.addAll(
                repositories.stream()
                        .map(DataSourceRepository::description)
                        .collect(Collectors.toList())
        );
        this.parameters = parameters;
        this.graphBuilderParameters = builderParams;
        this.routerConfigParameters = routerConfig;
        this.repositories = repositories;
    }

    public void open() {
        repositories.forEach(DataSourceRepository::open);

        addAll(findMultipleSources(null, CONFIG));
        add(findSingleSource(parameters.osm, null, OSM));
        add(findSingleSource(parameters.dem, null, DEM));
        addAll(findMultipleSources(parameters.gtfs, GTFS));
        addAll(findMultipleSources(parameters.netex, NETEX));

        baseGraph = findSingleSource(parameters.baseGraph, BASE_GRAPH_FILENAME, GRAPH);
        graph = findSingleSource(parameters.graph, GRAPH_FILENAME, GRAPH);
        otpStatus = findSingleSource(parameters.otpStatus, OTP_STATUS_IN_PROGRESS_FILE, GRAPH);
        buildReport = (CompositeDataSource) findSingleSource(
                parameters.buildReport, ANNOTATION_REPORT_DIR, REPORT
        );
        addAll(Arrays.asList(baseGraph, graph, otpStatus, buildReport));

        // Also read in unknown sources in case the data input source is miss-spelled,
        // We look for files on the local-file-system, other repositories ignore this call.
        addAll(findMultipleSources(null, UNKNOWN));
    }

    /**
     * @return a description(path) for each datasource used/enabled.
     */
    public List<String> getRepositoryDescriptions() {
        return repositoryDescriptions;
    }

    /**
     * List all existing data sources by file type. An empty list is returned if there is no files
     * of the given type.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     *
     * @return a collection of {@link DataSource} or {@link CompositeDataSource}. If the type is a
     * {@link FileType#isCompositeInputDataFile()} then it is safe to cast the elements to the sub
     * type.
     */
    @NotNull
    public Collection<DataSource> listExistingSourcesFor(FileType type) {
        return sources.get(type).stream().filter(DataSource::exists).collect(Collectors.toList());
    }

    /**
     * @return the graph builder parameters loaded from the configuration directory.
     */
    @NotNull
    public JsonNode graphBuilderParameters() {
        return graphBuilderParameters;
    }

    /**
     * @return the router config parameters loaded from the configuration directory.
     */
    @NotNull
    public JsonNode routerConfigParameters() {
        return routerConfigParameters;
    }

    /**
     * Return a list of repositories used to access data sources.
     */
    @NotNull
    public List<String> repositoryDescriptions() {
        return repositoryDescriptions;
    }

    @Override
    public void close() {
        for (DataSourceRepository it : repositories) {
            try {
                it.close();
            }
            catch (IOException e) {
                LOG.error("Failed to close repository: " + it.description());
            }
        }
    }

    @NotNull
    public DataSource getBaseGraph() {
        return baseGraph;
    }

    @NotNull
    public DataSource getGraph() {
        return graph;
    }

    @NotNull
    public DataSource getOtpStatus() {
        return otpStatus;
    }

    @NotNull
    public CompositeDataSource getBuildReport() {
        return buildReport;
    }

    /* private methods */

    /**
     * Add source to internal list of sources
     */
    private void add(DataSource source) {
        if(source != null) {
            sources.put(source.type(), source);
        }
    }

    /**
     * Add all sources to internal list of sources
     */
    private void addAll(List<DataSource> list) {
        list.forEach(this::add);
    }

    private DataSource findSingleSource(URI uri, String filename, FileType type) {
            return uri == null ? findSourceInRepo(filename, type) : findSourceInRepo(uri, type);
    }

    private List<DataSource> findMultipleSources(Collection<URI> uris, FileType type) {
        if(uris == null || uris.isEmpty()) {
            return  findSourcesInRepo(type);
        }
        else {
            return uris.stream()
                    .map(uti -> findSourceInRepo(uti, type))
                    .collect(Collectors.toList());
        }
    }

    private DataSource findSourceInRepo(URI uri, FileType type) {
        for (DataSourceRepository it : repositories) {
            DataSource res = it.findSource(uri, type);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    private DataSource findSourceInRepo(String filename, FileType type) {
        for (DataSourceRepository it : repositories) {
            DataSource res = it.findSource(filename, type);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    private List<DataSource> findSourcesInRepo(FileType type) {
        for (DataSourceRepository it : repositories) {
            List<DataSource> res = it.listSources(type);
            if (!res.isEmpty()) {
                return res;
            }
        }
        return Collections.emptyList();
    }
}
