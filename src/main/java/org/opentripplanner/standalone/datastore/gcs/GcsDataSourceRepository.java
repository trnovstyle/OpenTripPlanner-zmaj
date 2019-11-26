package org.opentripplanner.standalone.datastore.gcs;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.base.DataSourceRepository;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class GcsDataSourceRepository implements DataSourceRepository {
    private final String credentialsFilename;
    private Storage storage;

    public GcsDataSourceRepository(String credentialsFilename) {
        this.credentialsFilename = credentialsFilename;
    }

    @Override
    public void open() {
        this.storage = connectToStorage();
    }

    @Override
    public String description() {
        return "Google Cloud Storage";
    }

    @Override
    public DataSource findSource(URI uri, FileType type) {
        if(!isGoogleCloudStorageUri(uri)) {
            return null;
        }

        BlobPath path = BlobPath.parse(uri);

        if(path == null) {
            return null;
        }
        return createSource(path, type);
    }

    @Override
    public void close() { /* No need to close anything when accessing local file system. */ }


    /* private methods */

    private static boolean isGoogleCloudStorageUri(URI uri) {
        return "gs".equals(uri.getScheme());
    }

    private DataSource createSource(BlobPath path, FileType type) {
        Bucket bucket = storage.get(path.bucket);

        Blob blob = bucket.get(path.objectName);

        if(blob != null) {
            if(blob.getName().endsWith(".zip")) {
                return new GcsZipFileDataSource(blob, type, path.toString());
            }
            return new GcsFileDataSource(blob, type, path.toString());
        }

        if(type.isCompositeInputDataFile()) {
            return new GcsDirectoryDataSource(storage, path, type);
        }

        BlobId blobId = BlobId.of(bucket.getName(), path.objectName);
        return new GcsOutFileDataStore(storage, blobId, type, path.toString());
    }

    private Storage connectToStorage() {
        try {
            StorageOptions.Builder builder = StorageOptions.getDefaultInstance().toBuilder();

            if(credentialsFilename != null) {
                GoogleCredentials credentials = GoogleCredentials
                        .fromStream(new FileInputStream(credentialsFilename))
                        .createScoped(Collections.singletonList(
                                "https://www.googleapis.com/auth/cloud-platform"));
                builder.setCredentials(credentials);
            }
            return builder.build().getService();
        }
        catch (IOException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }
}
