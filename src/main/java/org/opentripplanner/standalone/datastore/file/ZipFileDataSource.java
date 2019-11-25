package org.opentripplanner.standalone.datastore.file;

import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * This is a wrapper around a ZipFile, it can be used to read the content, but
 * not write to it. The {@link #asOutputStream()} is throwing an exception.
 */
public class ZipFileDataSource implements CompositeDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(ZipFileDataSource.class);

    private final ZipFile zipFile;
    private final FileType type;
    private final long lastModified;
    private final Collection<DataSource> content;
    private boolean closed = false;

    ZipFileDataSource(File file, FileType type) {
        try {
            this.type = type;
            this.zipFile = new ZipFile(file, ZipFile.OPEN_READ);
            this.lastModified = file.lastModified();
            this.content = retrieveContent(zipFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            if(!closed) {
                // Only try to close the zip file once.
                closed = true;
                zipFile.close();
            }

        }
        catch (IOException e) {
            LOG.warn(path() + " close failed. Details: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * @return the internal zip file if still open. {@code null} is return if the file is closed.
     */
    ZipFile zipFile() {
        return closed ? null : zipFile;
    }

    @Override
    public String name() {
        return zipFile.getName();
    }

    @Override
    public String path() {
        return name();
    }

    @Override
    public FileType type() {
        return type;
    }

    @Override
    public long size() {
        return zipFile.size();
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public Collection<DataSource> content() {
        return content;
    }

    @Override
    public DataSource entry(String s) {
        return content.stream().filter(it -> it.name().equals(s)).findFirst().orElse(null);
    }

    private Collection<DataSource> retrieveContent(ZipFile zipFile) {
        Collection<DataSource> content = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            content.add(new ZipFileEntryDataSource(this, entry));
        }
        return content;
    }
}
