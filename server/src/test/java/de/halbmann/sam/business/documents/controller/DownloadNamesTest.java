package de.halbmann.sam.business.documents.controller;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.documents.AttachmentType;
import de.halbmann.sam.api.entity.instruments.InstrumentTransposing;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Naming rules of {@link DownloadNames}, without a database. */
class DownloadNamesTest {

    private final DownloadNames names = new DownloadNames();

    private static SheetMusicEntity sheet(String title) {
        SheetMusicEntity sheet = new SheetMusicEntity();
        sheet.setTitle(title);
        return sheet;
    }

    private static InstrumentationEntity instrumentation(String name, String displayName, String partLabel) {
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setName(name);
        instrument.setDisplayName(displayName);
        InstrumentationEntity instrumentation = new InstrumentationEntity();
        instrumentation.setInstrument(instrument);
        instrumentation.setPartLabel(partLabel);
        return instrumentation;
    }

    private static AttachmentEntity attachment(AttachmentType type, String filename, String mimeType) {
        DocumentEntity document = new DocumentEntity();
        document.setFilename(filename);
        document.setMimeType(mimeType);
        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setType(type);
        attachment.setDocument(document);
        return attachment;
    }

    @Test
    void part_isTitleAndInstrument_withTheOriginalExtension() {
        assertEquals(
                "1000 Träume weit – Tenorhorn in B 1.pdf",
                names.forInstrumentation(
                        sheet("1000 Träume weit"),
                        instrumentation("Tenorhorn", "Tenorhorn in B", "1"),
                        attachment(AttachmentType.PART, "scan0001.PDF", "application/pdf"),
                        Locale.GERMAN));
    }

    @Test
    void transposition_isPartOfTheName_soSameNamedInstrumentsDiffer() {
        InstrumentationEntity inC = instrumentation("Bariton", null, "1.");
        inC.getInstrument().setTransposition(InstrumentTransposing.C);
        InstrumentationEntity inBb = instrumentation("Bariton", null, "1.");
        inBb.getInstrument().setTransposition(InstrumentTransposing.Bb);
        AttachmentEntity part = attachment(AttachmentType.PART, "a.pdf", "application/pdf");

        assertEquals("Polka – Bariton (C) 1.pdf", names.forInstrumentation(sheet("Polka"), inC, part, Locale.GERMAN));
        assertEquals("Polka – Bariton (Bb) 1.pdf", names.forInstrumentation(sheet("Polka"), inBb, part, Locale.GERMAN));
    }

    @Test
    void instrumentWithoutDisplayNameOrPart_usesItsName() {
        assertEquals(
                "Polka – Tuba.pdf",
                names.forInstrumentation(
                        sheet("Polka"),
                        instrumentation("Tuba", null, null),
                        attachment(null, "x.pdf", "application/pdf"),
                        Locale.GERMAN));
    }

    @Test
    void nonPartOfAnInstrument_addsTheType() {
        assertEquals(
                "Polka – Tuba (Audio).mp3",
                names.forInstrumentation(
                        sheet("Polka"),
                        instrumentation("Tuba", null, null),
                        attachment(AttachmentType.AUDIO, "rec.mp3", "audio/mpeg"),
                        Locale.GERMAN));
    }

    @ParameterizedTest
    @CsvSource({
        "FULL_SCORE, de, Polka – Partitur.pdf",
        "FULL_SCORE, en, Polka – Full Score.pdf",
        "COVER, de, Polka – Titelblatt.pdf",
        "UNSPECIFIED, de, Polka.pdf"
    })
    void sheetDocument_isTitleAndType(AttachmentType type, String language, String expected) {
        assertEquals(
                expected,
                names.forSheet(
                        sheet("Polka"),
                        attachment(type, "download-1.pdf", "application/pdf"),
                        Locale.forLanguageTag(language)));
    }

    @Test
    void sheetDocumentWithoutType_isTheTitle() {
        assertEquals("Polka.pdf", names.forSheet(sheet("Polka"), attachment(null, "a.pdf", null), Locale.GERMAN));
    }

    @Test
    void extension_fallsBackToTheMimeType() {
        assertEquals(
                "Polka – Partitur.pdf",
                names.forSheet(
                        sheet("Polka"),
                        attachment(AttachmentType.FULL_SCORE, "no extension", "application/pdf"),
                        Locale.GERMAN));
    }

    @Test
    void invalidCharacters_areReplaced_umlautsKept() {
        assertEquals(
                "AC_DC Medley_ Teil 2 – Flöte 1.pdf",
                names.forInstrumentation(
                        sheet("AC/DC Medley: Teil 2"),
                        instrumentation("Flöte", null, "1"),
                        attachment(AttachmentType.PART, "f.pdf", "application/pdf"),
                        Locale.GERMAN));
    }

    @Test
    void bundle_isTitlePlusInstrumentOrType() {
        assertEquals(
                "Polka – Tuba",
                names.forBundle(sheet("Polka"), instrumentation("Tuba", null, null), null, Locale.GERMAN));
        assertEquals("Polka – Stimme", names.forBundle(sheet("Polka"), null, AttachmentType.PART, Locale.GERMAN));
        assertEquals("Polka", names.forBundle(sheet("Polka"), null, null, Locale.GERMAN));
    }

    @Test
    void locale_takesTheFirstSupportedLanguage_elseEnglish() {
        assertEquals(Locale.GERMAN, DownloadNames.locale(List.of(Locale.FRENCH, Locale.forLanguageTag("de-AT"))));
        assertEquals(Locale.ENGLISH, DownloadNames.locale(List.of(Locale.forLanguageTag("en-GB"), Locale.GERMAN)));
        assertEquals(Locale.ENGLISH, DownloadNames.locale(List.of(Locale.FRENCH)));
        assertEquals(Locale.ENGLISH, DownloadNames.locale(null));
    }

    @Test
    void safe_dropsTrailingDotsAndFallsBackWhenEmpty() {
        assertEquals("Ende", DownloadNames.safe("Ende...", "download"));
        assertEquals("download", DownloadNames.safe("  ", "download"));
    }

    /** The server's type labels must say what the UI says ({@code uploads.attachmentTypes}). */
    @ParameterizedTest
    @CsvSource({"de", "en"})
    void typeLabels_matchTheUi(String language) throws Exception {
        Path json = Path.of("..", "ui", "src", "main", "webui", "public", "i18n", language + ".json");
        JsonObject ui;
        try (Reader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            ui = Json.createReader(reader).readObject().getJsonObject("uploads").getJsonObject("attachmentTypes");
        }
        Map<AttachmentType, String> server = DownloadNames.AttachmentTypeLabels.labels(Locale.forLanguageTag(language));
        for (AttachmentType type : AttachmentType.values()) {
            assertEquals(ui.getString(type.name(), null), server.get(type), type + " in " + language);
        }
    }
}
