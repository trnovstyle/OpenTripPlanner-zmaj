package org.opentripplanner.standalone.datastore.gcs;

import com.google.cloud.storage.Blob;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.file.DirectoryDataSource;
import org.opentripplanner.standalone.datastore.file.ZipFileDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import static java.nio.channels.Channels.newInputStream;
import static java.nio.channels.Channels.newOutputStream;

/**
 * This class is a wrapper around and EXISTING Google Cloud Store bucket blob. It can
 * be read and overwritten.
 * <p>
 * Reading compressed blobs is supported. The only format supported is gzip (extension .gz).
 */
class GcsFileDataSource implements DataSource {

    private final String path;
    private final Blob file;
    private final FileType type;

    /**
     * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files
     * as well as normal files. It does not handle directories({@link DirectoryDataSource}) or
     * zip-files {@link ZipFileDataSource} witch contain multiple files.
     */
    GcsFileDataSource(Blob file, FileType type, String path) {
        this.file = file;
        this.type = type;
        this.path = path;
    }

    @Override
    public String name() {
        return file.getName();
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
        return file.getSize();
    }

    @Override
    public long lastModified() {
        return file.getUpdateTime();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public InputStream asInputStream() {
            // We support both gzip and unzipped files when reading.
            InputStream in = newInputStream(file.reader());

            if (file.getName().endsWith(".gz")) {
                try {
                    return new GZIPInputStream(in);
                }
                catch (IOException e) {
                    throw new IllegalStateException(e.getLocalizedMessage(), e);
                }
            }
            else {
                return in;
            }
    }

    @Override
    public OutputStream asOutputStream() {
        return newOutputStream(file.writer());
    }
}