package de.halbmann.sam.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClientCredentialsTenantResolverTest {

    private static final String API = "urn:sam:api";

    @Test
    void routesTokenForTheApiAudienceToM2mTenant() {
        assertEquals("m2m", resolve(Optional.of(API), "Bearer " + jwt("{\"aud\":[\"" + API + "\"]}")));
        assertEquals("m2m", resolve(Optional.of(API), "bearer " + jwt("{\"aud\":\"" + API + "\"}")));
    }

    @Test
    void keepsUserTokensOnDefaultTenant() {
        assertNull(resolve(Optional.of(API), "Bearer " + jwt("{\"aud\":[\"sam-ui\",\"http://localhost:1411\"]}")));
        assertNull(resolve(Optional.of(API), "Bearer " + jwt("{\"sub\":\"no-audience\"}")));
    }

    @Test
    void keepsUnreadableOrMissingTokensOnDefaultTenant() {
        assertNull(resolve(Optional.of(API), null));
        assertNull(resolve(Optional.of(API), "Basic dXNlcjpwYXNz"));
        assertNull(resolve(Optional.of(API), "Bearer not-a-jwt"));
        assertNull(resolve(Optional.of(API), "Bearer a.%%%.c"));
    }

    @Test
    void inactiveWithoutConfiguredAudience() {
        assertNull(resolve(Optional.empty(), "Bearer " + jwt("{\"aud\":[\"" + API + "\"]}")));
    }

    private static String resolve(Optional<String> audience, String authorization) {
        ClientCredentialsTenantResolver resolver = new ClientCredentialsTenantResolver();
        resolver.m2mAudience = audience;
        RoutingContext context = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(context.request()).thenReturn(request);
        when(request.getHeader("Authorization")).thenReturn(authorization);
        return resolver.resolve(context);
    }

    private static String jwt(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
                + "."
                + encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8))
                + ".sig";
    }
}
