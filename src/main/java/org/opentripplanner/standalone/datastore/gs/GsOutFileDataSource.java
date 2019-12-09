package org.opentripplanner.standalone.datastore.gs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.base.MimeTypes;
import org.opentripplanner.standalone.datastore.file.DirectoryDataSource;
import org.opentripplanner.standalone.datastore.file.ZipFileDataSource;

import java.io.OutputStream;

import static java.nio.channels.Channels.newOutputStream;

class GsOutFileDataSource extends AbstractGsDataSource implements DataSource {
    private final Storage storage;

    /**
     * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files
     * as well as normal files. It does not handle directories({@link DirectoryDataSource}) or
     * zip-files {@link ZipFileDataSource} witch contain multiple files.
     */
    GsOutFileDataSource(Storage storage, BlobId blobId, FileType type) {
        super(blobId, type);
        this.storage = storage;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public OutputStream asOutputStream() {
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId());

        String mimeType = MimeTypes.mimeType(name());
        if(mimeType != null) {
            builder.setContentType(mimeType);
        }
        return newOutputStream(storage.writer(builder.build()));
    }
}