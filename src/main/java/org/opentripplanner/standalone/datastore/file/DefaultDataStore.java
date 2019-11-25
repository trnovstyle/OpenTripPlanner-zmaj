package org.opentripplanner.standalone.datastore.file;


import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.generic.AbstractDataStore;

import java.io.File;
import java.util.Arrays;


/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class DefaultDataStore extends AbstractDataStore<File> {

    private final File baseDir;

    public DefaultDataStore(File baseDir, JsonNode builderParams, JsonNode routerConfig) {
        super(builderParams, routerConfig);
        this.baseDir = baseDir;
    }

    public static CompositeDataSource compositeSource(File file, FileType type) {
        if (file.isDirectory()) {
            return new DirectoryDataSource(file, type);
        }
        if (file.getName().endsWith(".zip")) {
            return new ZipFileDataSource(file, type);
        }
        throw new IllegalArgumentException("The " + file + " is not recognized as a zip-file or "
                + "directory. Unable to create composite data source for file type " + type + ".");
    }

    @Override
    protected void openStorage() { /* Nothing to do */ }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected Iterable<File> listEntries() {
        return Arrays.asList(baseDir.listFiles());
    }

    @Override
    protected String name(File file) {
        return file.getName();
    }

    @Override
    public DataSource createSource(String filename, FileType type) {
        return createSource(new File(baseDir, filename), type);
    }

    @Override
    protected DataSource createSource(File file, FileType type) {
        if (type.isCompositeInputDataFile()) {
            return compositeSource(file, type);
        }
        return new FileDataSource(file, type);
    }

    @Override
    public String path() {
        return baseDir.getPath();
    }

    @Override
    public void close() { /* No need to close anything when accessing local file system. */ }
}
