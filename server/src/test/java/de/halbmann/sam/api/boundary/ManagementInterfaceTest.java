package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

/**
 * Verifies the ops/diagnostic surface exposed via Quarkus's management interface (see ADR-0007):
 * {@code /q/info} and {@code /q/metrics} live on a separate, unauthenticated port and must never
 * be reachable through the main (authenticated) HTTP port.
 */
@QuarkusTest
class ManagementInterfaceTest {

    @Inject
    @ConfigProperty(name = "quarkus.management.test-port")
    int managementPort;

    @Test
    void qInfo_onManagementPort_isReachableWithoutAuth() {
        given().port(managementPort).get("/q/info").then().statusCode(200).body(containsString("git"));
    }

    @Test
    void qMetrics_onManagementPort_isReachableWithoutAuth() {
        given().port(managementPort).get("/q/metrics").then().statusCode(200);
    }

    @Test
    void qInfo_onMainHttpPort_isNotReachable() {
        // /q/info must stay isolated on the management port, never leaking onto
        // the main port shared with the authenticated /api/* surface.
        given().get("/q/info").then().statusCode(404);
    }

    @Test
    void qMetrics_onMainHttpPort_isNotReachable() {
        given().get("/q/metrics").then().statusCode(404);
    }
}
