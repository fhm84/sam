package de.halbmann.sam.api.entity.sheets;

/**
 * Physical condition of a printed sheet music copy in the ensemble's archive.
 */
public enum PhysicalCondition {

    /**
     * Copy is in good, usable condition.
     */
    GOOD,

    /**
     * Copy shows signs of wear but is still usable.
     */
    WORN,

    /**
     * Copy is damaged and may be difficult to use.
     */
    DAMAGED,

    /**
     * Copy cannot be found in the archive.
     */
    LOST
}
