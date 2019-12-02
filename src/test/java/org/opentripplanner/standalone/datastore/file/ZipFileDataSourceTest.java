package org.opentripplanner.standalone.datastore.file;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.standalone.datastore.FileType.GTFS;

public class ZipFileDataSourceTest {

    // Sometime close to 2000-01-01
    private static final long TIME = 30 * 365 * 24 * 60 * 60 * 1000L;
    private static final String FILENAME = "src/test/resources/netex_mapping_test/gtfs_minimal_fileset/gtfs_minimal.zip";

    @Test
    public void testAccessorsForNoneExistingFile() throws IOException {
        // Given:
        File target = new File(FILENAME);
        File copyTarget = new File(FILENAME);
        CompositeDataSource subject = new ZipFileDataSource(target, GTFS);
        CompositeDataSource copySubject = new ZipFileDataSource(copyTarget, GTFS);
        String expectedPath = target.getPath();

        // Verify zip file exist before we start the test
        assertTrue(target.getAbsolutePath(), target.exists());

        // Then
        assertEquals("gtfs_minimal.zip", subject.name());
        assertEquals(expectedPath, subject.path());
        assertEquals(GTFS, subject.type());
        assertTrue("Last modified: " + subject.lastModified(), subject.lastModified() > TIME);
        assertTrue("Size: " + subject.size(), subject.size() > 100);
        assertTrue(subject.exists());
        // We do not support writing to zip files
        assertFalse(subject.isWritable());

        assertEquals(expectedPath, subject.toString());
        assertEquals(copySubject, subject);
        assertEquals(copySubject.hashCode(), subject.hashCode());

        subject.close();
        copySubject.close();
    }

    @Test
    public void testIO() throws IOException {
        // Given:
        File target = new File(FILENAME);
        CompositeDataSource subject = new ZipFileDataSource(target, GTFS);

        Collection<DataSource> content = subject.content();
        Collection<String> names = content.stream().map(it -> it.name()).collect(Collectors.toList());

        System.out.println(names);
        assertTrue(
                names.toString(),
                names.containsAll(List.of("agency.txt", "stops.txt", "trips.txt"))
        );

        DataSource entry = subject.entry("feed_info.txt");

        List<String> lines = IOUtils.readLines(entry.asInputStream(), StandardCharsets.UTF_8);
        assertEquals("feed_id,feed_publisher_name,feed_publisher_url,feed_lang", lines.get(0));
        assertEquals("RB,Rutebanken,http://www.rutebanken.org,no", lines.get(1));

        // Close zip
        subject.close();
    }

    @Test
    public void testEntryProperties() throws IOException {
        // Given:
        File target = new File(FILENAME);
        CompositeDataSource subject = new ZipFileDataSource(target, GTFS);
        DataSource entry = subject.entry("trips.txt");

        assertEquals("trips.txt", entry.name());
        assertEquals("trips.txt (" + subject.path() + ")", entry.path());
        assertEquals(GTFS, entry.type());
        assertTrue("Last modified: " + entry.lastModified(), entry.lastModified() > TIME);
        assertTrue("Size: " + entry.size(), entry.size() > 100);
        assertTrue(entry.exists());
        // We do not support writing to zip entries
        assertFalse(entry.isWritable());
    }
}