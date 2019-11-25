package org.opentripplanner.standalone.datastore;


import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotNull;
import java.io.Closeable;
import java.util.Collection;

/**
 * It is an abstraction to reading of and writing to files whenever the file is located on the local
 * dick or is in the cloud.
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
 * #close()} method is called the only method witch is available is the {@link #path()}, all other
 * methods behavior is undefined. This also propagate to all {@link DataSource} children.
 */
public interface OtpDataStore extends Closeable {

    /**
     * Give a description of where the data store is located, like the file directory path for file
     * data store.
     * <p>
     * This method is available after this data store is closed.
     */
    @NotNull
    String path();

    /**
     * List all existing data sources by file type. An empty list is returned if there is no files
     * of the given type.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     *
     * @return a collection of {@link DataSource} or {@link CompositeDataSource}. If the type is a
     * {@link FileType#isCompositeInputDataFile()} then it is safe to cast the elements to the sub
     * type.
     */
    @NotNull
    Collection<DataSource> listExistingSourcesFor(FileType type);

    /**
     * Get the a proxy for the given source name, the source may or may not {@link
     * DataSource#exist()}.
     * <p>
     * This method should not be called after this data store is closed.
     *
     * @param sourceName the short name including extension like: {@code Graph.obj}
     */
    @NotNull
    DataSource getSource(String sourceName, FileType type);

    /**
     * @return the graph builder parameters loaded from the configuration directory.
     */
    @NotNull
    JsonNode graphBuilderParameters();

    /**
     * @return the router config parameters loaded from the configuration directory.
     */
    @NotNull
    JsonNode routerConfigParameters();
}
