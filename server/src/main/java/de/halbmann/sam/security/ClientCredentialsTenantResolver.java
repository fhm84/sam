package de.halbmann.sam.security;

import io.quarkus.oidc.TenantResolver;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Routes client-credentials tokens (the {@code cli} service account) to the {@code m2m} OIDC tenant.
 *
 * <p>Needed for providers like Pocket ID that put a user's groups only into the ID token and
 * userinfo, not the access token: user tokens then need a userinfo lookup for their roles (default
 * tenant), while a client-credentials token has no userinfo and carries its roles as API permissions
 * in {@code scp} ({@code m2m} tenant). The token is told apart by its audience, read here without
 * verification — the chosen tenant verifies it fully, including that audience.
 *
 * <p>Inactive unless {@code sam.oidc.m2m-audience} is set, so Keycloak setups keep the default tenant.
 */
@ApplicationScoped
public class ClientCredentialsTenantResolver implements TenantResolver {

    static final String M2M_TENANT = "m2m";

    @ConfigProperty(name = "sam.oidc.m2m-audience")
    Optional<String> m2mAudience;

    @Override
    public String resolve(RoutingContext context) {
        if (m2mAudience.isEmpty()) {
            return null;
        }
        String header = context.request().getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return audienceOf(header.substring(7)).contains(m2mAudience.get()) ? M2M_TENANT : null;
    }

    private static JsonArray audienceOf(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            return new JsonArray();
        }
        try {
            JsonObject payload =
                    new JsonObject(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
            Object aud = payload.getValue("aud");
            if (aud instanceof JsonArray array) {
                return array;
            }
            return aud instanceof String single ? JsonArray.of(single) : new JsonArray();
        } catch (RuntimeException e) {
            // Not a JWT we can read: let the default tenant reject it
            return new JsonArray();
        }
    }
}
