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
     * OIDC provider, {@code pocket-id} (default) or {@code keycloak}; selects the provider's
     * settings in {@code oidc/<provider>.properties}. Env var {@code SAM_OIDC_PROVIDER}.
     */
    public static final String OIDC_PROVIDER = "sam.oidc.provider";

    /**
     * Base URL of Pocket ID's API for user search ({@code PocketIdUserDirectory}). Defaults to the
     * issuer URL; set it when SAM reaches Pocket ID under another address.
     */
    public static final String POCKET_ID_URL = "sam.oidc.pocket-id.url";

    /**
     * Pocket ID API key for user search. It acts with full admin rights on Pocket ID, so it is set
     * only in the server's env ({@code SAM_OIDC_POCKET_ID_API_KEY}) and never logged. Absent → user
     * search is off and admins link members by user ID.
     */
    public static final String POCKET_ID_API_KEY = "sam.oidc.pocket-id.api-key";

    /**
     * Keycloak realm name used by the admin REST client for user search.
     * Env var {@code KEYCLOAK_REALM}, default {@code sam}.
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
     * entity references (sheet, persons, instruments) after initial classification before
     * returning the suggestion to the UI. Default: {@code false}.
     */
    public static final String CLASSIFICATION_AGENTIC = "sam.classification.agentic";

    /**
     * When {@code true}, the image rendered for the vision classification path is additionally
     * written to a temp file for inspection. Default: {@code false}.
     */
    public static final String CLASSIFICATION_DEBUG = "sam.classification.debug";
}
