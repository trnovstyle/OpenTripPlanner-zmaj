package org.opentripplanner.standalone.datastore;

import org.opentripplanner.common.LoggingUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;


/**
 * A data source is generalized type to represent an file, database blob or unit that OTP read or
 * write to.
 * <p>
 * The data source instance contain metadata like {@code name}, {@code description}, {@code type}
 * and so on. To access (read from or write to) a datasource the methods {@link #asInputStream()}
 * and {@link #asOutputStream()} will open a connection to the underlying data source and make it
 * available for reading/writing.
 * <p>
 * Only metadata is retrieved before a stream is opened, making sure minimum data is transferred
 * before it is actually needed.
 * <p>
 * The data source metadata should be fetched once. The data is NOT updated even if the source
 * itself changes. If this happens it might cause the streaming to fail.
 */
public interface DataSource {

    /**
     * @return the short name identifying the source within its scope (withing a {@link
     * OtpDataStore} or {@link CompositeDataSource}) Including the file extension.
     * <p>
     * Examples:
     * <p>
     * {@code build-config.json, gtfs.zip and stops.txt}
     */
    String name();

    /**
     * @return the full path (or description) to be used when describing this data source. This
     * method is mainly used for humans to identify the source in logs and error handling.
     */
    String path();

    /**
     * The file type this data source is identified as.
     */
    FileType type();

    /**
     * @return size in bytes, if unknown returns {@code -1}
     */
    default long size() { return -1; }

    /**
     * @return last modified timestamp in ms, if unknown returns {@code -1}
     */
    default long lastModified() { return -1; }

    /**
     * @return true is it exist in the data store; hence calling {@link #asInputStream()} is safe.
     */
    default boolean exist() { return true; }

    /**
     * @return {@code true} if it is possible to write to data source. Also, return {@code true} if
     * if it is not easy to check. No guarantee is given and the {@link #asOutputStream()} may
     * fail. This method can be used to avoid consuming a lot of resource before writing to a
     * datasource, if this method return {@code false}.
     */
    default boolean isWritable() { return true; }

    /**
     * Connect to this data source and make it available as an input stream. The caller is
     * responsible to close the connection.
     * <p>
     * Note! This method might get called several times, and each time a new Stream should be
     * created.
     */
    default InputStream asInputStream() {
        throw new UnsupportedOperationException(
                "This datasource type " + type()
                        + " do not support READING. Can not read from: " + path()
        );
    }

    default OutputStream asOutputStream() {
        throw new UnsupportedOperationException(
                "This datasource type " + type()
                + " do not support WRITING. Can not write to: " + path()
        );
    }

    /**
     * Return an info string like this:
     * <p>
     * {@code oslo_norway.osm.pbf, modified: 2018-02-13 22:23:27, size: 57,5M}
     */
    default String detailedInfo() {
        String info = name();
        if (lastModified() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            info += ", modified: " + sdf.format(lastModified());
        }
        if (size() > 0) {
            info += ", size: " + LoggingUtil.human(size());
        }
        return info;
    }
}
