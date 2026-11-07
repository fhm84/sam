package de.halbmann.sam.business.documents.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.core.exception.ConflictException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
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
        StreamWriter writer =
                DocumentBundleService.doMerge(entries, path -> new ByteArrayInputStream(content.get(path)));
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
        StreamWriter writer = DocumentBundleService.doMerge(entries, path -> null);
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

    @Test
    void theSameFileCanBeMergedTwice() throws IOException {
        byte[] p1 = pdfWithPages(1);
        List<MergedPdfEntry> entries = List.of(pdfEntry("a.pdf", "One", p1), pdfEntry("a.pdf", "Two", p1));

        try (PDDocument result = Loader.loadPDF(merge(entries, Map.of("a.pdf", p1)))) {
            assertThat(result.getNumberOfPages(), is(2));
        }
    }

    /**
     * Sources are closed and deleted as soon as they are appended, so the result must not depend on
     * them any more: text with its font and page images have to be fully in the merged file.
     */
    @Test
    void mergedFileHoldsTheFullContentOfItsSources() throws IOException {
        byte[] first = pdfWithTextAndImage("Erste Stimme", Color.RED);
        byte[] second = pdfWithTextAndImage("Zweite Stimme", Color.BLUE);

        byte[] merged = merge(
                List.of(pdfEntry("a.pdf", "A", first), pdfEntry("b.pdf", "B", second)),
                Map.of("a.pdf", first, "b.pdf", second));

        try (PDDocument result = Loader.loadPDF(merged)) {
            assertThat(result.getNumberOfPages(), is(2));
            String text = new PDFTextStripper().getText(result);
            assertThat(text, containsString("Erste Stimme"));
            assertThat(text, containsString("Zweite Stimme"));
            assertThat(firstImageColor(result, 0), is(Color.RED.getRGB()));
            assertThat(firstImageColor(result, 1), is(Color.BLUE.getRGB()));
        }
    }

    private static byte[] pdfWithTextAndImage(String text, Color imageColor) throws IOException {
        try (PDDocument doc = new PDDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 40; x++) {
                for (int y = 0; y < 40; y++) {
                    image.setRGB(x, y, imageColor.getRGB());
                }
            }
            PDImageXObject xObject = LosslessFactory.createFromImage(doc, image);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.drawImage(xObject, 50, 50);
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 200);
                content.showText(text);
                content.endText();
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static int firstImageColor(PDDocument doc, int pageIndex) throws IOException {
        PDResources resources = doc.getPage(pageIndex).getResources();
        for (COSName name : resources.getXObjectNames()) {
            if (resources.getXObject(name) instanceof PDImageXObject image) {
                return image.getImage().getRGB(0, 0);
            }
        }
        throw new AssertionError("no image on page " + pageIndex);
    }

    @Test
    void waitsForAFreeSlotAndGivesUpWhenNoneFreesUp() throws IOException {
        byte[] p1 = pdfWithPages(1);
        Semaphore noFreeSlot = new Semaphore(0);

        StreamWriter writer = DocumentBundleService.doMerge(
                List.of(pdfEntry("a.pdf", "A", p1)),
                path -> new ByteArrayInputStream(p1),
                noFreeSlot,
                Duration.ofMillis(50));

        assertThrows(ConflictException.class, () -> writer.write(new ByteArrayOutputStream()));
        assertThat("a refused merge must not take a slot", noFreeSlot.availablePermits(), is(0));
    }

    @Test
    void releasesItsSlotWhenAFileIsBroken() {
        byte[] broken = "not a pdf".getBytes(StandardCharsets.UTF_8);
        Semaphore slots = new Semaphore(1);

        StreamWriter writer = DocumentBundleService.doMerge(
                List.of(pdfEntry("bad.pdf", "Bad", broken)),
                path -> new ByteArrayInputStream(broken),
                slots,
                Duration.ofMillis(50));

        assertThrows(IOException.class, () -> writer.write(new ByteArrayOutputStream()));
        assertThat(slots.availablePermits(), is(1));
    }

    @Test
    void leavesNoTemporaryFilesBehind() throws IOException {
        byte[] p1 = pdfWithPages(1);
        List<String> before = temporaryCopies();

        merge(List.of(pdfEntry("a.pdf", "A", p1), pdfEntry("b.pdf", "B", p1)), Map.of("a.pdf", p1, "b.pdf", p1));

        assertThat(temporaryCopies(), is(before));
    }

    @Test
    void leavesNoTemporaryFilesBehindWhenAFileIsBroken() throws IOException {
        byte[] good = pdfWithPages(1);
        byte[] broken = "not a pdf".getBytes(StandardCharsets.UTF_8);
        List<String> before = temporaryCopies();

        assertThrows(
                IOException.class,
                () -> merge(
                        List.of(pdfEntry("good.pdf", "Good", good), pdfEntry("bad.pdf", "Bad", broken)),
                        Map.of("good.pdf", good, "bad.pdf", broken)));

        assertThat(temporaryCopies(), is(before));
    }

    private static List<String> temporaryCopies() throws IOException {
        try (Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return files.map(f -> f.getFileName().toString())
                    .filter(n -> n.startsWith("sam-merge-"))
                    .sorted()
                    .toList();
        }
    }
}
