package org.opentripplanner.standalone.datastore.file;

import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;


/**
 * This is a adapter to wrap a file directory and all files in it as a
 * composite data source. Sub-directories are ignored.
 */
public class DirectoryDataSource implements CompositeDataSource {
    private final File path;
    private final FileType type;
    private final Collection<DataSource> content = new ArrayList<>();

    @SuppressWarnings("ConstantConditions")
    DirectoryDataSource(File path, FileType type) {
        this.path = path;
        this.type = type;
        for (File file : path.listFiles()) {
            // Skip any nested directories
            if(file.isDirectory()) { continue; }
            // In general the file type at a child level is not used, but we tag
            // each file with the same type as the parent directory.
            // Examples: GTFS or NETEX.
            content.add(new FileDataSource(file, type));
        }
    }

    @Override
    public Collection<DataSource> content() {
        return content;
    }

    @Override
    public DataSource entry(String filename) {
        return content.stream().filter(it -> it.name().equals(filename)).findFirst().orElse(null);
    }

    @Override
    public String name() {
        return path.getName();
    }

    @Override
    public String path() {
        return path.getPath();
    }

    @Override
    public FileType type() {
        return type;
    }

    @Override
    public long lastModified() {
        return path.lastModified();
    }

    @Override
    public boolean exist() {
        return path.exists();
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
