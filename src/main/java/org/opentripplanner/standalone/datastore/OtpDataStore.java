package org.opentripplanner.standalone.datastore;


import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.standalone.config.GraphBuilderParameters;

import java.io.Closeable;
import java.util.Collection;
import java.util.Set;

/**
 * This interface provide an abstraction layer for accessing OTP data sources - the default is to
 * read/write data from the local file system, but this abstraction make it possible to implement
 * alternative ways to access data. In a cloud ecosystem you might find it easier to access the data
 * directly from the cloud storage, rather than first copy the data into your node local disk, and
 * then copy it back into cloud storage after building a graph. Depending on the source this might
 * also offer enhanced performance.
 * <p>
 * This interface extends {@link Closeable} because it might need to access a remote resource.
 * Keeping the connection until the source is read is desirable. For other resources, like the local
 * file system, there is no need to implement the close method.
 * <p>
 * Make sure the {@link #close()} method is called after all data is read. After the {@link
 * #close()} method is called the only method witch is available is the {@link #description()}, all
 * other methods behavior is undefined. This also propagate to all {@link DataSource} children.
 * <p>
 * Use the {@link org.opentripplanner.standalone.datastore.configure.DataStoreConfig} to obtain a
 * new instance of this interface.
 */
public interface OtpDataStore extends Closeable {

    /**
     * Give a short description of where the data store is located, like the file directory path for
     * file data store.
     * <p>
     * Is available after this data store is closed.
     */
    String description();


    /**
     * Return true if there is at least one input data source for the given type.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     */
    boolean hasInputOf(FileType type);

    /**
     * List all existing input data sources by input file type. An empty list is returned if there
     * is no files of the given type.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     */
    Collection<DataSource> listInputFor(FileType type);

    /**
     * List all existing input data composite(zip file) data sources by input file type. An empty
     * list is * returned if there is no files of the given type.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     */
    Collection<CompositeDataSource> listCompositeInputFor(FileType type);


    /**
     * @return all input file types present available in the datasource.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     */
    Set<FileType> listInputFileTypes();

    /**
     * Get the base graph, the graph may or may not {@link DataSource#exist()}.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     */
    DataSource getBaseGraph();

    /**
     * Get the graph, the graph may or may not {@link DataSource#exist()}.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     */
    DataSource getGraph();

    /**
     * @return the graph builder parameters loaded from the configuration directory.
     */
    JsonNode graphBuilderParameters();

    /**
     * @return the router config parameters loaded from the configuration directory.
     */
    JsonNode routerConfigParameters();
}
