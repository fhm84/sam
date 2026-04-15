package de.halbmann.sam.business.documents.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertSame;

import de.halbmann.sam.api.entity.documents.DocumentDownload;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.Style;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class PdfMetadataEnricherTest {

    private final PdfMetadataEnricher enricher = new PdfMetadataEnricher();

    // ── helpers ─────────────────────────────────────────────────────────────

    private static byte[] minimalPdf() throws IOException {
        try (PDDocument doc = new PDDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static DocumentDownload downloadFor(byte[] bytes, String mimeType) {
        return new DocumentDownload(
                new ByteArrayInputStream(bytes), UUID.randomUUID(), "test.pdf", bytes.length, mimeType, "deadbeef");
    }

    private static PDDocumentInformation readInfo(byte[] bytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return doc.getDocumentInformation();
        }
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        try (stream) {
            return stream.readAllBytes();
        }
    }

    private static SheetMusicEntity sheet(String title) {
        SheetMusicEntity s = new SheetMusicEntity();
        s.setTitle(title);
        return s;
    }

    private static MusicianEntity musician(String name) {
        MusicianEntity m = new MusicianEntity();
        m.setName(name);
        return m;
    }

    private static InstrumentEntity instrument(String name, String displayName) {
        InstrumentEntity i = new InstrumentEntity();
        i.setId(name);
        i.setName(name);
        i.setDisplayName(displayName);
        return i;
    }

    private static InstrumentationEntity instrumentation(InstrumentEntity instrument, String partLabel) {
        InstrumentationEntity ie = new InstrumentationEntity();
        ie.setInstrument(instrument);
        ie.setPartLabel(partLabel);
        return ie;
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test
    void nonPdfIsReturnedUnchanged() {
        DocumentDownload original = downloadFor(new byte[] {1, 2, 3}, "image/png");
        DocumentDownload result = enricher.enrich(original, sheet("Anything"), null);
        assertSame(original, result, "non-PDF download must be returned as-is");
    }

    @Test
    void titleIsInjectedFromSheet() throws IOException {
        byte[] pdf = minimalPdf();
        DocumentDownload result = enricher.enrich(downloadFor(pdf, "application/pdf"), sheet("Polka ins Glück"), null);
        PDDocumentInformation info = readInfo(readAll(result.stream()));
        assertThat(info.getTitle(), is("Polka ins Glück"));
    }

    @Test
    void authorContainsBothComposerAndArranger() throws IOException {
        SheetMusicEntity s = sheet("Test");
        s.setComposer(musician("Anton Gälle"));
        s.setArranger(musician("Mathias Gronert"));

        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), s, null);
        PDDocumentInformation info = readInfo(readAll(result.stream()));
        assertThat(info.getAuthor(), is("Anton Gälle, arr. Mathias Gronert"));
    }

    @Test
    void authorIsComposerAloneWhenNoArranger() throws IOException {
        SheetMusicEntity s = sheet("Test");
        s.setComposer(musician("Anton Gälle"));

        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), s, null);
        PDDocumentInformation info = readInfo(readAll(result.stream()));
        assertThat(info.getAuthor(), is("Anton Gälle"));
        assertThat(info.getAuthor(), not(containsString("arr.")));
    }

    @Test
    void subjectIsInstrumentNameWithPartLabel() throws IOException {
        SheetMusicEntity s = sheet("Test");
        InstrumentationEntity ie = instrumentation(instrument("Trumpet", "Trompete"), "2");

        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), s, ie);
        PDDocumentInformation info = readInfo(readAll(result.stream()));
        assertThat(info.getSubject(), is("Trompete 2"));
    }

    @Test
    void subjectIsInstrumentNameOnlyWhenPartLabelIsNull() throws IOException {
        SheetMusicEntity s = sheet("Test");
        InstrumentationEntity ie = instrumentation(instrument("Flute", "Flöte"), null);

        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), s, ie);
        PDDocumentInformation info = readInfo(readAll(result.stream()));
        assertThat(info.getSubject(), is("Flöte"));
    }

    @Test
    void subjectFallsBackToGenreAndStyleAtSheetLevel() throws IOException {
        SheetMusicEntity s = sheet("Test");
        s.setGenre(Genre.MARCH);
        s.setStyle(Style.TRADITIONAL);

        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), s, null);
        PDDocumentInformation info = readInfo(readAll(result.stream()));
        assertThat(info.getSubject(), is("MARCH, TRADITIONAL"));
    }

    @Test
    void keywordsIncludeTagsIswcAndGema() throws IOException {
        SheetMusicEntity s = sheet("Test");
        s.setTags(Set.of("christmas", "outdoor"));
        s.setIswc("T-123456789-0");
        s.setGemaWorkNumber("12345678");

        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), s, null);
        String keywords = readInfo(readAll(result.stream())).getKeywords();

        assertThat(Arrays.asList(keywords.split(", ")), hasItems("christmas", "outdoor"));
        assertThat(keywords, containsString("ISWC:T-123456789-0"));
        assertThat(keywords, containsString("GEMA:12345678"));
    }

    @Test
    void creatorIsAlwaysSetToSam() throws IOException {
        // no sheet context at all
        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), null, null);
        PDDocumentInformation info = readInfo(readAll(result.stream()));
        assertThat(info.getCreator(), is("SAM"));
    }

    @Test
    void noMetadataInjectedWhenSheetIsNull() throws IOException {
        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), null, null);
        PDDocumentInformation info = readInfo(readAll(result.stream()));
        assertThat(info.getTitle(), nullValue());
        assertThat(info.getAuthor(), nullValue());
        assertThat(info.getSubject(), nullValue());
        assertThat(info.getKeywords(), nullValue());
    }

    @Test
    void returnedSizeMatchesActualEnrichedByteLength() throws IOException {
        SheetMusicEntity s = sheet("Size Check");
        s.setComposer(musician("Some Composer"));

        DocumentDownload result = enricher.enrich(downloadFor(minimalPdf(), "application/pdf"), s, null);
        byte[] enrichedBytes = readAll(result.stream());
        assertThat(result.size(), is((long) enrichedBytes.length));
    }

    @Test
    void corruptedPdfFallsBackGracefully() {
        byte[] garbage = new byte[] {0x25, 0x50, 0x44, 0x00, (byte) 0xFF}; // not a valid PDF
        DocumentDownload original = downloadFor(garbage, "application/pdf");

        DocumentDownload result = enricher.enrich(original, sheet("Test"), null);

        // no exception; size and checksum preserved from the original
        assertThat(result.size(), is(original.size()));
        assertThat(result.checksumSha256(), is(original.checksumSha256()));
    }
}
