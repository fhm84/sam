package de.halbmann.sam.business.sheets.entity;

import de.halbmann.sam.core.exception.ValidationException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Restricts a sheet search by how many parts (instrumentation rows) a sheet has for one
 * instrument, e.g. "exactly 4 horns" or "no oboes" ({@code = 0}).
 *
 * <p>Sent as a {@code partCount} query parameter in the form {@code <instrumentId>:<comparison>:<parts>},
 * e.g. {@code HORN_F:EQ:4}.
 */
public record PartCountFilter(String instrumentId, Comparison comparison, int parts) {

    /**
     * How a sheet's part count is compared with {@link #parts()}. Every comparison has its negation
     * in the set ("not more than 2" is {@code LTE 2}), so no separate NOT is needed.
     */
    public enum Comparison {
        EQ("="),
        NEQ("<>"),
        LT("<"),
        LTE("<="),
        GT(">"),
        GTE(">=");

        private final String operator;

        Comparison(String operator) {
            this.operator = operator;
        }

        /** The comparison operator, valid in both JPQL and SQL. */
        public String operator() {
            return operator;
        }
    }

    private static final String COMPARISONS =
            Arrays.stream(Comparison.values()).map(Enum::name).collect(Collectors.joining("|"));

    /**
     * The instrument ID is matched greedily, so it may contain {@code :} itself. Six digits at most
     * keep the count within {@code int} range.
     */
    private static final Pattern FORMAT = Pattern.compile("(.+):(" + COMPARISONS + "):(\\d{1,6})");

    /**
     * @throws ValidationException if {@code value} isn't {@code <instrumentId>:<comparison>:<parts>}
     *     with a known comparison and a non-negative whole number of parts
     */
    public static PartCountFilter fromParam(String value) {
        Matcher matcher = value == null ? null : FORMAT.matcher(value);
        if (matcher == null || !matcher.matches() || matcher.group(1).isBlank()) {
            throw new ValidationException("Invalid partCount '" + value
                    + "': expected <instrumentId>:<comparison>:<parts>, comparison one of "
                    + COMPARISONS.replace('|', ',') + ", parts a whole number from 0");
        }
        return new PartCountFilter(
                matcher.group(1), Comparison.valueOf(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }
}
