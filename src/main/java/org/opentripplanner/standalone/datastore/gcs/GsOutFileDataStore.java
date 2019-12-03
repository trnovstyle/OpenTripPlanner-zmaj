package org.opentripplanner.standalone.datastore.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.file.DirectoryDataSource;
import org.opentripplanner.standalone.datastore.file.ZipFileDataSource;

import java.io.OutputStream;

import static java.nio.channels.Channels.newOutputStream;

class GsOutFileDataStore implements DataSource {

    private final Storage storage;
    private final String source;
    private final BlobId blobId;
    private final FileType type;

    /**
     * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files
     * as well as normal files. It does not handle directories({@link DirectoryDataSource}) or
     * zip-files {@link ZipFileDataSource} witch contain multiple files.
     */
    GsOutFileDataStore(Storage storage, BlobId blobId, FileType type, String source) {
        this.storage = storage;
        this.blobId = blobId;
        this.type = type;
        this.source = source;
    }

    @Override
    public String name() {
        return blobId.getName();
    }

    @Override
    public String path() {
        return source + ":" + name();
    }

    @Override
    public FileType type() {
        return type;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public OutputStream asOutputStream() {
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        return newOutputStream(storage.writer(blobInfo));
    }
}