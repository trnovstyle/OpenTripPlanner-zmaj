package org.opentripplanner.standalone.datastore.file;


import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.base.LocalDataSourceRepository;
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
import static org.opentripplanner.standalone.datastore.base.LocalDataSourceRepository.isCurrentDir;
import static org.opentripplanner.standalone.datastore.file.ConfigLoader.isConfigFile;


/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class FileDataSourceRepository implements LocalDataSourceRepository {
    private static final String OTP_STATUS_FILENAME = "otp-status";
    private static final Logger LOG = LoggerFactory.getLogger(FileDataSourceRepository.class);

    private final File baseDir;

    public FileDataSourceRepository(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Use for unit testing
     */
    @NotNull
    public static CompositeDataSource compositeSource(File file, FileType type) {
        // The cast is safe
        return createCompositeSource(file, type);
    }

    @Override
    public String description() {
        return baseDir.getPath();
    }

    @Override
    public void open() { /* Nothing to do */ }

    @Override
    public DataSource findSource(URI uri, FileType type) {
        return new FileDataSource(new File(uri), type);
    }

    @Override
    public DataSource findSource(String filename, FileType type) {
        return new FileDataSource(new File(baseDir, filename), type);
    }

    @Override
    public CompositeDataSource findCompositeSource(URI uri, FileType type) {
        return createCompositeSource(new File(uri), type);
    }

    @Override
    public CompositeDataSource findCompositeSource(String localFilename, FileType type) {
        File file = isCurrentDir(localFilename) ? baseDir : new File(baseDir, localFilename);
        return createCompositeSource(file, type);
    }

    @Override
    public List<DataSource> listExistingSources(FileType type) {
        // Return ALL resources of the given type, this is
        // auto-detecting matching files on the local file system
        return existingFiles()
                .stream()
                .filter(it -> it.type() == type)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String toString() {
        return "FileDataSourceRepository{" + "baseDir=" + baseDir + '}';
    }

    /* private methods */

    private List<DataSource> existingFiles() {
        List<DataSource> existingFiles = new ArrayList<>();
        File[] files = baseDir.listFiles();

        if (files == null) {
            LOG.error("'{}' is not a readable input directory.", baseDir);
            return existingFiles;
        }

        for (File file : files) {
            FileType type = resolveFileType(file.getName());
            if(isCompositeDataSource(file)) {
                existingFiles.add(createCompositeSource(file, type));
            }
            else {
                existingFiles.add(new FileDataSource(file, type));
            }
        }
        return existingFiles;
    }

    private boolean isCompositeDataSource(File file) {
       return file.isDirectory() || file.getName().endsWith(".zip");
    }

    private static CompositeDataSource createCompositeSource(File file, FileType type) {
        if (file.exists() && file.isDirectory()) {
            return new DirectoryDataSource(file, type);
        }
        if (file.getName().endsWith(".zip")) {
            return new ZipFileDataSource(file, type);
        }
        // If writing to a none-existing directory
        if (!file.exists() && type.isOutputDataSource()) {
            return new DirectoryDataSource(file, type);
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
        if (name.startsWith(OTP_STATUS_FILENAME)) { return OTP_STATUS; }
        if (name.equals(BUILD_REPORT_DIR)) { return REPORT; }
        if (isConfigFile(name)) { return CONFIG;}
        return UNKNOWN;
    }
}
