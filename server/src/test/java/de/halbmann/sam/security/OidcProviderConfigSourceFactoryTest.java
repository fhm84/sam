package de.halbmann.sam.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OidcProviderConfigSourceFactoryTest {

    @Test
    void defaultsToPocketIdWithUserinfoRolesAndM2mTenant() {
        SmallRyeConfig config = config("prod", Map.of(), Map.of());

        assertEquals("userinfo", config.getRawValue("quarkus.oidc.roles.source"));
        assertEquals("groups", config.getRawValue("quarkus.oidc.roles.role-claim-path"));
        assertEquals("true", config.getRawValue("quarkus.oidc.authentication.user-info-required"));
        assertEquals("urn:sam:api", config.getRawValue("sam.oidc.m2m-audience"));
        assertEquals("urn:sam:api", config.getRawValue("quarkus.oidc.m2m.token.audience"));
        assertEquals("scp", config.getRawValue("quarkus.oidc.m2m.roles.role-claim-path"));
        assertEquals("openid profile email groups", config.getRawValue("sam.oidc.ui.scope"));
        assertEquals("groups", config.getRawValue("sam.oidc.ui.role-claim-path"));
    }

    @Test
    void m2mTenantUsesTheSharedIssuer() {
        SmallRyeConfig config =
                config("prod", Map.of("%prod.quarkus.oidc.auth-server-url", "https://login.example.org"), Map.of());

        assertEquals("https://login.example.org", config.getRawValue("quarkus.oidc.m2m.auth-server-url"));
    }

    @Test
    void keycloakReadsRealmRolesFromTheAccessTokenWithoutM2mTenant() {
        SmallRyeConfig config = config("prod", Map.of(), Map.of("sam.oidc.provider", "keycloak"));

        assertEquals("realm_access/roles", config.getRawValue("quarkus.oidc.roles.role-claim-path"));
        assertFalse(config.getOptionalValue("quarkus.oidc.roles.source", String.class)
                .isPresent());
        assertFalse(
                config.getOptionalValue("sam.oidc.m2m-audience", String.class).isPresent());
        assertFalse(config.getOptionalValue("quarkus.oidc.m2m.auth-server-url", String.class)
                .isPresent());
        assertEquals("openid profile email", config.getRawValue("sam.oidc.ui.scope"));
        assertEquals("realm_access.roles", config.getRawValue("sam.oidc.ui.role-claim-path"));
    }

    @Test
    void providerFileIsProfileAware() {
        assertEquals(
                "http://localhost:1411", config("dev", Map.of(), Map.of()).getRawValue("quarkus.oidc.auth-server-url"));
        assertEquals(
                "http://localhost:8180/realms/sam",
                config("dev", Map.of(), Map.of("sam.oidc.provider", "keycloak"))
                        .getRawValue("quarkus.oidc.auth-server-url"));
        assertTrue(config("dev", Map.of(), Map.of("sam.oidc.provider", "keycloak"))
                .getOptionalValue("quarkus.keycloak.admin-client.server-url", String.class)
                .isPresent());
        assertFalse(config("dev", Map.of(), Map.of())
                .getOptionalValue("quarkus.keycloak.admin-client.server-url", String.class)
                .isPresent());
    }

    @Test
    void profiledProviderInApplicationPropertiesYieldsToEnv() {
        Map<String, String> appProperties = Map.of("%dev.sam.oidc.provider", "keycloak");

        assertEquals(
                "realm_access/roles",
                config("dev", appProperties, Map.of()).getRawValue("quarkus.oidc.roles.role-claim-path"));
        assertEquals(
                "groups",
                config("dev", appProperties, Map.of("sam.oidc.provider", "pocket-id"))
                        .getRawValue("quarkus.oidc.roles.role-claim-path"));
    }

    @Test
    void envOverridesTheProviderFile() {
        SmallRyeConfig config = config("prod", Map.of(), Map.of("sam.oidc.ui.scope", "openid groups"));

        assertEquals("openid groups", config.getRawValue("sam.oidc.ui.scope"));
    }

    @Test
    void rejectsUnknownProvider() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> config("prod", Map.of(), Map.of("sam.oidc.provider", "authelia")));
        assertTrue(e.getMessage().contains("authelia"), e.getMessage());
    }

    /** Builds the config like Quarkus does: application.properties at 250, env vars at 300. */
    private static SmallRyeConfig config(String profile, Map<String, String> appProperties, Map<String, String> env) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withProfile(profile)
                .withSources(new PropertiesConfigSource(appProperties, "application.properties", 250))
                .withSources(new PropertiesConfigSource(env, "env", 300))
                .withSources(new OidcProviderConfigSourceFactory())
                .build();
    }
}
