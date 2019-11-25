package org.opentripplanner.graph_builder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.standalone.config.GraphBuilderParameters;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class InputDataSourceSelection {
    private static final Logger LOG = LoggerFactory.getLogger(InputDataSourceSelection.class);

    private final Multimap<FileType, DataSource> selectedFilesToImport = ArrayListMultimap.create();
    private final List<DataSource> skippedFiles = new ArrayList<>();
    private final OtpDataStore store;
    private final GraphBuilderParameters parameters;

    InputDataSourceSelection(
            OtpDataStore store, GraphBuilderParameters builderParams
    ) {
        this.store = store;
        this.parameters = builderParams;
    }

    InputDataSourceSelection selectFilesToImport() {

        for (FileType type : FileType.values()) {
            if (include(type, parameters)) {
                selectedFilesToImport.putAll(type, store.listExistingSourcesFor(type));
            }
            else {
                skippedFiles.addAll(store.listExistingSourcesFor(type));
            }
        }
        return this;
    }

    InputDataSourceSelection logSkippedAndSelectedFiles() {
        LOG.info("Loading input files from {}", store.path());

        for (DataSource it : skippedFiles) {
            LOG.info("  Skipping {} file: {}", it.type(), it.name());
        }
        for (DataSource it : selectedFilesToImport.values()) {
            LOG.info("  Found {} file: {}", it.type(), it.name());
        }
        return this;
    }

    Multimap<FileType, DataSource> andGetFilesToImport() {
        return selectedFilesToImport;
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
