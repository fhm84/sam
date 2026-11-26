package de.halbmann.sam.business.sheets.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.halbmann.sam.business.sheets.entity.PartCountFilter.Comparison;
import de.halbmann.sam.core.exception.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PartCountFilterTest {

    @ParameterizedTest
    @CsvSource({
        "HORN_F:EQ:4,    HORN_F,  EQ,  4",
        "OBOE:NEQ:0,     OBOE,    NEQ, 0",
        "TUBA:LT:2,      TUBA,    LT,  2",
        "TUBA:LTE:2,     TUBA,    LTE, 2",
        "TUBA:GT:12,     TUBA,    GT,  12",
        "TUBA:GTE:999999, TUBA,   GTE, 999999",
        // The ID is everything before the last two separators
        "PERC:SNARE:EQ:1, PERC:SNARE, EQ, 1",
        "!X:GTE:1,       !X,      GTE, 1",
    })
    void fromParam_splitsInstrumentComparisonAndParts(
            String value, String instrument, Comparison comparison, int parts) {
        assertEquals(new PartCountFilter(instrument, comparison, parts), PartCountFilter.fromParam(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                " ",
                "HORN",
                "HORN:EQ",
                "HORN:EQ:",
                ":EQ:1",
                "  :EQ:1",
                "HORN:eq:1",
                "HORN:NE:1",
                "HORN:EQ:-1",
                "HORN:EQ:1.5",
                "HORN:EQ:two",
                "HORN:EQ:4:",
                "HORN:EQ:1234567",
            })
    void fromParam_rejectsMalformedValues(String value) {
        assertThrows(ValidationException.class, () -> PartCountFilter.fromParam(value));
    }

    @ParameterizedTest
    @CsvSource({"EQ, =", "NEQ, <>", "LT, <", "LTE, <=", "GT, >", "GTE, >="})
    void comparison_rendersItsOperator(Comparison comparison, String operator) {
        assertEquals(operator, comparison.operator());
    }
}
