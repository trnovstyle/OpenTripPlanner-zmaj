package org.opentripplanner.standalone.datastore.gcs;

import com.google.cloud.storage.Blob;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.base.ByteArrayDataSource;
import org.opentripplanner.standalone.datastore.file.DirectoryDataSource;
import org.opentripplanner.standalone.datastore.file.ZipFileDataSource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.channels.Channels.newInputStream;

class GcsZipFileDataSource extends GcsFileDataSource implements CompositeDataSource {

    private List<DataSource> content = new ArrayList<>();

    /**
     * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files
     * as well as normal files. It does not handle directories({@link DirectoryDataSource}) or
     * zip-files {@link ZipFileDataSource} witch contain multiple files.
     */
    GcsZipFileDataSource(Blob file, FileType type, String path) {
        super(file, type, path);
    }

    @Override
    public Collection<DataSource> content() {
        if(content.isEmpty()) {
            loadContent();
        }
        return content;
    }

    @Override
    public DataSource entry(String name) {
        return content.stream().filter(it -> name.equals(it.name())).findFirst().orElse(null);
    }

    @Override
    public void close() {
        /* No need to close the blob */
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public InputStream asInputStream() {
        throw new UnsupportedOperationException(
                "This datasource type " + type()
                        + " do not support READING. Can not read from: " + path()
        );
    }

    @Override
    public OutputStream asOutputStream() {
        throw new UnsupportedOperationException(
                "This datasource type " + type()
                        + " do not support WRITING. Can not write to: " + path()
        );
    }

    private void loadContent() {
        // We support both gzip and unzipped files when reading.
        try(
                InputStream in = newInputStream(getFile().reader());
                ZipInputStream zis = new ZipInputStream(in)
        ) {
            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                // we only support flat ZIP files
                if (entry.isDirectory()) { continue; }

                ByteArrayOutputStream buf = new ByteArrayOutputStream(4048);
                zis.transferTo(buf);
                byte[] bArray = buf.toByteArray();

                content.add(
                        new ByteArrayDataSource(
                            path() + "/" + entry.getName(),
                            entry.getName(),
                            type(),
                            bArray.length,
                            entry.getLastModifiedTime().toMillis()
                        ).withBytes(bArray)
                );
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
