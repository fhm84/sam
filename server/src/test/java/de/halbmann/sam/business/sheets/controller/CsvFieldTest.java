package de.halbmann.sam.business.sheets.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class CsvFieldTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            quoteCharacter = '`',
            value = {
                "=1+2|'=1+2",
                "+49 170|'+49 170",
                "-2|'-2",
                "@SUM(A1)|'@SUM(A1)",
                "`=HYPERLINK(\"x\",\"y\")`|`\"'=HYPERLINK(\"\"x\"\",\"\"y\"\")\"`",
                "Böhmischer Traum|Böhmischer Traum",
                "Marsch - Trio|Marsch - Trio",
                "a, b|`\"a, b\"`",
                "say \"hi\"|`\"say \"\"hi\"\"\"`"
            })
    void formulasBecomeText_andSpecialCharactersAreQuoted(String value, String expected) {
        assertEquals(expected, SheetExportService.csvField(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void missingValue_isAnEmptyCell(String value) {
        assertEquals("", SheetExportService.csvField(value));
    }

    @Test
    void tabsAndLineBreaks_atTheStart() {
        assertEquals("'\tx", SheetExportService.csvField("\tx"));
        assertEquals("\"'\rx\"", SheetExportService.csvField("\rx"));
        assertEquals("\"a\nb\"", SheetExportService.csvField("a\nb"));
    }
}
