package de.halbmann.sam.business.controller;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.business.entity.AttachmentEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * FileType/Extension validator. Based on the (optional) configuration ({@link EnvConsts#FILETYPES}) the
 * document filename ({@link AttachmentEntity#getDisplayName()}) is verified.
 */
@RequestScoped
public class FileTypeValidator {

    private final Pattern pattern;

    private final Optional<String> fileTypes;

    @Inject
    public FileTypeValidator(@ConfigProperty(name = EnvConsts.FILETYPES) final Optional<String> fileTypes) {
        this.fileTypes = fileTypes;
        String patternString = fileTypes.orElse(".");
        final boolean blackList = patternString.startsWith("^");
        if (blackList) {
            patternString = patternString.substring(1);
            pattern = Pattern.compile("^(.(?!.*\\." + patternString + "))*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } else {
            pattern = Pattern.compile("(.+(\\." + patternString + ")$)",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    public void verifyFileType(final AttachmentEntity attachment) {
        if (fileTypes.isPresent() && !pattern.matcher(attachment.getDisplayName()).matches()) {
            throw new IllegalArgumentException("Filetype for document '" + attachment.getDisplayName() + "' is not allowed to be uploaded!");
        }
    }

}