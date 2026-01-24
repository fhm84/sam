package de.halbmann.storage.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface FileSystemWrapper {

  Path resolve(String relativePath);

  OutputStream openForWrite(String relativePath) throws IOException;

  InputStream openForRead(String relativePath) throws IOException;

  boolean exists(String relativePath);

  default void save(String relativePath, InputStream in) throws IOException {
    try (OutputStream out = openForWrite(relativePath)) {
      in.transferTo(out);
    }
  }

  void move(String sourceRelativePath, String targetRelativePath) throws IOException;

  void delete(String relativePath) throws IOException;
}
