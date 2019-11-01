package org.opentripplanner.graph_builder;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


/**
 * This class create a status file and provide methods to write status
 * to a file for other process to synchronize against.
 */
class StatusFile {
    private static final Logger LOG = LoggerFactory.getLogger(StatusFile.class);
    private static final String IN_PROGRESS_FILENAME = ".inProgress";
    private static final String OK_FILENAME = ".ok";
    private static final String FAILED_FILENAME = ".failed";

    private final File inProgressFile;
    private final File okFile;
    private final File failedFile;

    StatusFile(File baseDir, String filePrefix) {

        this.inProgressFile = new File(baseDir, filePrefix + IN_PROGRESS_FILENAME);
        this.okFile = new File(baseDir, filePrefix + OK_FILENAME);
        this.failedFile = new File(baseDir, filePrefix + FAILED_FILENAME);
    }

    void setInProgress() {
        try {
            FileUtils.deleteQuietly(inProgressFile);
            FileUtils.deleteQuietly(okFile);
            FileUtils.deleteQuietly(failedFile);
            FileUtils.writeStringToFile(inProgressFile, "{}", "UTF-8");
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    void setOk() {
        try {
            FileUtils.moveFile(inProgressFile, okFile);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    void setFailed() {
        try {
            FileUtils.moveFile(inProgressFile, failedFile);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }
}
