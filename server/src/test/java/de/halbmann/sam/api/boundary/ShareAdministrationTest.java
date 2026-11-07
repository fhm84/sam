package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.shares.ShareType;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.business.members.controller.MemberService;
import de.halbmann.sam.business.shares.boundary.ShareRepository;
import de.halbmann.sam.business.shares.entity.ShareEntity;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Admins see and revoke every share link, so a leaked link, or one of someone who left, can be shut
 * down. Everyone else still only touches their own.
 */
@QuarkusTest
class ShareAdministrationTest {

    private static final String CREATOR = "sub-share-creator";
    private static final String OTHER = "sub-share-other";

    @Inject
    ShareRepository shareRepository;

    @Inject
    SheetService sheetService;

    @Inject
    MemberService memberService;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private String sheetId;
    private String memberId;
    private UUID token;

    @BeforeEach
    void createShareOfALinkedMember() {
        CreateSheetMusic sheet = ObjectMother.createSimpleSheetMusic();
        sheet.setTitle("Shared " + suffix);
        sheetId = sheetService.addSheet(sheet).getId().toString();

        Member member = new Member();
        member.setName("Link Creator " + suffix);
        memberId = memberService.addMember(member).getId().toString();
        memberService.linkUser(memberId, CREATOR);

        token = QuarkusTransaction.requiringNew().call(() -> {
            ShareEntity share = new ShareEntity();
            share.setCreatorUserId(CREATOR);
            share.setResourceType(ShareType.SHEET);
            share.setResourceId(UUID.fromString(sheetId));
            shareRepository.persist(share);
            return share.getId();
        });
    }

    @AfterEach
    void deleteData() {
        QuarkusTransaction.requiringNew().run(() -> shareRepository.deleteById(token));
        sheetService.deleteSheet(sheetId);
        memberService.deleteMember(memberId);
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-admin"))
    void admin_listsEveryonesLinksWithTheirCreator() {
        given().get("/api/shares/all")
                .then()
                .statusCode(200)
                .body("data.id", hasItem(token.toString()))
                .body("data.find { it.id == '" + token + "' }.creatorName", is("Link Creator " + suffix))
                .body("data.find { it.id == '" + token + "' }.resourceLabel", is("Shared " + suffix));
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-admin"))
    void admin_ownListStillShowsOnlyOwnLinks() {
        given().get("/api/shares").then().statusCode(200).body("data.id", not(hasItem(token.toString())));
        given().get("/api/shares").then().body("data.creatorName", everyItem(nullValue()));
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_mayNotListEveryonesLinks() {
        given().get("/api/shares/all").then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void musician_mayNotListEveryonesLinks() {
        given().get("/api/shares/all").then().statusCode(403);
    }

    @Test
    void anonymous_mayNotListEveryonesLinks() {
        given().get("/api/shares/all").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-admin"))
    void admin_revokesSomeoneElsesLink_andTheLinkStopsWorking() {
        given().get("/api/public/shares/{t}/download", token).then().statusCode(204);

        given().delete("/api/shares/{id}", token).then().statusCode(204);

        given().get("/api/public/shares/{t}/download", token).then().statusCode(404);
        given().get("/api/public/shares/{t}", token).then().statusCode(200).body("expired", is(true));
    }

    @Test
    @TestSecurity(
            user = "librarian2",
            roles = {"music_librarian"})
    @OidcSecurity(claims = @Claim(key = "sub", value = OTHER))
    void otherLibrarian_mayNotRevokeSomeoneElsesLink() {
        given().delete("/api/shares/{id}", token).then().statusCode(404);
        given().get("/api/public/shares/{t}", token).then().statusCode(200).body("expired", is(false));
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    @OidcSecurity(claims = @Claim(key = "sub", value = CREATOR))
    void creator_stillRevokesTheirOwnLink() {
        given().delete("/api/shares/{id}", token).then().statusCode(204);
    }
}
