package org.opentripplanner.standalone.datastore.gcs;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;


/**
 * This is a an adapter to to simulate a file directory on a GCS. Files created using an instance of this
 * class wil have a common namespace. It does only support creating new output sources, it can not
 * be used to list files with the common namespace (directory path).
 */
public class GcsDirectoryDataSource implements CompositeDataSource {

    private final Storage storage;
    private final BlobPath path;
    private final FileType type;

    GcsDirectoryDataSource(Storage storage, BlobPath path, FileType type) {
        this.storage = storage;
        this.path = path;
        this.type = type;
    }

    @Override
    public String name() {
        return path.objectName;
    }

    @Override
    public String path() {
        return path.uri();
    }

    @Override
    public FileType type() {
        return type;
    }

    @Override
    public boolean exists() {
        return getBucket().list(Storage.BlobListOption.prefix(childPrefix())).hasNextPage();
    }

    @Override
    public DataSource entry(String name) {
        Blob blob = getBucket().get(name);
        // If file exist
        if(blob != null) {
            return new GcsFileDataSource(blob, type, path());
        }
        // New file
        BlobId blobId = BlobId.of(path.bucket, childPath(name));
        return new GsOutFileDataStore(storage, blobId, type, path());
    }

    @Override
    public Collection<DataSource> content() {
        Collection<DataSource> content = new ArrayList<>();
        forEachChildBlob(blob -> content.add(new GcsFileDataSource(blob, type, path())));
        return content;
    }

    @Override
    public void delete() {
        forEachChildBlob(Blob::delete);
    }

    @Override
    public void close() { }

    private Bucket getBucket() {
        return storage.get(path.bucket);
    }

    private Page<Blob> listBlobs(Bucket bucket) {
        return bucket.list(Storage.BlobListOption.prefix(childPrefix()));
    }

    private String childPrefix() {
        return path.objectName + "/";
    }

    private String childPath(String name) {
        return childPrefix() + name;
    }

    private void forEachChildBlob(Consumer<Blob>  consumer) {
        int pathIndex = childPrefix().length();
        for (Blob blob : listBlobs(getBucket()).iterateAll()) {
            String name = blob.getName().substring(pathIndex);
            // Skip nested content
            if(!name.contains("/")) {
                consumer.accept(blob);
            }
        }
    }
}
