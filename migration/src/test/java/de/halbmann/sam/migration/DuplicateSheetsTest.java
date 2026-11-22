package de.halbmann.sam.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DuplicateSheetsTest {

    @Test
    void mergesCopiesIntoTheFirstWithAllPartsAndNotes() {
        SheetMusic first = sheet("Garten-Polka", "Verlag A", "Rückseite: Am Bach", "1. Trompete in b", "1. Flöte in c");
        SheetMusic other = sheet("Schnee-Marsch", "Verlag A", null, "1. Trompete in b");
        SheetMusic second =
                sheet("garten-polka", "verlag a", "Rückseite: Im Wald", "1. Trompete in b", "2. Trompete in b");

        DuplicateSheets.Result result = DuplicateSheets.merge(List.of(first, other, second));

        assertEquals(List.of(first, other), result.sheets(), "merged sheet keeps the first copy's place");
        assertSame(first, result.sheets().getFirst());
        assertEquals(
                List.of("TROMPETE_BB 1.", "FLOETE_C 1.", "TROMPETE_BB 2."),
                parts(first),
                "union of parts, first copy's order, no duplicates");
        assertEquals("Rückseite: Am Bach\nRückseite: Im Wald", first.getAdditionalNotes());
        assertEquals(List.of("Garten-Polka (Verlag A): 2 copies, 3 parts"), result.merged());
    }

    @Test
    void keepsSheetsWithoutCopiesUntouched() {
        SheetMusic a = sheet("Garten-Polka", "Verlag A", "Rückseite: Am Bach", "1. Trompete in b");
        SheetMusic b = sheet("Garten-Polka", "Verlag B", null, "1. Trompete in b");

        DuplicateSheets.Result result = DuplicateSheets.merge(List.of(a, b));

        assertEquals(List.of(a, b), result.sheets(), "different publishers are different sheets");
        assertTrue(result.merged().isEmpty());
        assertEquals("Rückseite: Am Bach", a.getAdditionalNotes());
    }

    @Test
    void identicalNotesAndMissingNotesAreNotRepeated() {
        SheetMusic first = sheet("Garten-Polka", null, null, "1. Trompete in b");
        SheetMusic second = sheet("Garten-Polka", null, null, "1. Trompete in b");

        DuplicateSheets.merge(List.of(first, second));

        assertNull(first.getAdditionalNotes());
        assertEquals(1, first.getInstrumentations().size());
    }

    @Test
    void refusesToMergeCopiesThatDisagree() {
        SheetMusic first = sheet("Garten-Polka", "Verlag A", null, "1. Trompete in b");
        SheetMusic second = sheet("Garten-Polka", "Verlag A", null, "1. Trompete in b");
        Person composer = new Person();
        composer.setName("Erika Muster");
        second.setComposer(composer);
        second.setTags(Set.of("Fest"));

        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> DuplicateSheets.merge(List.of(first, second)));
        assertTrue(e.getMessage().contains("Garten-Polka"), e.getMessage());
    }

    private static SheetMusic sheet(String title, String publisher, String notes, String... parts) {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle(title);
        sheet.setPublisher(publisher);
        sheet.setAdditionalNotes(notes);
        sheet.setInstrumentations(new ArrayList<>(
                InstrumentationParser.parse(String.join(";", parts)).instrumentations()));
        return sheet;
    }

    private static List<String> parts(SheetMusic sheet) {
        return sheet.getInstrumentations().stream()
                .map(DuplicateSheetsTest::part)
                .toList();
    }

    private static String part(Instrumentation instrumentation) {
        return instrumentation.getInstrument().getId() + " " + instrumentation.getPartLabel();
    }
}
