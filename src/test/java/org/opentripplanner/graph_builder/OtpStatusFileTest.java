package org.opentripplanner.graph_builder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.file.DirectoryDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OtpStatusFileTest {
    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("OtpStatusFileTest-").toFile();
    }

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void tearDown() {
        tempDir.delete();
    }

    @Test
    public void testOtpStatusFile() {
        OtpStatusFile.start(new DirectoryDataSource(tempDir, FileType.OTP_STATUS), "my-status");

        assertTrue(new File(tempDir, "my-status.inProgress").exists());

        OtpStatusFile.exitStatusOk();

        assertFalse(new File(tempDir, "my-status.inProgress").exists());
        assertTrue(new File(tempDir, "my-status.ok").exists());
    }
}