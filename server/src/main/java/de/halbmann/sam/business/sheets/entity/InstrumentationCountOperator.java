package de.halbmann.sam.business.sheets.entity;

/**
 * Comparison operator for an {@link InstrumentationCountCriterion}, applied to the number of
 * {@code InstrumentationEntity} rows a sheet has for a given instrument.
 */
public enum InstrumentationCountOperator {
    EQ("="),
    LTE("<="),
    GTE(">=");

    private final String sql;

    InstrumentationCountOperator(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
