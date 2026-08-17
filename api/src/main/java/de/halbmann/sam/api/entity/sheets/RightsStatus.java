package de.halbmann.sam.api.entity.sheets;

/**
 * Legal status of a sheet with respect to archiving, digitization, and distribution.
 *
 * <p>
 * This is distinct from the free-text copyright field or the GEMA work number — it is a
 * queryable status a librarian assigns to make clear what is and isn't allowed to be
 * done with the physical or digital material.
 * </p>
 */
public enum RightsStatus {

    /**
     * No rights research has been done yet, or the status is genuinely unclear.
     */
    UNKNOWN,

    /**
     * Public domain — no copyright restrictions apply.
     */
    PUBLIC_DOMAIN,

    /**
     * Covered by a license (e.g. purchased performance/print license) that permits the
     * ensemble's normal use.
     */
    LICENSED,

    /**
     * Not licensed in the general sense, but the publisher/arranger has given explicit
     * permission to archive and keep a copy (e.g. a written note, an email) — short of a
     * full license.
     */
    PERMITTED_ARCHIVE,

    /**
     * Use is restricted; explicit permission must be sought before each new use
     * (performance, copying, distribution).
     */
    RESTRICTED,

    /**
     * The physical original may be archived and catalogued, but digitizing/scanning it is
     * specifically prohibited (common for rental/hire-only orchestral material).
     */
    NO_DIGITALIZATION
}
