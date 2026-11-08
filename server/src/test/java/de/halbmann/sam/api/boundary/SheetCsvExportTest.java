package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** A title that looks like a spreadsheet formula leaves the CSV export as text. */
@QuarkusTest
@TestSecurity(
        user = "musician1",
        roles = {})
class SheetCsvExportTest {

    @Inject
    SheetService sheetService;

    private String sheetId;

    @AfterEach
    void deleteSheet() {
        sheetService.deleteSheet(sheetId);
    }

    @Test
    void formulaInTitle_isExportedAsText() {
        CreateSheetMusic sheet = ObjectMother.createSimpleSheetMusic();
        sheet.setTitle("=HYPERLINK(\"https://example.org\",\"Open\")");
        sheet.setSubtitle("-1+2");
        sheetId = sheetService.addSheet(sheet).getId().toString();

        String csv = given().queryParam("format", "CSV")
                .get("/api/sheets/{id}/export", sheetId)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        String row = csv.lines().skip(1).findFirst().orElseThrow();
        assertEquals(
                sheetId + ",\"'=HYPERLINK(\"\"https://example.org\"\",\"\"Open\"\")\",'-1+2,",
                row.substring(0, row.indexOf(",'-1+2,") + ",'-1+2,".length()));
    }
}
