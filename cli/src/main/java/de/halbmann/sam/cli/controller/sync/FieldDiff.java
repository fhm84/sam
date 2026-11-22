package de.halbmann.sam.cli.controller.sync;

/**
 * One field whose value in the import file differs from the target record.
 *
 * @param field the bean property name
 * @param fileValue the value in the import file (never {@code null})
 * @param targetValue the value on the target, possibly {@code null}
 */
public record FieldDiff(String field, Object fileValue, Object targetValue) {

    @Override
    public String toString() {
        return field + ": " + FieldDiffs.display(targetValue) + " → " + FieldDiffs.display(fileValue);
    }
}
