package org.opentripplanner.standalone.datastore.generic;

import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteArrayDataSource implements DataSource {
    private final String path;
    private final String name;
    private final FileType type;
    private final long size;
    private final long lastModified;
    private final ByteArrayInputStream inputStream;

    public ByteArrayDataSource(
            String path,
            String name,
            FileType type,
            long size,
            long lastModified,
            byte[] buffer
    ) {
        this.path = path;
        this.name = name;
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
        this.inputStream = new ByteArrayInputStream(buffer);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public FileType type() {
        return type;
    }

    @Override
    public long size() {
        return size;
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
    public InputStream asInputStream() {
        return inputStream;
    }
}
