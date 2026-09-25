package de.halbmann.sam.business.sheets.entity;

import de.halbmann.sam.core.exception.ValidationException;

/**
 * One "sheets with N of instrument X" search criterion, parsed from the
 * {@code [!]<instrumentId>:<operator>:<count>} wire format used by
 * {@code SheetFilterRequest#getInstrumentCriteria()}.
 */
public record InstrumentationCountCriterion(
        String instrumentId, InstrumentationCountOperator operator, int count, boolean negate) {

    public static InstrumentationCountCriterion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Invalid instrument criterion: value must not be blank");
        }

        boolean negate = raw.startsWith("!");
        String body = negate ? raw.substring(1) : raw;
        String[] parts = body.split(":");
        if (parts.length != 3 || parts[0].isBlank()) {
            throw new ValidationException(
                    "Invalid instrument criterion '" + raw + "', expected format [!]<instrumentId>:<operator>:<count>");
        }

        InstrumentationCountOperator operator;
        try {
            operator = InstrumentationCountOperator.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid instrument criterion operator '" + parts[1] + "' in '" + raw + "'", e);
        }

        int count;
        try {
            count = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid instrument criterion count '" + parts[2] + "' in '" + raw + "'", e);
        }
        if (count < 0) {
            throw new ValidationException("Invalid instrument criterion count in '" + raw + "': must be >= 0");
        }

        return new InstrumentationCountCriterion(parts[0], operator, count, negate);
    }
}
