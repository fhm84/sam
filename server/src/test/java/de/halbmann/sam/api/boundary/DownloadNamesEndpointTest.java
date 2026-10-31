package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.shares.ShareType;
import de.halbmann.sam.business.shares.boundary.ShareRepository;
import de.halbmann.sam.business.shares.entity.ShareEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Downloads are named after the sheet and the instrument (or the document type) in the app's
 * language, for single downloads, share links and ZIP entries alike.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class DownloadNamesEndpointTest {

    @Inject
    ShareRepository shareRepository;

    private String title;
    private String sheetId;
    private String instrumentationId;
    private String partId;
    private String scoreId;
    private String instrumentId;

    @BeforeEach
    void createSheetWithPartAndScore() {
        title = "Böhmischer Traum " + UUID.randomUUID().toString().substring(0, 8);
        instrumentId =
                "TENORHORN_DN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        given().contentType(ContentType.JSON)
                .body(Map.of("id", instrumentId, "name", "Tenorhorn", "displayName", "Tenorhorn in B"))
                .post("/api/instruments")
                .then()
                .statusCode(200);
        sheetId = given().contentType(ContentType.JSON)
                .body(Map.of("title", title))
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        given().contentType(ContentType.JSON)
                .body(Map.of("instrumentId", instrumentId, "partLabel", "1"))
                .post("/api/sheets/{id}/instrumentations", sheetId)
                .then()
                .statusCode(204);
        instrumentationId = given().get("/api/sheets/{id}/instrumentations", sheetId)
                .then()
                .statusCode(200)
                .extract()
                .path("[0].id");
        partId = upload("/api/sheets/" + sheetId + "/instrumentations/" + instrumentationId + "/documents", "PART");
        scoreId = upload("/api/sheets/" + sheetId + "/documents", "FULL_SCORE");
    }

    @AfterEach
    void deleteSheetAndInstrument() {
        given().delete("/api/sheets/{id}", sheetId);
        given().delete("/api/instruments/{id}", instrumentId);
    }

    private static String upload(String path, String type) {
        byte[] pdf = ("%PDF-1.4\n% " + UUID.randomUUID() + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);
        return given().multiPart("file", "scan0001.pdf", pdf, "application/pdf")
                .multiPart("type", type)
                .post(path)
                .then()
                .statusCode(200)
                .extract()
                .path("attachment.id");
    }

    private static String filename(Response response) {
        String disposition = response.header("Content-Disposition");
        assertNotNull(disposition, "no Content-Disposition");
        String encoded = disposition.substring(disposition.indexOf("''") + 2);
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    @Test
    void part_isNamedAfterTitleAndInstrument() {
        Response response = given().header("Accept-Language", "de")
                .get("/api/sheets/{s}/instrumentations/{i}/documents/{a}", sheetId, instrumentationId, partId);

        assertEquals(200, response.statusCode());
        assertEquals(title + " – Tenorhorn in B 1.pdf", filename(response));
        assertEquals("Accept-Language", response.header("Vary"), "cached names must not cross languages");
    }

    @Test
    void sheetDocument_isNamedAfterTitleAndType_inTheAppLanguage() {
        assertEquals(
                title + " – Partitur.pdf",
                filename(given().header("Accept-Language", "de")
                        .get("/api/sheets/{s}/documents/{a}", sheetId, scoreId)));
        assertEquals(
                title + " – Full Score.pdf",
                filename(given().header("Accept-Language", "en")
                        .get("/api/sheets/{s}/documents/{a}", sheetId, scoreId)));
    }

    @Test
    void topLevelDownloadOfAnAttachment_findsItsSheet() {
        assertEquals(
                title + " – Partitur.pdf",
                filename(given().header("Accept-Language", "de").get("/api/documents/{a}", scoreId)));
    }

    @Test
    void zipEntries_carryTheSameNames() throws Exception {
        byte[] zip = given().contentType(ContentType.JSON)
                .header("Accept-Language", "de")
                .body(Map.of("ids", List.of(scoreId, partId), "format", "ZIP", "baseName", title))
                .post("/api/documents/batch")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();

        assertEquals(List.of(title + " – Partitur.pdf", title + " – Tenorhorn in B 1.pdf"), entryNames(zip));
    }

    @Test
    void shareLinkDownload_isNamedTheSameWay() {
        UUID token = QuarkusTransaction.requiringNew().call(() -> {
            ShareEntity share = new ShareEntity();
            share.setCreatorUserId("librarian1");
            share.setResourceType(ShareType.SHEET);
            share.setResourceId(UUID.fromString(sheetId));
            shareRepository.persist(share);
            return share.getId();
        });

        Response response =
                given().header("Accept-Language", "de").get("/api/public/shares/{t}/sheet/{a}", token, scoreId);

        assertEquals(200, response.statusCode());
        assertEquals(title + " – Partitur.pdf", filename(response));
    }

    private static List<String> entryNames(byte[] zip) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                names.add(e.getName());
            }
        }
        return names;
    }
}
