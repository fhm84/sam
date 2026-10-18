package de.halbmann.sam.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OidcProviderConfigSourceFactoryTest {

    private static final String RESOURCE = "quarkus.oidc-client.grant-options.client.resource";

    @Test
    void defaultsToPocketIdTokenRequestForTheApiResource() {
        SmallRyeConfig config = config(Map.of());

        assertEquals("http://localhost:1411", config.getRawValue("quarkus.oidc-client.auth-server-url"));
        assertEquals("/api/oidc/token", config.getRawValue("quarkus.oidc-client.token-path"));
        assertEquals("urn:sam:api", config.getRawValue(RESOURCE));
    }

    @Test
    void keycloakUsesItsTokenEndpointWithoutResource() {
        SmallRyeConfig config = config(Map.of("sam.oidc.provider", "keycloak"));

        assertEquals("http://localhost:8180/realms/sam", config.getRawValue("quarkus.oidc-client.auth-server-url"));
        assertEquals("/protocol/openid-connect/token", config.getRawValue("quarkus.oidc-client.token-path"));
        assertFalse(config.getOptionalValue(RESOURCE, String.class).isPresent());
    }

    @Test
    void prodTakesIssuerAndSecretFromEnv() {
        SmallRyeConfig config =
                config(Map.of("OIDC_SERVER_URL", "https://login.example.org", "SAM_CLI_CLIENT_SECRET", "s3cret"));

        assertEquals("https://login.example.org", config.getRawValue("quarkus.oidc-client.auth-server-url"));
        assertEquals("s3cret", config.getRawValue("quarkus.oidc-client.credentials.secret"));
    }

    @Test
    void rejectsUnknownProvider() {
        assertThrows(IllegalArgumentException.class, () -> config(Map.of("sam.oidc.provider", "authelia")));
    }

    /** application.properties' prod secret at 250, env vars at 300, like the CLI jar runs. */
    private static SmallRyeConfig config(Map<String, String> env) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withProfile("prod")
                .withSources(new PropertiesConfigSource(
                        Map.of("%prod.quarkus.oidc-client.credentials.secret", "${SAM_CLI_CLIENT_SECRET}"),
                        "application.properties",
                        250))
                .withSources(new PropertiesConfigSource(env, "env", 300))
                .withSources(new OidcProviderConfigSourceFactory())
                .build();
    }
}
