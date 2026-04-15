package de.halbmann.sam.business.documents.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;

class MergedPdfBookmarksTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private static byte[] pdfWithPages(int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (int i = 0; i < pageCount; i++) doc.addPage(new PDPage());
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static MergedPdfEntry pdfEntry(String path, String label, byte[] content) {
        DocumentEntity doc = new DocumentEntity();
        doc.setPath(path);
        doc.setMimeType("application/pdf");
        AttachmentEntity att = new AttachmentEntity();
        att.setDocument(doc);
        att.setDisplayName(path);
        return new MergedPdfEntry(att, label);
    }

    private static MergedPdfEntry nonPdfEntry(String label) {
        DocumentEntity doc = new DocumentEntity();
        doc.setPath("image.png");
        doc.setMimeType("image/png");
        AttachmentEntity att = new AttachmentEntity();
        att.setDocument(doc);
        return new MergedPdfEntry(att, label);
    }

    private static List<String> bookmarkTitles(PDDocument doc) {
        PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
        if (outline == null) return List.of();
        List<String> titles = new ArrayList<>();
        PDOutlineItem item = outline.getFirstChild();
        while (item != null) {
            titles.add(item.getTitle());
            item = item.getNextSibling();
        }
        return titles;
    }

    private static byte[] merge(List<MergedPdfEntry> entries, Map<String, byte[]> content) throws IOException {
        StreamWriter writer = DocumentsService.doMerge(entries, path -> new ByteArrayInputStream(content.get(path)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(baos);
        return baos.toByteArray();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void eachPdfEntryGetsABookmark() throws IOException {
        byte[] p1 = pdfWithPages(1);
        byte[] p2 = pdfWithPages(2);

        List<MergedPdfEntry> entries =
                List.of(pdfEntry("p1.pdf", "Trumpet 2", p1), pdfEntry("p2.pdf", "Trombone 1", p2));

        try (PDDocument result = Loader.loadPDF(merge(entries, Map.of("p1.pdf", p1, "p2.pdf", p2)))) {
            assertThat(bookmarkTitles(result), contains("Trumpet 2", "Trombone 1"));
        }
    }

    @Test
    void bookmarkTitlesMatchEntryLabels() throws IOException {
        byte[] p1 = pdfWithPages(1);
        byte[] p2 = pdfWithPages(1);
        byte[] p3 = pdfWithPages(1);

        List<MergedPdfEntry> entries = List.of(
                pdfEntry("a.pdf", "Flöte 1", p1),
                pdfEntry("b.pdf", "Flöte 2", p2),
                pdfEntry("c.pdf", "Klarinette", p3));

        Map<String, byte[]> content = Map.of("a.pdf", p1, "b.pdf", p2, "c.pdf", p3);
        try (PDDocument result = Loader.loadPDF(merge(entries, content))) {
            assertThat(bookmarkTitles(result), contains("Flöte 1", "Flöte 2", "Klarinette"));
        }
    }

    @Test
    void nonPdfEntriesAreSkipped() throws IOException {
        byte[] p1 = pdfWithPages(1);

        List<MergedPdfEntry> entries = List.of(pdfEntry("doc.pdf", "Score", p1), nonPdfEntry("Cover Image"));

        try (PDDocument result = Loader.loadPDF(merge(entries, Map.of("doc.pdf", p1)))) {
            assertThat(result.getNumberOfPages(), is(1));
            assertThat(bookmarkTitles(result), contains("Score"));
        }
    }

    @Test
    void returnsNullWhenNoPdfEntriesPresent() {
        List<MergedPdfEntry> entries = List.of(nonPdfEntry("Cover"), nonPdfEntry("Notes"));
        StreamWriter writer = DocumentsService.doMerge(entries, path -> null);
        assertNull(writer, "doMerge should return null when no PDF entries are present");
    }

    @Test
    void totalPageCountMatchesSumOfSourcePages() throws IOException {
        byte[] p1 = pdfWithPages(3);
        byte[] p2 = pdfWithPages(2);

        List<MergedPdfEntry> entries = List.of(pdfEntry("p1.pdf", "Part A", p1), pdfEntry("p2.pdf", "Part B", p2));

        try (PDDocument result = Loader.loadPDF(merge(entries, Map.of("p1.pdf", p1, "p2.pdf", p2)))) {
            assertThat(result.getNumberOfPages(), is(5));
        }
    }

    @Test
    void bookmarksPointToDifferentPages() throws IOException {
        byte[] p1 = pdfWithPages(2); // pages 0-1
        byte[] p2 = pdfWithPages(1); // page 2

        List<MergedPdfEntry> entries = List.of(pdfEntry("p1.pdf", "First", p1), pdfEntry("p2.pdf", "Second", p2));

        try (PDDocument result = Loader.loadPDF(merge(entries, Map.of("p1.pdf", p1, "p2.pdf", p2)))) {
            PDDocumentOutline outline = result.getDocumentCatalog().getDocumentOutline();
            assertNotNull(outline);

            PDOutlineItem firstItem = outline.getFirstChild();
            PDOutlineItem secondItem = firstItem.getNextSibling();

            PDPageDestination dest1 = (PDPageDestination) firstItem.getDestination();
            PDPageDestination dest2 = (PDPageDestination) secondItem.getDestination();

            // Second bookmark must point to a different page than the first
            assertThat(dest2.getPage(), not(sameInstance(dest1.getPage())));
            // Second bookmark points to page index 2 (after the 2-page first doc)
            assertThat(result.getPages().indexOf(dest2.getPage()), is(2));
        }
    }

    @Test
    void singleEntryStillGetsOutlineWithOneItem() throws IOException {
        byte[] p1 = pdfWithPages(1);
        List<MergedPdfEntry> entries = List.of(pdfEntry("solo.pdf", "Solo Part", p1));

        try (PDDocument result = Loader.loadPDF(merge(entries, Map.of("solo.pdf", p1)))) {
            assertThat(bookmarkTitles(result), contains("Solo Part"));
        }
    }
}
