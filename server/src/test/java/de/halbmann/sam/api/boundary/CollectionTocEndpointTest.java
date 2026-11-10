package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.shares.ShareType;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.shares.boundary.ShareRepository;
import de.halbmann.sam.business.shares.entity.ShareEntity;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The table of contents really renders: both endpoints answer with a PDF that holds the collection's
 * content, parsed back with PDFBox. Characters that are special in XHTML or outside ASCII are part
 * of the data, since the template goes through an XML parser on its way to the PDF.
 */
@QuarkusTest
class CollectionTocEndpointTest {

    private static final String LIBRARIAN = "librarian1";
    private static final String TEXT_ITEM = "Grußwort des Vorsitzenden";

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetService sheetService;

    @Inject
    ShareRepository shareRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String collectionName = "Frühjahrskonzert & Märsche " + suffix;
    private final String firstTitle = "Böhmische Liebe " + suffix;
    private final String secondTitle = "Walzer & Polka " + suffix;
    private final List<String> sheetIds = new ArrayList<>();
    private String collectionId;
    private UUID shareToken;

    @BeforeEach
    void createCollection() {
        SheetCollection collection = new SheetCollection();
        collection.setName(collectionName);
        collection.setDescription("Programm für den Frühling");
        collection.setType(CollectionType.SETLIST);
        collection.setDate(LocalDate.of(2026, 4, 18));
        collectionId = collectionService.add(collection).getId().toString();

        addSheet(firstTitle, "Für Blasorchester", Duration.ofSeconds(200));
        addTextItem(TEXT_ITEM);
        addSheet(secondTitle, null, Duration.ofSeconds(245));
    }

    @AfterEach
    void deleteData() {
        if (shareToken != null) {
            QuarkusTransaction.requiringNew().run(() -> shareRepository.deleteById(shareToken));
        }
        collectionService.delete(collectionId);
        sheetIds.forEach(sheetService::deleteSheet);
    }

    private void addSheet(String title, String subtitle, Duration duration) {
        CreateSheetMusic sheet = ObjectMother.createSimpleSheetMusic();
        sheet.setTitle(title);
        sheet.setSubtitle(subtitle);
        sheet.setDuration(duration);
        String sheetId = sheetService.addSheet(sheet).getId().toString();
        sheetIds.add(sheetId);

        CreateCollectionItem item = new CreateCollectionItem();
        item.setType(CollectionItemType.SHEET);
        item.setSheetId(UUID.fromString(sheetId));
        collectionService.addItem(collectionId, item);
    }

    private void addTextItem(String text) {
        CreateCollectionItem item = new CreateCollectionItem();
        item.setType(CollectionItemType.TEXT);
        item.setTextContent(text);
        collectionService.addItem(collectionId, item);
    }

    /** The parts of a rendered PDF the tests look at; line breaks in the text are spaces. */
    private record Pdf(int pages, String text) {}

    private static Pdf parsePdf(Response response) throws IOException {
        assertEquals(200, response.statusCode());
        assertEquals("application/pdf", response.contentType());
        try (PDDocument document = Loader.loadPDF(response.asByteArray())) {
            String text = new PDFTextStripper().getText(document);
            return new Pdf(document.getNumberOfPages(), text.replaceAll("\\s+", " "));
        }
    }

    private static void assertContains(Pdf pdf, String expected) {
        assertTrue(pdf.text().contains(expected), () -> "'" + expected + "' not in: " + pdf.text());
    }

    /**
     * How often {@code value} is printed, ignoring whitespace: text extraction may break or space
     * out a large, wrapped heading differently from the source.
     */
    private static int occurrences(Pdf pdf, String value) {
        String text = pdf.text().replaceAll("\\s+", "");
        return text.split(Pattern.quote(value.replaceAll("\\s+", "")), -1).length - 1;
    }

