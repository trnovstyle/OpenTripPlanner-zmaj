package org.opentripplanner.standalone.datastore.file;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.reflect.ReflectionLibrary;
import org.opentripplanner.standalone.config.GraphBuilderParameters;
import org.opentripplanner.util.EnvironmentVariableReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Find and parse config files first to reveal syntax errors early without waiting for graph build.
 */
public class ConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String BUILDER_CONFIG_FILENAME = "build-config.json";
    private static final String ROUTER_CONFIG_FILENAME = "router-config.json";

    /**
     * Check if a file is a config file using the configuration file name.
     * This method returns {@code true} if the file match {@code (build-config|router-config).json}.
     */
    public static boolean isConfigFile(String filename) {
        return BUILDER_CONFIG_FILENAME.equals(filename) || ROUTER_CONFIG_FILENAME.equals(filename);
    }

    /**
     * Load the graph builder configuration file as a JsonNode three. An empty node is
     * returned if the given {@code configDir}  is {@code null} or config file is NOT found.
     * <p>
     * This method also log all loaded parameters to the console.
     * <p>
     * @see #loadJson for more details.
     */
    public static JsonNode loadBuilderConfig(File configDir) {
        // Use default build parameters if no configDir is available.
        if (configDir == null) {
            return MissingNode.getInstance();
        }
        assertConfigDirIsADirectory(configDir);

        File builderConfigFile = new File(configDir, BUILDER_CONFIG_FILENAME);
        JsonNode builderConfig = loadJson(builderConfigFile);

        LOG.info("Found and loaded JSON configuration file '{}'", builderConfigFile);
        LOG.info(ReflectionLibrary.dumpFields(new GraphBuilderParameters(builderConfig)));

        return builderConfig;
    }

    /**
     * Load the router configuration file as a JsonNode three. An empty node is
     * returned if the given {@code configDir}  is {@code null} or config file is NOT found.
     * <p>
     * @see #loadJson for more details.
     */
    public static JsonNode loadRouterConfig(File configDir) {
        // Use default build parameters if no configDir is available.
        if (configDir == null) {
            return MissingNode.getInstance();
        }
        assertConfigDirIsADirectory(configDir);

        return loadJson(new File(configDir, ROUTER_CONFIG_FILENAME));
    }

    /**
     * Open and parse the JSON file at the given path into a Jackson JSON tree. Comments and
     * unquoted keys are allowed. Returns an empty node if the file does not exist. Throws an
     * exception if the file contains syntax errors or cannot be parsed for some other reason.
     * <p>
     * We do not require any JSON config files to be present because that would get in the way of
     * the simplest rapid deployment workflow. Therefore we return an empty JSON node when the file
     * is missing, causing us to fall back on all the default values as if there was a JSON file
     * present with no fields defined.
     */
    public static JsonNode loadJson(File file) {
        try {
            String configString = IOUtils.toString(
                    new FileInputStream(file), StandardCharsets.UTF_8
            );
            EnvironmentVariableReplacer envReplacer = new EnvironmentVariableReplacer();
            configString = envReplacer.replace(configString);

            return toJsonNode(configString, file.toString());
        }
        catch (FileNotFoundException ex) {
            LOG.info("File '{}' is not present. Using default configuration.", file);
            return MissingNode.getInstance();
        }
        catch (IOException e) {
            LOG.error("Error while parsing JSON config file '{}': {}", file, e.getMessage());
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Convert a String into JsonNode. Comments and unquoted fields are allowed
     * int he given {@code jsonAsString} input.
     */
    public static JsonNode toJsonNode(String jsonAsString, String source) {
        try {
            if(jsonAsString == null) {
                return MissingNode.getInstance();
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

            return mapper.readTree(jsonAsString);
        }
        catch (IOException e) {
            LOG.error("Error while parsing JSON config file '{}': {}", source, e.getMessage());
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    private static void assertConfigDirIsADirectory(File configDir) {
        if (!configDir.isDirectory()) {
            throw new IllegalArgumentException(
                    configDir + " is not a readable configuration directory.");
        }
    }
}
