package org.opentripplanner.standalone.datastore.file;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigLoaderTest {

    private static final String BUILD_CONFIG_FILENAME = "build-config.json";
    private static final String FILENAME = "a.obj";
    private static final String UTF_8 = "UTF-8";
    private static final String ROUTER_CONFIG_FILENAME = "router-config.json";
    private File tempDir;
    private String expectedEnvValue;
    private String json;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("OtpDataStoreTest-").toFile();
        // Given:
        Map.Entry<String, String> eVar = System.getenv().entrySet()
                .stream()
                .filter(e -> e.getValue().matches("\\w{1,20}"))
                .findFirst()
                .orElse(null);

        String eKey =  eVar == null ? "not-found" : "${" + eVar.getKey() + "}";
        expectedEnvValue =  "env:" +  (eVar == null ? "not-found" : eVar.getValue());
        json = "{"
                + "  \n// A comment"
                + "  \nkeyA: 567,"
                + "  \n\"keyB\": \"env:" + eKey + "\""
                + "\n}";
    }

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void tearDown() {
        tempDir.delete();
    }


    @Test
    public void isConfigFile() {
        assertTrue(ConfigLoader.isConfigFile("build-config.json"));
        assertTrue(ConfigLoader.isConfigFile(ROUTER_CONFIG_FILENAME));
        // TODO TGR - add loading of otp-config.json
        //assertTrue(ConfigLoader.isConfigFile("otp-config.json"));
        assertFalse(ConfigLoader.isConfigFile("not-config.json"));
    }

    @Test
    public void loadBuilderConfig() throws IOException {
        // Given:
        File file = new File(tempDir, BUILD_CONFIG_FILENAME);
        FileUtils.write(file, json, UTF_8);

        // when:
        JsonNode node = ConfigLoader.loadBuilderConfig(tempDir);

        // then:
        assertEquals(567, node.path("keyA").asInt());
        assertEquals(expectedEnvValue, node.path("keyB").asText());
    }

    @Test
    public void loadRouterConfig() throws IOException {
        // Given:
        File file = new File(tempDir, ROUTER_CONFIG_FILENAME);
        FileUtils.write(file, json, UTF_8);

        // when:
        JsonNode node = ConfigLoader.loadRouterConfig(tempDir);

        // then:
        assertEquals(567, node.path("keyA").asInt());
        assertEquals(expectedEnvValue, node.path("keyB").asText());
    }

    @Test
    public void loadNoneExistingFileResultInMissingNode() throws IOException {
        // Given:
        File noneExistingFile = new File(tempDir, "aConfig.json");

        // when:
        JsonNode res = ConfigLoader.loadJson(noneExistingFile);

        // then:
        assertTrue(res.toString(), res.isMissingNode());
    }

    @Test
    public void parseJsonString() {
        // when:
        JsonNode node = ConfigLoader.toJsonNode(json, "JSON-STRING");

        // then:
        assertEquals(567, node.path("keyA").asInt());
    }


    @Test
    public void configFailsIfDirDoesNotExist() throws IOException {
        File cfgDir = new File(tempDir, "cfg");

        try {
            ConfigLoader.loadRouterConfig(cfgDir);
            fail("Expected to fail!");
        }
        catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains(cfgDir.getName()));
        }
    }

    @Test
    public void configFailsIfDirIsAFile() throws IOException {
        File file = new File(tempDir, "AFile.txt");
        FileUtils.write(file, "{}", UTF_8);

        try {
            ConfigLoader.loadRouterConfig(file);
            fail("Expected to fail!");
        }
        catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains(file.getName()));
        }
    }

}