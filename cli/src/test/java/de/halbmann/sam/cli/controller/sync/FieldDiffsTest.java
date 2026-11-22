package de.halbmann.sam.cli.controller.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.instruments.Instrument;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FieldDiffsTest {

    private static final List<String> FIELDS = List.of("title", "publisher", "edition", "composer", "tempo");

    @Test
    void reportsOnlyFieldsTheFileHasAValueFor() {
        SheetMusic file = sheet("Garten-Polka", "Verlag A");
        file.setEdition("  "); // blank: unknown, not "clear"
        SheetMusic target = sheet("Garten-Polka", "Verlag B");
        target.setEdition("2. Auflage");
        target.setTempo(120);

        List<FieldDiff> diffs = FieldDiffs.compare(file, target, FIELDS);

        assertEquals(List.of(new FieldDiff("publisher", "Verlag A", "Verlag B")), diffs);
        assertEquals("publisher: \"Verlag B\" → \"Verlag A\"", diffs.getFirst().toString());
    }

    @Test
    void reportShowsLineBreaksInsideAValueOnOneLine() {
        SheetMusic file = sheet("Garten-Polka", "Verlag A");
        file.setAdditionalNotes("Rückseite: Am Bach\nRückseite: Im Wald");
        SheetMusic target = sheet("Garten-Polka", "Verlag A");

        assertEquals(
                "additionalNotes: (empty) → \"Rückseite: Am Bach\\nRückseite: Im Wald\"",
                FieldDiffs.compare(file, target, List.of("additionalNotes"))
                        .getFirst()
                        .toString());
    }

    @Test
    void comparesStringsStrippedAndPersonsByName() {
        SheetMusic file = sheet(" Garten-Polka ", "Verlag A");
        file.setComposer(person(null, "erika muster"));
        SheetMusic target = sheet("Garten-Polka", "Verlag A");
        target.setComposer(person(UUID.randomUUID(), "Erika Muster"));

        assertTrue(FieldDiffs.compare(file, target, FIELDS).isEmpty());
    }

    @Test
    void applyWritesOnlyTheDifferingFields() {
        SheetMusic file = sheet("Garten-Polka", " Verlag A ");
        file.setComposer(person(null, "Erika Muster"));
        SheetMusic target = sheet("Garten-Polka", "Verlag B");
        target.setEdition("2. Auflage");
        target.setComposer(person(UUID.randomUUID(), "Max Muster"));

        FieldDiffs.apply(FieldDiffs.compare(file, target, FIELDS), target);

        assertEquals("Verlag A", target.getPublisher(), "stripped");
        assertEquals("2. Auflage", target.getEdition(), "a field the file leaves empty keeps its value");
        assertEquals("Erika Muster", target.getComposer().getName());
        assertNull(target.getComposer().getId(), "by name, so the server resolves the person");
    }

    @Test
    void comparesAcrossClassesByPropertyName() {
        CreateInstrument file = new CreateInstrument();
        file.setId("FLOETE_C");
        file.setDisplayName("Flöte in C");
        Instrument target = new Instrument();
        target.setId("FLOETE_C");
        target.setDisplayName("Flöte");

        List<FieldDiff> diffs = FieldDiffs.compare(file, target, List.of("displayName", "family", "aliases"));

        assertEquals(List.of(new FieldDiff("displayName", "Flöte in C", "Flöte")), diffs);
    }

    private static SheetMusic sheet(String title, String publisher) {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle(title);
        sheet.setPublisher(publisher);
        return sheet;
    }

    private static Person person(UUID id, String name) {
        Person person = new Person();
        person.setId(id);
        person.setName(name);
        return person;
    }
}
