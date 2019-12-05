/**
 * Add support for Google Cloud Storage, getting all input files and storing the Graph.obj in the
 * cloud.
 * <p>
 * This implementation will use the existing
 * {@link org.opentripplanner.standalone.datastore.file.ConfigLoader}
 * to load config from the local disk.
 */
package org.opentripplanner.standalone.datastore.gs;