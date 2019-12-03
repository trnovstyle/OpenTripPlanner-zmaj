package org.opentripplanner.standalone.datastore;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.standalone.datastore.configure.DataStoreConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.standalone.datastore.FileType.CONFIG;
import static org.opentripplanner.standalone.datastore.FileType.DEM;
import static org.opentripplanner.standalone.datastore.FileType.GRAPH;
import static org.opentripplanner.standalone.datastore.FileType.GTFS;
import static org.opentripplanner.standalone.datastore.FileType.NETEX;
import static org.opentripplanner.standalone.datastore.FileType.OSM;
import static org.opentripplanner.standalone.datastore.FileType.OTP_STATUS;
import static org.opentripplanner.standalone.datastore.FileType.REPORT;
import static org.opentripplanner.standalone.datastore.FileType.UNKNOWN;

public class OtpDataStoreTest {

    private static final String BUILD_CONFIG_FILENAME = "build-config.json";
    private static final String ROUTER_CONFIG_FILENAME = "router-config.json";
    private static final String OSM_FILENAME = "osm.pbf";
    private static final String DEM_FILENAME = "dem.tif";
    private static final String NETEX_FILENAME = "netex.zip";
    private static final String GTFS_FILENAME = "gtfs.zip";
    private static final String BASE_GRAPH_FILENAME = "baseGraph.obj";
    private static final String GRAPH_FILENAME = "Graph.obj";
    private static final String OTP_STATUS_FILENAME = "otp-status.inProgress";
    private static final String REPORT_FILENAME = "report";
    private static final String UTF_8 = "UTF-8";
    private static final long D2000_01_01 = ZonedDateTime.parse("2000-01-01T12:00+01:00")
            .toInstant().toEpochMilli();

    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("OtpDataStoreTest-").toFile();
    }

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void tearDown() {
        tempDir.delete();
    }

    @Test
    public void readEmptyDir() {
        OtpDataStore store = new DataStoreConfig(tempDir).open();
        assertNoneExistingFile(store.getBaseGraph(), BASE_GRAPH_FILENAME, GRAPH);
        assertNoneExistingFile(store.getGraph(), GRAPH_FILENAME, GRAPH);
        assertNoneExistingFile(store.getOtpStatus(), OTP_STATUS_FILENAME, OTP_STATUS);
        assertNoneExistingFile(store.getBuildReport(), REPORT_FILENAME, REPORT);

        assertTrue(store.listExistingSourcesFor(CONFIG).isEmpty());
        assertTrue(store.listExistingSourcesFor(OSM).isEmpty());
        assertTrue(store.listExistingSourcesFor(DEM).isEmpty());
        assertTrue(store.listExistingSourcesFor(GTFS).isEmpty());
        assertTrue(store.listExistingSourcesFor(NETEX).isEmpty());
        assertTrue(store.listExistingSourcesFor(GRAPH).isEmpty());
        assertTrue(store.listExistingSourcesFor(OTP_STATUS).isEmpty());
        assertTrue(store.listExistingSourcesFor(REPORT).isEmpty());
        assertTrue(store.listExistingSourcesFor(UNKNOWN).isEmpty());
    }

    @Test
    public void readDirWithEverything() throws IOException {
        write(BUILD_CONFIG_FILENAME, "{}");
        write(ROUTER_CONFIG_FILENAME, "{}");
        write(OSM_FILENAME, "Data");
        write(DEM_FILENAME, "Data");
        writeZip(GTFS_FILENAME);
        writeZip(NETEX_FILENAME);
        write(BASE_GRAPH_FILENAME, "Data");
        write(GRAPH_FILENAME, "Data");
        write(OTP_STATUS_FILENAME, "Data");
        writeToReport();

        OtpDataStore store = new DataStoreConfig(tempDir).open();
        assertExistingSource(store.getBaseGraph(), BASE_GRAPH_FILENAME, GRAPH);
        assertExistingSource(store.getGraph(), GRAPH_FILENAME, GRAPH);
        assertReportExist(store.getBuildReport());
        assertExistingSource(store.getOtpStatus(), OTP_STATUS_FILENAME, OTP_STATUS);

        assertExistingSources(store.listExistingSourcesFor(OSM), OSM_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(DEM), DEM_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(GTFS), GTFS_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(NETEX), NETEX_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(CONFIG), BUILD_CONFIG_FILENAME, ROUTER_CONFIG_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(GRAPH), BASE_GRAPH_FILENAME, GRAPH_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(OTP_STATUS), OTP_STATUS_FILENAME);
    }


    @Test
    public void testResolvingFileUris() throws IOException {
        // Given a temp data directory to dump files in, must be different from tempDir
        File tempDataDir = Files.createTempDirectory("ODST-2-").toFile();
        // Set the base dir to tempDir (this is our baseDir)
        File baseDir = tempDir;

        // Get the uri for the temp data dir to insert into config file
        String uri = tempDataDir.toURI().toString() + "/";

        // Insert a URI for osm, gtfs, graph and report data sources
        String buildConfigJson = String.format(
                "{"
                + "%n  storage: {"
                + "%n      osm: ['%s'],"
                + "%n      gtfs: ['%s'],"
                + "%n      graph: '%s',"
                + "%n      buildReport: '%s'"
                + "%n  }"
                + "%n}",
                uri + OSM_FILENAME,
                uri + GTFS_FILENAME,
                uri + GRAPH_FILENAME,
                uri + REPORT_FILENAME
        ).replace('\'', '\"');

        // Create build-config, otp-status  and a unknown file in the 'baseDir'
        write(BUILD_CONFIG_FILENAME, buildConfigJson);
        write(OTP_STATUS_FILENAME, "Data");
        write("unknown.txt", "Data");

        // Save osm, gtfs, graph, report, base-graph and unknown-2-file in 'tempDataDir'
        tempDir = tempDataDir;
        // Save data to the URI location,
        write(OSM_FILENAME, "Data");
        writeZip(GTFS_FILENAME);
        write(GRAPH_FILENAME, "Data");
        writeToReport();
        // We add 2 more files, these are not configured in the build-config, and we expect
        // them to be invisible to the store; hence we won´t find them
        write(BASE_GRAPH_FILENAME, "Data");
        write("unknown-2.txt", "Data");

        // Open data store using the base-dir
        OtpDataStore store = new DataStoreConfig(baseDir).open();
        List<String> filenames = listFilesByRelativeName(store, baseDir, tempDataDir);
        filenames.sort(String::compareTo);
        String result = String.join(", ", filenames);

        // We expect to find all files set in the build-config (URIs) and
        // the ALL files added in the baseDir, but not the base-graph and unknown file
        // added to the same temp-data-dir.
        assertEquals(
                "CONFIG base:/build-config.json, "
                + "GRAPH data:/Graph.obj, "
                + "GTFS data:/gtfs.zip, "
                + "OSM data:/osm.pbf, "
                + "OTP_STATUS base:/otp-status.inProgress, "
                + "REPORT data:/report, "
                + "UNKNOWN base:/unknown.txt",
                result
        );
    }


    /* private helper methods */

    private List<String> listFilesByRelativeName(OtpDataStore store, File baseDir, File dataDir) {
        List<String> files = new ArrayList<>();
        for (FileType type : FileType.values()) {
            store.listExistingSourcesFor(type).forEach(
                    s -> {
                        String p = s.path();

                        if(p.contains(baseDir.getName())) {
                            p = "base:" + p.substring(baseDir.getPath().length());
                        }
                        else if(p.contains(dataDir.getName())) {
                            p = "data:" + p.substring(dataDir.getPath().length());
                        }
                        files.add(type.name() + " " + p);
                    }
            );
        }
        return files;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void writeToReport() throws IOException {
        File reportDir = new File(tempDir, REPORT_FILENAME);
        reportDir.mkdirs();
        FileUtils.write(new File(reportDir, "index.html"), "<html />", UTF_8);
    }

    private void write(String filename, String data) throws IOException {
        FileUtils.write(new File(tempDir, filename), data, OtpDataStoreTest.UTF_8);
    }

    private void writeZip(String filename) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(tempDir, filename)));
        ZipEntry e = new ZipEntry("stop.txt");
        out.putNextEntry(e);
        out.write("data".getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
        out.finish();
        out.close();
    }

    private void assertNoneExistingFile(DataSource source, String name, FileType type) {
        assertEquals(type, source.type());
        assertEquals(name, source.name());
        assertFalse(source.exists());
    }

    private void assertExistingSource(DataSource source, String name, FileType type) {
        assertEquals(type, source.type());
        assertEquals(name, source.name());
        assertTrue(source.exists());
        assertTrue("Last modified: " + source.lastModified(), source.lastModified() > D2000_01_01);
    }

    private void assertExistingSources(Collection<DataSource> sources, String ... names) {
        assertEquals("Size of: " + sources, names.length, sources.size());
        List<String> nameList = Arrays.asList(names);

        for (DataSource source : sources) {
            assertTrue(source.name(), nameList.contains(source.name()));
        }
    }

    private void assertReportExist(CompositeDataSource report) {
        assertEquals(REPORT, report.type());
        assertEquals(REPORT_FILENAME, report.name());
        assertTrue(report.exists());
        assertTrue(report.isWritable());
        assertEquals(report.content().toString(), 1, report.content().size());
    }
}