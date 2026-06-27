package de.halbmann.sam;

import lombok.experimental.UtilityClass;

/**
 * Central registry of SAM application configuration keys. All custom {@code sam.*} MicroProfile
 * Config property names are defined here so they can be referenced by both the consuming bean and
 * the documentation without hard-coding the string in multiple places.
 */
@UtilityClass
public class EnvConsts {

    /** Storage root: bare path for local FS ({@code /data/sam}) or S3 URI ({@code s3://bucket/prefix}). Required. */
    public static final String FILESYSTEM_BASE_PATH = "sam.filesystem.base.path";

    /**
     * Optional file-extension whitelist or blacklist applied on upload.
     * Whitelist: {@code pdf|xml|mp3}. Blacklist (prefix {@code ^}): {@code ^exe|bat}.
     * Absent → all file types are accepted.
     */
    public static final String FILETYPES = "sam.files.types";

    /**
     * Keycloak realm name used by the admin REST client for user search.
     * Must be set per profile (dev/prod); no default.
     */
    public static final String KEYCLOAK_ADMIN_REALM = "sam.admin.keycloak.realm";

    /**
     * Minimum coverage score (0.0–1.0) awarded to an ensemble voice that has at least one
     * positive instrumentation match. Prevents a single part from contributing near-zero.
     * Default: {@code 0.7}.
     */
    public static final String COVERAGE_BASE_SCORE = "sam.coverage.base-score";

    /**
     * When {@code true}, a second AI pass using {@code ClassificationAgent} autonomously resolves
     * entity references (sheet, musicians, instruments) after initial classification before
     * returning the suggestion to the UI. Default: {@code false}.
     */
    public static final String CLASSIFICATION_AGENTIC = "sam.classification.agentic";
}
