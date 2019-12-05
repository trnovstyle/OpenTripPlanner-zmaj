package org.opentripplanner.standalone.datastore;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.opentripplanner.standalone.config.StorageParameters;
import org.opentripplanner.standalone.datastore.base.DataSourceRepository;
import org.opentripplanner.standalone.datastore.base.LocalDataSourceRepository;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.opentripplanner.graph_builder.GraphBuilder.BASE_GRAPH_FILENAME;
import static org.opentripplanner.graph_builder.GraphBuilder.GRAPH_FILENAME;
import static org.opentripplanner.standalone.datastore.FileType.CONFIG;
import static org.opentripplanner.standalone.datastore.FileType.DEM;
import static org.opentripplanner.standalone.datastore.FileType.GRAPH;
import static org.opentripplanner.standalone.datastore.FileType.GTFS;
import static org.opentripplanner.standalone.datastore.FileType.NETEX;
import static org.opentripplanner.standalone.datastore.FileType.OSM;
import static org.opentripplanner.standalone.datastore.FileType.OTP_STATUS;
import static org.opentripplanner.standalone.datastore.FileType.REPORT;
import static org.opentripplanner.standalone.datastore.FileType.UNKNOWN;
import static org.opentripplanner.standalone.datastore.base.LocalDataSourceRepository.CURRENT_DIRECTORY;

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
 */
public class OtpDataStore {
    public static final String BUILD_REPORT_DIR = "report";

    private final List<String> repositoryDescriptions = new ArrayList<>();
    private final StorageParameters parameters;
    private final JsonNode graphBuilderParameters;
    private final JsonNode routerConfigParameters;
    private final List<DataSourceRepository> allRepositories;
    private final LocalDataSourceRepository localRepository;
    private final Multimap<FileType, DataSource> sources = ArrayListMultimap.create();

    /* Named resources available for both reading and writing. */
    private DataSource baseGraph;
    private DataSource graph;
    private CompositeDataSource otpStatusDir;
    private CompositeDataSource buildReportDir;

    /**
     * Use the {@link org.opentripplanner.standalone.datastore.configure.DataStoreConfig} to
     * create a new instance of this class.
     */
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
        this.allRepositories = repositories;
        this.localRepository = getLocalDataSourceRepo(repositories);
    }

    public void open() {
        allRepositories.forEach(DataSourceRepository::open);

        addAll(findMultipleSources(Collections.emptyList(), CONFIG));
        addAll(findMultipleSources(parameters.osm, OSM));
        addAll(findMultipleSources(parameters.dem,  DEM));
        addAll(findMultipleSources(parameters.gtfs, GTFS));
        addAll(findMultipleSources(parameters.netex, NETEX));

        baseGraph = findSingleSource(parameters.baseGraph, BASE_GRAPH_FILENAME, GRAPH);
        graph = findSingleSource(parameters.graph, GRAPH_FILENAME, GRAPH);
        otpStatusDir = findCompositeSource(parameters.otpStatusDir, CURRENT_DIRECTORY, OTP_STATUS);
        buildReportDir = findCompositeSource(parameters.buildReportDir, BUILD_REPORT_DIR, REPORT);
        addAll(Arrays.asList(baseGraph, graph, otpStatusDir, buildReportDir));

        // Also read in unknown sources in case the data input source is miss-spelled,
        // We look for files on the local-file-system, other repositories ignore this call.
        addAll(findMultipleSources(Collections.emptyList(), UNKNOWN));
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
     * @return The collection may contain elements of type {@link DataSource} or
     * {@link CompositeDataSource}.
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

    @NotNull
    public DataSource getBaseGraph() {
        return baseGraph;
    }

    @NotNull
    public DataSource getGraph() {
        return graph;
    }

    @NotNull
    public CompositeDataSource getOtpStatusDir() {
        return otpStatusDir;
    }

    @NotNull
    public CompositeDataSource getBuildReportDir() {
        return buildReportDir;
    }


    /* private methods */

    private void add(DataSource source) {
        if(source != null) {
            sources.put(source.type(), source);
        }
    }

    private void addAll(List<DataSource> list) {
        list.forEach(this::add);
    }

    private LocalDataSourceRepository getLocalDataSourceRepo(List<DataSourceRepository> repositories) {
        List<LocalDataSourceRepository> localRepos = repositories
                .stream()
                .filter(it -> it instanceof LocalDataSourceRepository)
                .map(it -> (LocalDataSourceRepository)it)
                .collect(Collectors.toList());
        if(localRepos.size() != 1) {
            throw new IllegalStateException("Only one LocalDataSourceRepository is supported.");
        }
        return localRepos.get(0);
    }

    private CompositeDataSource findCompositeSource(@Nullable URI uri, @NotNull String filename, @NotNull FileType type) {
        if(uri != null) {
            return findSourceByUri(it -> it.findCompositeSource(uri, type));
        }
        else {
            return localRepository.findCompositeSource(filename, type);
        }
    }

    private DataSource findSingleSource(@Nullable URI uri, @NotNull String filename, @NotNull FileType type) {
        if(uri != null) {
            return findSourceByUri(uri, type);
        }
        return localRepository.findSource(filename, type);
    }

    private List<DataSource> findMultipleSources(@NotNull Collection<URI> uris, @NotNull FileType type) {
        if(uris.isEmpty()) {
            return localRepository.listExistingSources(type);
        }
        else {
            return uris.stream()
                    .map(uri -> findSourceByUri(uri, type))
                    .collect(Collectors.toList());
        }
    }

    private DataSource findSourceByUri(@NotNull URI uri, @NotNull FileType type) {
        return findSourceByUri(it -> it.findSource(uri, type));
    }

    private <T> T findSourceByUri(Function<DataSourceRepository, T> repoFindSource) {
        for (DataSourceRepository it : allRepositories) {
            T res = repoFindSource.apply(it);
            if (res != null) {
                return res;
            }
        }
        return null;
    }
}
