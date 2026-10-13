package de.halbmann.sam.business.sheets.entity;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.core.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InstrumentationCountCriterionTest {

    @Test
    void parse_readsIdOperatorAndCount() {
        assertEquals(
                new InstrumentationCountCriterion("HORN_F", InstrumentationCountOperator.LTE, 4),
                InstrumentationCountCriterion.parse("HORN_F:LTE:4"));
    }

    @ParameterizedTest
    @EnumSource(InstrumentationCountOperator.class)
    void parse_acceptsEveryOperator(InstrumentationCountOperator operator) {
        assertEquals(
                operator,
                InstrumentationCountCriterion.parse("OBOE:" + operator.name() + ":1")
                        .operator());
    }

    @Test
    void parse_treatsBangAsPartOfTheId() {
        assertEquals("!OBOE", InstrumentationCountCriterion.parse("!OBOE:GTE:1").instrumentId());
    }

    @Test
    void parse_instrumentIdMayContainColons() {
        assertEquals("A:B", InstrumentationCountCriterion.parse("A:B:EQ:1").instrumentId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void parse_rejectsBlankValue(String raw) {
        assertThrows(ValidationException.class, () -> InstrumentationCountCriterion.parse(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HORN", "HORN:EQ", ":EQ:1", " :EQ:1"})
    void parse_rejectsMissingParts(String raw) {
        assertThrows(ValidationException.class, () -> InstrumentationCountCriterion.parse(raw));
    }

    @Test
    void parse_rejectsUnknownOperator() {
        assertThrows(ValidationException.class, () -> InstrumentationCountCriterion.parse("HORN:NE:1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HORN:EQ:many", "HORN:EQ:", "HORN:EQ:4:"})
    void parse_rejectsNonNumericOrMissingCount(String raw) {
        assertThrows(ValidationException.class, () -> InstrumentationCountCriterion.parse(raw));
    }

    @Test
    void parse_rejectsNegativeCount() {
        assertThrows(ValidationException.class, () -> InstrumentationCountCriterion.parse("HORN:EQ:-1"));
    }

    @Test
    void toCondition_comparesCountExpressionWithParameter() {
        assertEquals("c <> :n", InstrumentationCountCriterion.parse("X:NEQ:1").toCondition("c", "n"));
    }
}
