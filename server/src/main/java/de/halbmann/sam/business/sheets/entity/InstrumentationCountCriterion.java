package de.halbmann.sam.business.sheets.entity;

import de.halbmann.sam.core.exception.ValidationException;

/**
 * One "sheets with N of instrument X" search criterion, parsed from the
 * {@code <instrumentId>:<operator>:<count>} wire format used by
 * {@code SheetFilterRequest#getInstrumentCriteria()}.
 */
public record InstrumentationCountCriterion(String instrumentId, InstrumentationCountOperator operator, int count) {

    public static InstrumentationCountCriterion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Invalid instrument criterion: value must not be blank");
        }

        // Operator and count are split off from the right, so an instrument ID may itself contain ':'
        int countSeparator = raw.lastIndexOf(':');
        int operatorSeparator = countSeparator > 0 ? raw.lastIndexOf(':', countSeparator - 1) : -1;
        if (operatorSeparator < 0 || raw.substring(0, operatorSeparator).isBlank()) {
            throw new ValidationException(
                    "Invalid instrument criterion '" + raw + "', expected format <instrumentId>:<operator>:<count>");
        }
        String instrumentId = raw.substring(0, operatorSeparator);
        String operatorName = raw.substring(operatorSeparator + 1, countSeparator);
        String countValue = raw.substring(countSeparator + 1);

        InstrumentationCountOperator operator;
        try {
            operator = InstrumentationCountOperator.valueOf(operatorName);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid instrument criterion operator '" + operatorName + "' in '" + raw + "'", e);
        }

        int count;
        try {
            count = Integer.parseInt(countValue);
        } catch (NumberFormatException e) {
            throw new ValidationException(
                    "Invalid instrument criterion count '" + countValue + "' in '" + raw + "'", e);
        }
        if (count < 0) {
            throw new ValidationException("Invalid instrument criterion count in '" + raw + "': must be >= 0");
        }

        return new InstrumentationCountCriterion(instrumentId, operator, count);
    }

    /**
     * This criterion as a query condition: {@code countExpression} (counting the sheet's parts for
     * {@link #instrumentId()}) compared against the named parameter {@code countParameter}.
     */
    public String toCondition(String countExpression, String countParameter) {
        return countExpression + " " + operator.sql() + " :" + countParameter;
    }
}