    @Test
    @TestSecurity(
            user = LIBRARIAN,
            roles = {"music_librarian"})
    void toc_rendersTheCollectionsContent() throws IOException {
        Pdf pdf = parsePdf(given().get("/api/sheet-collections/{id}/toc", collectionId));

        assertEquals(1, pdf.pages());
        assertContains(pdf, collectionName);
        assertContains(pdf, "Programm für den Frühling");
        assertContains(pdf, "SETLIST");
        assertContains(pdf, "2026-04-18");
        assertContains(pdf, firstTitle);
        assertContains(pdf, "Für Blasorchester");
        assertContains(pdf, "3:20");
        assertContains(pdf, secondTitle);
        assertContains(pdf, "4:05");
        assertContains(pdf, TEXT_ITEM);
        assertContains(pdf, "Total");
        assertContains(pdf, "7:25");
    }

    @Test
    @TestSecurity(
            user = LIBRARIAN,
            roles = {"music_librarian"})
    void toc_canLeaveOutTextItems() throws IOException {
        Pdf pdf = parsePdf(
                given().queryParam("includeTextItems", false).get("/api/sheet-collections/{id}/toc", collectionId));

        assertContains(pdf, firstTitle);
        assertFalse(pdf.text().contains(TEXT_ITEM), "text item still rendered");
    }

    @Test
    @TestSecurity(
            user = LIBRARIAN,
            roles = {"music_librarian"})
    void toc_longCollection_continuesOverPagesWithPageNumbers() throws IOException {
        for (int i = 1; i <= 60; i++) {
            addTextItem("Zwischentext " + i + " mit etwas Inhalt");
        }

        Pdf pdf = parsePdf(given().get("/api/sheet-collections/{id}/toc", collectionId));

        assertTrue(pdf.pages() > 1, "expected several pages, got " + pdf.pages());
        assertContains(pdf, "Zwischentext 60 ");
        for (int page = 1; page <= pdf.pages(); page++) {
            assertContains(pdf, page + " / " + pdf.pages());
        }
        // In the heading once, and in the footer of every page (the document title is not printed)
        assertEquals(pdf.pages() + 1, occurrences(pdf, collectionName), pdf::text);
        // Below the table, not taken into the page footer
        assertEquals(1, occurrences(pdf, "Generated on"), pdf::text);
    }

    /** A name that would break out of a CSS string stays plain text in the PDF. */
    @Test
    @TestSecurity(
            user = LIBRARIAN,
            roles = {"music_librarian"})
    void toc_nameWithCssSyntax_isPrintedAsIs() throws IOException {
        String name =
                "x\"; } } body { background-image: url(\"http://127.0.0.1:9/x.png\"); } @page { content: \"y " + suffix;
        SheetCollection collection = new SheetCollection();
        collection.setName(name);
        String id = collectionService.add(collection).getId().toString();
        try {
            Pdf pdf = parsePdf(given().get("/api/sheet-collections/{id}/toc", id));

            // Heading and footer
            assertEquals(2, occurrences(pdf, name), pdf::text);
        } finally {
            collectionService.delete(id);
        }
    }

    @Test
    @TestSecurity(
            user = LIBRARIAN,
            roles = {"music_librarian"})
    void toc_collectionWithNothingButAName_stillRenders() throws IOException {
        SheetCollection bare = new SheetCollection();
        bare.setName("Leer " + suffix);
        String bareId = collectionService.add(bare).getId().toString();
        try {
            Pdf pdf = parsePdf(given().get("/api/sheet-collections/{id}/toc", bareId));

            assertContains(pdf, "Leer " + suffix);
            assertFalse(pdf.text().contains("Total"), "total without any duration");
        } finally {
            collectionService.delete(bareId);
        }
    }

    /** No {@code @TestSecurity}: the recipient of a share link has no login. */
    @Test
    void publicShare_rendersTheTocWithoutLogin() throws IOException {
        shareToken = QuarkusTransaction.requiringNew().call(() -> {
            ShareEntity share = new ShareEntity();
            share.setCreatorUserId(LIBRARIAN);
            share.setResourceType(ShareType.COLLECTION);
            share.setResourceId(UUID.fromString(collectionId));
            shareRepository.persist(share);
            return share.getId();
        });

        Response response = given().get("/api/public/shares/{token}/toc", shareToken);
        Pdf pdf = parsePdf(response);

        assertContains(pdf, collectionName);
        assertContains(pdf, firstTitle);
        assertContains(pdf, TEXT_ITEM);
        assertEquals("attachment; filename*=utf-8''collection-toc.pdf", response.header("Content-Disposition"));
    }
}
