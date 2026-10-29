package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.ensembles.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberContact;
import de.halbmann.sam.api.entity.members.MembershipStatus;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.business.ensembles.controller.EnsembleMembershipService;
import de.halbmann.sam.business.ensembles.controller.EnsembleService;
import de.halbmann.sam.business.members.controller.MemberService;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Member contact details, the linked login, invite timestamps and the onboarding state are visible
 * only to the music librarian and the admin — on {@code /members} and wherever a member is embedded
 * (an ensemble seat). A sheet's composer is a person, which carries no personal data for anyone.
 * Test data is created through the services, so it doesn't depend on the caller's write role.
 */
@QuarkusTest
class PersonalDataVisibilityTest {

    @Inject
    MemberService memberService;

    @Inject
    SheetService sheetService;

    @Inject
    EnsembleService ensembleService;

    @Inject
    EnsembleMembershipService membershipService;

    private static Member memberWithContact() {
        MemberContact contact = new MemberContact();
        contact.setEmail("player@example.org");
        contact.setMobile("+49 170 0000000");
        contact.setNotes("prefers 2nd part");
        Member member = new Member();
        member.setName("Contact Test " + UUID.randomUUID());
        member.setContact(contact);
        return member;
    }

    private String createMember() {
        return memberService.addMember(memberWithContact()).getId().toString();
    }

    private String createFormerMember() {
        Member member = memberWithContact();
        member.setStatus(MembershipStatus.FORMER);
        return memberService.addMember(member).getId().toString();
    }

    private String createSheetComposedByMember() {
        // A member who composes: the sheet references the member's person by ID
        Person composer = new Person();
        composer.setId(UUID.fromString(createMember()));
        CreateSheetMusic sheet = ObjectMother.createSheetMusicWithComposer(composer);
        sheet.setTitle("Contact Test " + UUID.randomUUID());
        return sheetService.addSheet(sheet).getId().toString();
    }

    private String createEnsembleWithMember() {
        CreateEnsemble ensemble = new CreateEnsemble();
        ensemble.setName("Contact Test " + UUID.randomUUID());
        String ensembleId = ensembleService.addEnsemble(ensemble).getId().toString();
        CreateEnsembleMembership seat = new CreateEnsembleMembership();
        seat.setMemberId(UUID.fromString(createMember()));
        membershipService.addMember(ensembleId, seat);
        return ensembleId;
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_loadMember_hasNoPersonalData() {
        given().get("/api/members/" + createMember())
                .then()
                .statusCode(200)
                .body("name", startsWith("Contact Test"))
                .body("status", equalTo("ACTIVE"))
                .body("contact", nullValue())
                .body("onboarding", nullValue());
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_listMembers_hasNoContact() {
        createMember();
        given().get("/api/members?name=Contact Test")
                .then()
                .statusCode(200)
                .body("data", not(empty()))
                .body("data.contact", everyItem(nullValue()));
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_ensembleSeat_memberHasNoContact() {
        given().get("/api/ensembles/" + createEnsembleWithMember() + "/members")
                .then()
                .statusCode(200)
                .body("[0].member.name", startsWith("Contact Test"))
                .body("[0].member.contact", nullValue());
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void plainUser_loadFormerMember_hasNoLeavingDate() {
        given().get("/api/members/" + createFormerMember())
                .then()
                .statusCode(200)
                .body("status", equalTo("FORMER"))
                .body("leftAt", nullValue());
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_loadFormerMember_seesLeavingDate() {
        given().get("/api/members/" + createFormerMember())
                .then()
                .statusCode(200)
                .body("leftAt", notNullValue());
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_loadMember_seesContact() {
        given().get("/api/members/" + createMember())
                .then()
                .statusCode(200)
                .body("contact.email", equalTo("player@example.org"))
                .body("contact.notes", equalTo("prefers 2nd part"))
                .body("onboarding", equalTo("NONE"));
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    void admin_loadSheet_composerCarriesNoPersonalData() {
        given().get("/api/sheets/" + createSheetComposedByMember())
                .then()
                .statusCode(200)
                .body("composer.name", startsWith("Contact Test"))
                .body("composer", not(hasKey("contact")))
                .body("composer", not(hasKey("userId")));
    }
}
