package de.halbmann.sam.storage.upload;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.core.storage.upload.UploadContext;
import de.halbmann.sam.core.storage.upload.UploadPolicy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * FileType/Extension validator. Based on the (optional) configuration ({@link EnvConsts#FILETYPES})
 * the documents filename is verified.
 */
@Priority(20)
@ApplicationScoped
public class FileTypePolicy implements UploadPolicy {

    private final Pattern pattern;

    private final Optional<String> fileTypes;

    @Inject
    public FileTypePolicy(@ConfigProperty(name = EnvConsts.FILETYPES) final Optional<String> fileTypes) {
        this.fileTypes = fileTypes;
        String patternString = fileTypes.orElse(".");
        final boolean blackList = patternString.startsWith("^");
        if (blackList) {
            patternString = patternString.substring(1);
            pattern = Pattern.compile(
                    "^(.(?!.*\\." + patternString + "))*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } else {
            pattern =
                    Pattern.compile("(.+(\\." + patternString + ")$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    @Override
    public void verify(UploadContext context) throws IOException {
        if (fileTypes.isPresent() && !pattern.matcher(context.filename()).matches()) {
            throw new IllegalArgumentException(
                    "Filetype for document '" + context.filename() + "' is not allowed to be uploaded!");
        }

        // Optionally check extension matches MIME type
        String ext = Optional.ofNullable(context.filename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf('.') + 1).toLowerCase())
                .orElse("");

        // Example: enforce PDF files end with .pdf
        if ("application/pdf".equals(context.mimeType()) && !"pdf".equals(ext)) {
            throw new IOException("File extension does not match detected PDF type");
        }
    }
}
