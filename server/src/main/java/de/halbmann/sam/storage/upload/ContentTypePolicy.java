package de.halbmann.sam.storage.upload;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.core.exception.ValidationException;
import de.halbmann.sam.core.storage.upload.UploadContext;
import de.halbmann.sam.core.storage.upload.UploadPolicy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Accepts only uploads whose detected content type is on the allow-list ({@link
 * EnvConsts#ALLOWED_CONTENT_TYPES}). The type comes from the content, so a web page or a program
 * renamed to {@code .pdf} is still refused: the archive shouldn't hand out such files from the
 * band's address through a share link.
 */
@Priority(25)
@ApplicationScoped
public class ContentTypePolicy implements UploadPolicy {

    private final List<String> allowed;

    @Inject
    public ContentTypePolicy(@ConfigProperty(name = EnvConsts.ALLOWED_CONTENT_TYPES) final List<String> allowed) {
        this.allowed = allowed.stream()
                .map(type -> type.trim().toLowerCase(Locale.ROOT))
                .filter(type -> !type.isEmpty())
                .toList();
    }

    @Override
    public void verify(final UploadContext context) {
        if (!isAllowed(context.mimeType())) {
            throw new ValidationException("Files of type '" + context.mimeType() + "' can't be uploaded");
        }
    }

    boolean isAllowed(final String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String type = mimeType.toLowerCase(Locale.ROOT);
        int slash = type.indexOf('/');
        if (slash <= 0) {
            return false;
        }
        String family = type.substring(0, slash);
        return allowed.stream()
                .anyMatch(entry -> entry.equals(type) || entry.equals("*/*") || entry.equals(family + "/*"));
    }
}
