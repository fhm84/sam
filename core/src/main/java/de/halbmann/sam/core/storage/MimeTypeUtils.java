package de.halbmann.sam.core.storage;

import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

@UtilityClass
public class MimeTypeUtils {

    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();

    public static String resolveExtension(String mimeType, String originalFilename) {
        return extensionFor(mimeType)
                .or(() -> Optional.ofNullable(FilenameUtils.getExtension(originalFilename))
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
