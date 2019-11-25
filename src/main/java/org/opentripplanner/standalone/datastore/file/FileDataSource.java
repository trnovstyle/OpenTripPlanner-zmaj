package org.opentripplanner.standalone.datastore.file;

import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.standalone.datastore.FileType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

public class FileDataSource implements DataSource {

  private final File file;
  private final FileType type;


  /**
   * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files
   * as well as normal files. It does not handle directories({@link DirectoryDataSource}) or
   * zip-files {@link ZipFileDataSource} witch contain multiple files.
   */
  public FileDataSource(File file, FileType type) {
    this.file = file;
    this.type = type;
  }

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public String path() {
    return file.toString();
  }

  @Override
  public FileType type() {
    return type;
  }

  @Override
  public long size() {
    return file.length();
  }

  @Override
  public long lastModified() {
    return file.lastModified();
  }

  @Override
  public boolean exist() {
    return file.exists() && file.canRead();
  }

  @Override
  public boolean isWritable() {
    // We assume we can write to a file if the parent directory exist, and if the
    // file it self exist then it must be writable. If the file do not exist
    // we assume we can create a new file and write to it - there is no check on this.
    return file.getParentFile().exists() && (!file.exists() || file.canWrite());
  }

  @Override
  public InputStream asInputStream() {
    try {
      // We support both gzip and unzipped files when reading.
      if (file.getName().endsWith(".gz")) {
        return new GZIPInputStream(new FileInputStream(file));
      } else {
        return new FileInputStream(file);
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public OutputStream asOutputStream() {
    try {
      return new FileOutputStream(file);
    }
    catch (FileNotFoundException e) {
      throw new IllegalStateException(e.getLocalizedMessage(), e);
    }
  }
}
