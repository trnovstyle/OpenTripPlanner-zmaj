package org.opentripplanner.standalone.datastore;

import java.io.Closeable;
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
    default void delete() {
        throw new UnsupportedOperationException(
                "This datasource type " + getClass().getSimpleName()
                + " do not support DELETE. Can not delete: " + path()
        );
    }

    /**
     * Delete content in store/container/directory.
     */
    default void delete(String entry) {
        throw new UnsupportedOperationException(
                "This datasource type " + getClass().getSimpleName()
                + " do not support DELETE entry. Can not delete " + entry
                + " in " + path() + "."
        );
    }

    /**
     * Rename content inside store/container/directory.
     */
    default void rename(String currentEntryName, String newEntryName) {
        throw new UnsupportedOperationException(
                "This datasource type " + getClass().getSimpleName()
                + " do not support rename entry by name. "
                + "Can not rename from: " + currentEntryName + " to " + newEntryName
                + " in " + path() + "."
        );
    }
}
