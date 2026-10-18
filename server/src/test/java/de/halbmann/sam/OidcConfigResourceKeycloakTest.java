package de.halbmann.sam;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/** With {@code sam.oidc.provider=keycloak}, the app boots with Keycloak's settings and tells the UI so. */
@QuarkusTest
@TestProfile(OidcConfigResourceKeycloakTest.KeycloakProvider.class)
class OidcConfigResourceKeycloakTest {

    public static class KeycloakProvider implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("sam.oidc.provider", "keycloak");
        }
    }

    @Test
    void servesKeycloakScopeAndRoleClaim() {
        given().get("/api/oidc-config.json")
                .then()
                .statusCode(200)
                .body("clientId", equalTo("sam-ui"))
                .body("scope", equalTo("openid profile email"))
                .body("roleClaimPath", equalTo("realm_access.roles"));
    }

    @Test
    void readsRealmRolesWithoutM2mTenant() {
        var config = ConfigProvider.getConfig();
        assertEquals("realm_access/roles", config.getValue("quarkus.oidc.roles.role-claim-path", String.class));
        assertFalse(
                config.getOptionalValue("sam.oidc.m2m-audience", String.class).isPresent());
    }
}
