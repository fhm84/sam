package de.halbmann.sam.storage;

import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

public final class MimeTypeUtils {

  private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();

  private MimeTypeUtils() {}

  public static String resolveExtension(String mimeType, String originalFilename) {
    return MimeTypeUtils.extensionFor(mimeType)
        .or(
            () ->
                Optional.ofNullable(FilenameUtils.getExtension(originalFilename))
                    .filter(ext -> !ext.isBlank()))
        .orElse("bin");
  }

  public static Optional<String> extensionFor(String mimeType) {
    if (mimeType == null || mimeType.isBlank()) {
      return Optional.empty();
    }

    try {
      MimeType mt = MIME_TYPES.forName(mimeType);
      String ext = mt.getExtension(); // includes leading dot
      if (ext != null && !ext.isBlank()) {
        return Optional.of(ext.substring(1)); // remove dot
      }
    } catch (Exception ignored) {
    }

    return Optional.empty();
  }
}
