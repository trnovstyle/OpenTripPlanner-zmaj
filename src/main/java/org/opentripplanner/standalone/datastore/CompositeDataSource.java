package org.opentripplanner.standalone.datastore;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * A composite data source contain a collection of other {@link DataSource}s.
 * <p>
 * Example: gtfs.zip and netex.zip
 */
public interface CompositeDataSource extends DataSource, Closeable {

    /**
     * Open the composite data source and read the content. This does not read each entry, but just
     * the metadata for each of them.
     */
    Collection<DataSource> content();

    /**
     * Retrieve a single entry by name, or {@code null} if not found.
     * <p>
     * Example:
     * <p>
     * {@code DataSource routesSrc = gtfsSource.entry("routes.txt")}
     */
    DataSource entry(String name);

    /**
     * Delete content and container in store.
     */
    default void delete() throws IOException {
        throw new UnsupportedOperationException(
                "This datasource type " + type()
                + " do not support DELETE. Can not delete: " + path()
        );
    }
}
