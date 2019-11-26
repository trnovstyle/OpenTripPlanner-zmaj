package org.opentripplanner.standalone.datastore.base;

import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * This data source keep its data in memory as a byte array. You may insert data using
 * {@link #withBytes(byte[])} or {@link #asOutputStream()}. To access data you can use the
 * {@link #getBytes()} or reading the input stream {@link #asInputStream()}.
 * <p>
 * Any existing data in the datasource will be erased if you insert data using the output stream
 * {@link #asOutputStream()} or set the byte array {@link #withBytes(byte[])}.
 */
public class ByteArrayDataSource implements DataSource {

    private final String path;
    private final String name;
    private final FileType type;
    private final long size;
    private final long lastModified;
    private ByteArrayOutputStream out = null;
    private byte[] buffer;


    public ByteArrayDataSource(String path, String name, FileType type) {
        this(path, name, type, -1, -1);
    }

    public ByteArrayDataSource(
            String path, String name, FileType type, long size, long lastModified
    ) {
        this.path = path;
        this.name = name;
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
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

    /**
     * Return the internal byte array as an {@link InputStream}. A new input stream is generated for
     * each call to this method, and it is safe to do so.
     */
    @Override
    public InputStream asInputStream() {
        return new ByteArrayInputStream(getBytes());
    }

    /**
     * Clean any existing data, and return a new {@link OutputStream} witch can be used to insert data
     * into the byte array.
     */
    @Override
    public OutputStream asOutputStream() {
        if (out == null) {
            out = new ByteArrayOutputStream(1024);
            buffer = null;
        }
        return out;
    }

    public byte[] getBytes() {
        return buffer == null ? out.toByteArray() : buffer;
    }

    /**
     * Clean any existing data, and set the byte array.
     */
    public ByteArrayDataSource withBytes(byte[] buffer) {
        this.buffer = buffer;
        this.out = null;
        return this;
    }
}
