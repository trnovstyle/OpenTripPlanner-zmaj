package org.opentripplanner.standalone.datastore.base;


import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;

import java.io.Closeable;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * It is an abstraction to reading of and writing to files whenever the file is located on the local
 * disk or is in the cloud.
 * <p>
 * This interface provide an abstraction layer for accessing OTP data input and output sources. It
 * is an abstraction for reading from and/or writing to files whenever the file is located on the
 * local disk or is in the cloud. The interface make it possible to implement alternative ways to
 * access data. In a cloud ecosystem you might find it easier to access the data directly from the
 * cloud storage, rather than first copy the data into your node local disk, and then copy the build
 * graph back into cloud storage after building it. Depending on the source this might also offer
 * enhanced performance.
 * <p>
 * Use the {@link org.opentripplanner.standalone.datastore.configure.DataStoreConfig} to obtain a
 * new instance of this interface.
 * <p>
 * This interface extends {@link Closeable} because it might need to access a remote resource.
 * Keeping the connection until the source is read is desirable. For other resources, like the local
 * file system, there is no need to implement the close method.
 * <p>
 * Make sure the {@link #close()} method is called after all data is read. After the {@link
 * #close()} method is called the other methods behavior is undefined. This also propagate to all
 * {@link DataSource} children.
 */
public interface DataSourceRepository extends Closeable {

    /**
     * @return a description that identify the datasource witch is helpful to the user in case an
     * error occurs when using the repository.
     */
    String description();

    /**
     * Open and/or connect to repository/storage. This method is called once, and the session is
     * terminated calling the {@link #close()} method.
     */
    void open();

    /**
     * Get the a data source for the given uri and type.
     * <p>
     * The source may or may not {@link DataSource#exists()}.
     *
     * @param uri  a uniq URI to get the data source.
     * @param type the file type to load.
     * @return the datasource wrapper that can be used to access the data source. Depending on the
     * type, the returned data source can be safely casted to a sub-type. Return {@code null} if the
     * URI protocol is not handled by the repository.
     */
    DataSource findSource(URI uri, FileType type);

    /**
     * Get the a data source for the given localFilename and type.
     * <p>
     * The source may or may not {@link DataSource#exists()}.
     * <p>
     * Only the default repository should implement this - that is the local file system repository.
     *
     * @param localFilename the short name including extension like: {@code Graph.obj}.
     * @param type          the file type to load.
     * @return the datasource wrapper that can be used to access the data source. Depending on the
     * type, the returned data source can be safely casted to a sub-type. Return {@code null} if the
     * file is not found.
     */
    default DataSource findSource(String localFilename, FileType type) {
        return null;
    }

    /**
     * List all data sources for the given type.
     * <p>
     * The source may or may not {@link DataSource#exists()}.
     * <p>
     * Only the default repository should implement this - that is the local file system
     * repository.
     *
     * @param type the file type to load.
     * @return the datasource wrapper that can be used to access the data source. Depending on the
     * type, the returned data source can be safely casted to a sub-type.
     */
    default List<DataSource> listSources(FileType type) {
        return Collections.emptyList();
    }
}
