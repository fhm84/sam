package de.halbmann.sam.schema;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Boots the application with Hibernate's schema validation against the schema built purely by
 * the Flyway migrations. Startup fails if an entity maps a table or column that no migration
 * creates (or with an incompatible type) — the drift that Hibernate's {@code update} strategy
 * used to paper over in every profile. Prod runs with {@code validate} as well.
 */
@QuarkusTest
@TestProfile(FlywaySchemaValidationTest.ValidateSchemaProfile.class)
class FlywaySchemaValidationTest {

    public static class ValidateSchemaProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            // Both the current and the deprecated property name, with and without the profile
            // prefix, so no lower-priority setting elsewhere wins.
            return Map.of(
                    "quarkus.hibernate-orm.schema-management.strategy", "validate",
                    "%test.quarkus.hibernate-orm.schema-management.strategy", "validate",
                    "quarkus.hibernate-orm.database.generation", "validate",
                    "%test.quarkus.hibernate-orm.database.generation", "validate");
        }
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void applicationStartsWithValidatedFlywaySchema() {
        given().get("/api/sheets").then().statusCode(200);
    }
}
