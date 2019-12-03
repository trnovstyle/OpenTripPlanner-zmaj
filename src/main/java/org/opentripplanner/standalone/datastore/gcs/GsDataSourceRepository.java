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
import org.opentripplanner.standalone.datastore.base.ZipStreamDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class GsDataSourceRepository implements DataSourceRepository {
    private final String credentialsFilename;
    private Storage storage;

    public GsDataSourceRepository(String credentialsFilename) {
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
        return createSource(path, type);
    }


    /* private methods */

    private static boolean isGoogleCloudStorageUri(URI uri) {
        return "gs".equals(uri.getScheme());
    }

    private DataSource createSource(BlobPath path, FileType type) {
        Bucket bucket = storage.get(path.bucket);

        Blob blob = bucket.get(path.objectName);

        if(blob != null) {
            DataSource gsSource = new GcsFileDataSource(blob, type, path.toString());
            if(blob.getName().endsWith(".zip")) {
                return new ZipStreamDataSource(gsSource);
            }
            return gsSource;
        }

        if(type.isCompositeInputDataFile()) {
            return new GcsDirectoryDataSource(storage, path, type);
        }

        BlobId blobId = BlobId.of(bucket.getName(), path.objectName);
        return new GsOutFileDataStore(storage, blobId, type, path.toString());
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
