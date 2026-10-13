package de.halbmann.sam.business.sheets.entity;

/**
 * Comparison operator for an {@link InstrumentationCountCriterion}, applied to the number of
 * {@code InstrumentationEntity} rows a sheet has for a given instrument. The set is closed under
 * negation (e.g. "not at least 1" is {@code LT 1}), so criteria need no separate NOT flag.
 */
public enum InstrumentationCountOperator {
    EQ("="),
    NEQ("<>"),
    LT("<"),
    LTE("<="),
    GT(">"),
    GTE(">=");

    private final String sql;

    InstrumentationCountOperator(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
