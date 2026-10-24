package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianContact;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.business.musicians.controller.MusicianService;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Musician contact details, the linked login and invite timestamps are visible only to the music
 * librarian and the admin — on {@code /musicians} and wherever a musician is embedded (a sheet's
 * composer). Test data is created through the services, so it doesn't depend on the caller's
 * write role.
 */
@QuarkusTest
class PersonalDataVisibilityTest {

    @Inject
    MusicianService musicianService;

    @Inject
    SheetService sheetService;

    private static Musician musicianWithContact() {
        MusicianContact contact = new MusicianContact();
        contact.setEmail("player@example.org");
        contact.setMobile("+49 170 0000000");
        contact.setNotes("prefers 2nd part");
        Musician musician = new Musician();
        musician.setName("Contact Test " + UUID.randomUUID());
        musician.setContact(contact);
        return musician;
    }

    private String createMusician() {
        return musicianService.addMusician(musicianWithContact()).getId().toString();
    }

    private String createSheetWithComposer() {
        // An inline composer only gets a name, so reference an existing musician by ID
        Musician composer = new Musician();
        composer.setId(UUID.fromString(createMusician()));
        CreateSheetMusic sheet = ObjectMother.createSheetMusicWithComposer(composer);
        sheet.setTitle("Contact Test " + UUID.randomUUID());
        return sheetService.addSheet(sheet).getId().toString();
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_loadMusician_hasNoContact() {
        given().get("/api/musicians/" + createMusician())
                .then()
                .statusCode(200)
                .body("name", startsWith("Contact Test"))
                .body("contact", nullValue());
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_listMusicians_hasNoContact() {
        createMusician();
        given().get("/api/musicians?name=Contact Test")
                .then()
                .statusCode(200)
                .body("data", not(empty()))
                .body("data.contact", everyItem(nullValue()));
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_loadSheet_composerHasNoContact() {
        given().get("/api/sheets/" + createSheetWithComposer())
                .then()
                .statusCode(200)
                .body("composer.name", startsWith("Contact Test"))
                .body("composer.contact", nullValue());
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_loadMusician_seesContact() {
        given().get("/api/musicians/" + createMusician())
                .then()
                .statusCode(200)
                .body("contact.email", equalTo("player@example.org"))
                .body("contact.notes", equalTo("prefers 2nd part"));
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    void admin_loadSheet_composerHasContact() {
        given().get("/api/sheets/" + createSheetWithComposer())
                .then()
                .statusCode(200)
                .body("composer.contact.mobile", equalTo("+49 170 0000000"));
    }
}
