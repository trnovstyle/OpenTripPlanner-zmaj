package org.opentripplanner.standalone.datastore;


import com.fasterxml.jackson.databind.JsonNode;

import java.io.Closeable;

/**
 * This interface provide an abstraction layer for accessing OTP data sources needed for running
 * a routing service. See the {@link OtpDataStore} for a full description on DataSources.
 * <p>
 * This interface extends {@link Closeable} because it might need to access a remote resource.
 * Keeping the connection until the source is read is desirable. For other resources, like the local
 * file system, there is no need to implement the close method.
 * <p>
 * Make sure the {@link #close()} method is called after all data is read. After the {@link
 * #close()} method is called the only method witch is available is the {@link #path()}, all
 * other methods behavior is undefined. This also propagate to all {@link DataSource} children.
 * <p>
 * Use the {@link org.opentripplanner.standalone.datastore.configure.DataStoreConfig} to obtain a
 * new instance of this interface.
 */
public interface RouterDataStore extends Closeable {

    /**
     * Give a short description of where the data store is located, like the file directory path for
     * file data store.
     * <p>
     * Is available after this data store is closed.
     */
    String path();


    /**
     * Get the graph, the graph may or may not {@link DataSource#exist()}.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     */
    DataSource getGraph();

    /**
     * @return the router config parameters loaded from the configuration directory.
     */
    JsonNode routerConfigParameters();
}
