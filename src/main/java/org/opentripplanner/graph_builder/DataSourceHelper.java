package org.opentripplanner.graph_builder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.OtpDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataSourceHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceHelper.class);

    private final OtpDataStore store;
    private final Multimap<FileType, DataSource> dataInput = ArrayListMultimap.create();
    private final Multimap<FileType, DataSource> unknown = ArrayListMultimap.create();

    DataSourceHelper(OtpDataStore store) {
        this.store = store;
    }

    DataSourceHelper findFilesToImport() {
        for (FileType type : FileType.values()) {
            if (type != FileType.UNKNOWN) {
                dataInput.putAll(type, store.listExistingSourcesFor(type));
            }
            else {
                unknown.putAll(type, store.listExistingSourcesFor(type));
            }
        }
        return this;
    }

    DataSourceHelper logSkippedAndSelectedFiles() {
        LOG.info("Loading files from: {}", String.join(", ", store.getRepositoryDescriptions()));

        // Sort data input files by type
        for (FileType type : FileType.values()) {
            for (DataSource source : dataInput.get(type)) {
                if (type == FileType.CONFIG) {
                    log("%s loaded", source);
                }
                else {
                    log("Found %s", source);
                }
            }
        }
        for (FileType type : FileType.values()) {
            for (DataSource source : unknown.get(type)) {
                log("Skipping %s", source);
            }
        }
        return this;
    }

    private void log(String op, DataSource source) {
        String opTxt = String.format(op, source.type().text());
        LOG.info("  - {} {}", opTxt, source.detailedInfo());
    }

    Multimap<FileType, DataSource> get() {
        return dataInput;
    }
}
