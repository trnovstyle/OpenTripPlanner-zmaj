package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Configure paths to each individual file resource. Use URIs to specify paths. I a parameter is
 * specified it override any local files, and the local file is NOT loaded.
 * <p>
 * Google Cloud Storage(GCS) access is supported. Use the following formats:
 * <pre>
 *     gs://bucket-name/pate/a/b/c/filename.ext
 * </pre>
 */
public class StorageParameters {
    /**
     * Google Cloud Storage credentials file to use when accessing GCS blobs. When using GCS from
     * outside of the bucket cluster you need to provide a path the the service credentials, you may
     * use environment variables in the given value: {@code "credentialsFile" : "${MY_GOC_SERVICE}"
     * }
     * <p>
     * This parameter is optional.
     */
    public final String gcsCredentials;


    /**
     * Path to the baseGraph for reading and writing. The file is created or overwritten if OTP
     * saves the graph to the file.
     * <p>
     * Example: {@code "baseGraph" : "file:///Users/kelvin/otp/baseGraph.obj" }
     * <p>
     * This parameter is optional.
     */
    public final URI baseGraph;

    /**
     * Path to the graph for reading and writing. The file is created or overwritten if OTP saves
     * the graph to the file.
     * <p>
     * Example: {@code "graph" : "gs://my-bucket/otp/graph.obj" }
     * <p>
     * This parameter is optional.
     */
    public final URI graph;

    /**
     * Path to the open street map file.
     * <p>
     * Example: {@code "osm" : "file:///Users/kelvin/otp/norway-osm.pbf" }
     * <p>
     * This parameter is optional.
     */
    public final URI osm;

    /**
     * Path to elevation data file.
     * <p>
     * Example: {@code "osm" : "file:///Users/kelvin/otp/norway-dem.tif" }
     * <p>
     * This parameter is optional.
     */
    public final URI dem;

    /**
     * Array of GTFS data files.
     * <p>
     * Example: {@code "transit" : [ "file:///Users/kelvin/otp/gtfs.zip", "gs://my-bucket/gtfs.zip" ]" }
     * <p>
     * This parameter is optional.
     */
    @NotNull
    public final List<URI> gtfs = new ArrayList<>();

    /**
     * Array of Netex data files.
     * <p>
     * Example: {@code "transit" : [ "file:///Users/kelvin/otp/netex.zip", "gs://my-bucket/netex.zip" ]" }
     * <p>
     * This parameter is optional.
     */
    @NotNull
    public final List<URI> netex = new ArrayList<>();

    /**
     * Path to the OTP status file name WITHOUT any extension. When OTP start the the file is
     * created with the extension ".inProgress" and then when OTP exit the file extension is
     * changed to ".ok" or ".failed".
     * <p>
     * Example: {@code "otpStatus" : "file:///Users/kelvin/otp/otp-status" }
     * <p>
     * This parameter is optional.
     */
    public final URI otpStatus;


    /**
     * Directory path to where the graph build report should be written. The html report is
     * written into this directory. If the directory exist, any existing files are deleted.
     * If it does not exist, it is created.
     * <p>
     * Example: {@code "osm" : "file:///Users/kelvin/otp/norway-dem.tif" }
     * <p>
     * This parameter is optional.
     */
    public final URI buildReport;

    StorageParameters(JsonNode node) {
        this.gcsCredentials = node.path("gcsCredentials").asText(null);
        this.baseGraph = uriFromJson("baseGraph", node);
        this.graph = uriFromJson("graph", node);
        this.osm = uriFromJson("osm", node);
        this.dem = uriFromJson("dem", node);
        this.gtfs.addAll(uris("gtfs", node));
        this.netex.addAll(uris("netex", node));
        this.otpStatus = uriFromJson("otpStatus", node);
        this.buildReport = uriFromJson("buildReport", node);
    }

    static List<URI> uris(String name, JsonNode node) {
        List<URI> uris = new ArrayList<>();
        for (JsonNode it : node.path(name)) {
            uris.add(uriFromString(name, it.asText()));
        }
        return uris;
    }

    static URI uriFromJson(String name, JsonNode node) {
        return uriFromString(name, node.path(name).asText());
    }

    static URI uriFromString(String name, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new URI(text);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Unable to parse 'storage' parameter in 'builder-config.json': "
                    + "\n\tActual: \"" + name + "\" : \"" + text + "\""
                    + "\n\tExpected valid URI, it should be parsable by java.net.URI class.");
        }
    }

    /**
     * Detect if any of the file resources in the storage have scheme "gs" (prefixed with "gs:")
     */
    public boolean isGoogleCloudStorageEnabled() {
        return gatAllFileUris().stream().anyMatch(it -> "gs".equals(it.getScheme()));
    }

    private Collection<URI> gatAllFileUris() {
        List<URI> uris = new SkipNullList<>();
        uris.add(baseGraph);
        uris.add(graph);
        uris.add(osm);
        uris.add(dem);
        uris.addAll(gtfs);
        uris.addAll(netex);
        uris.add(otpStatus);
        uris.add(buildReport);
        return uris;
    }

    /**
     * An array list witch drop adding {@code null} elements.
     */
    static class SkipNullList<T> extends ArrayList<T> {
        @Override
        public boolean add(T obj) {
            return obj != null && super.add(obj);
        }
    }
}
