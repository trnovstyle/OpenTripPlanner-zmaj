package org.opentripplanner.standalone.datastore.file;


import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.base.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
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
import static org.opentripplanner.standalone.datastore.FileType.OTP_STATUS;
import static org.opentripplanner.standalone.datastore.FileType.REPORT;
import static org.opentripplanner.standalone.datastore.FileType.UNKNOWN;
import static org.opentripplanner.standalone.datastore.OtpDataStore.BUILD_REPORT_DIR;
import static org.opentripplanner.standalone.datastore.OtpDataStore.OTP_STATUS_FILENAME;
import static org.opentripplanner.standalone.datastore.file.ConfigLoader.isConfigFile;


/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class FileDataSourceRepository implements DataSourceRepository {
    private static final Logger LOG = LoggerFactory.getLogger(FileDataSourceRepository.class);

    private final File baseDir;
    private final List<AbstractFileDataSource> existingFiles = new ArrayList<>();

    public FileDataSourceRepository(File baseDir) {
        this.baseDir = baseDir;
    }

    @NotNull
    public static CompositeDataSource compositeSource(File file, FileType type) {
        // The cast is safe
        return (CompositeDataSource) createCompositeSource(file, type);
    }

    @Override
    public String description() {
        return baseDir.getPath();
    }

    @Override
    public void open() {
        initExistingFiles();
    }

    @Override
    public DataSource findSource(URI uri, FileType type) {
        return findOrCreateSource(new File(uri), type);
    }

    @Override
    public DataSource findSource(String filename, FileType type) {
        return findOrCreateSource(filename, type);
    }

    @Override
    public List<DataSource> listSources(FileType type) {
        // Return ALL resources of the given type, this is
        // auto-detecting matching files on the local file system
        return existingFiles
                .stream()
                .filter(it -> it.type == type)
                .collect(Collectors.toUnmodifiableList());
    }


    /* private methods */

    private void initExistingFiles() {
        File[] files = baseDir.listFiles();

        if (files == null) {
            LOG.error("'{}' is not a readable input directory.", baseDir);
            return;
        }

        for (File file : files) {
            FileType type = resolveFileType(file.getName());
            existingFiles.add(createSource(file, type));
        }
    }

    private AbstractFileDataSource findOrCreateSource(String filename, FileType type) {
        return findOrCreateSource(new File(baseDir, filename), type);
    }

    private AbstractFileDataSource findOrCreateSource(File file, FileType type) {
        // Lookup existing objects and return if found
        for (AbstractFileDataSource it : existingFiles) {
            if(it.type == type && it.sameAs(file)) {
                return it;
            }
        }
        // Create a new one
        return createSource(file, type);
    }

    private AbstractFileDataSource createSource(File file, FileType type) {
        if (type.isCompositeInputDataFile()) {
            return createCompositeSource(file, type);
        }
        return new FileDataSource(file, type);
    }

    private static AbstractFileDataSource createCompositeSource(File file, FileType type) {
        if (file.isDirectory() || type == REPORT) {
            return new DirectoryDataSource(file, type);
        }
        if (file.getName().endsWith(".zip")) {
            return new ZipFileDataSource(file, type);
        }
        throw new IllegalArgumentException("The " + file + " is not recognized as a zip-file or "
                + "directory. Unable to create composite data source for file type " + type + ".");
    }

    private static FileType resolveFileType(String name) {
        if (name.toLowerCase().contains("gtfs")) { return GTFS; }
        if (name.toLowerCase().contains("netex")) { return NETEX; }
        if (name.endsWith(".pbf")) { return OSM; }
        if (name.endsWith(".osm")) { return OSM; }
        if (name.endsWith(".osm.xml")) { return OSM; }
        // Digital elevation model (elevation raster)
        if (name.endsWith(".tif") || name.endsWith(".tiff")) { return DEM; }
        if (name.equals(GRAPH_FILENAME)) { return GRAPH; }
        if (name.equals(BASE_GRAPH_FILENAME)) { return GRAPH; }
        if (name.equals(OTP_STATUS_FILENAME)) { return OTP_STATUS; }
        if (name.equals(BUILD_REPORT_DIR)) { return REPORT; }
        if (isConfigFile(name)) { return CONFIG;}
        return UNKNOWN;
    }
}
