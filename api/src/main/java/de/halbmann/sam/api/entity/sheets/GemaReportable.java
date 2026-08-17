package de.halbmann.sam.api.entity.sheets;

/**
 * Whether a sheet is subject to GEMA reporting ("GEMA-pflichtig" in German) when performed.
 */
public enum GemaReportable {

    /**
     * Not yet determined.
     */
    UNKNOWN,

    /**
     * The piece must be reported to GEMA when performed.
     */
    YES,

    /**
     * The piece is GEMA-free (e.g. public domain or covered by a GEMA-free license).
     */
    NO
}
