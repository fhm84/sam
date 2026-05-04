package de.halbmann.sam.api.entity.shares;

/** The type of resource a share token grants access to. */
public enum ShareType {

    /** A single instrumentation (one instrument part of a sheet, including its attached files). */
    INSTRUMENTATION,

    /** A full sheet collection / setlist (TOC PDF + all individual parts within it). */
    COLLECTION,

    /** An entire sheet (all instrumentation parts, each individually downloadable + one ZIP download-all). */
    SHEET,
}
