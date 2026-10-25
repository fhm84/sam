package de.halbmann.sam.security;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Provides information about the currently authenticated user derived from the OIDC JWT.
 *
 * <p>Ensemble membership is not part of the token: it lives in the database ({@code
 * EnsembleMembershipEntity}), so neither OIDC provider needs per-ensemble groups.
 */
@RequestScoped
public class CurrentUserService {

    @Inject
    SecurityIdentity identity;

    @Inject
    Instance<JsonWebToken> jwt;

    /** Returns the OIDC subject claim (unique user ID), or {@code null} for anonymous requests. */
    public String getUserId() {
        if (identity.isAnonymous()) {
            return null;
        }
        // A bearer token's principal is the token itself; also where @OidcSecurity tests put it
        if (identity.getPrincipal() instanceof JsonWebToken token) {
            return token.getSubject();
        }
        return jwt.isUnsatisfied() ? null : jwt.get().getSubject();
    }

    /** Returns the {@code preferred_username} claim, or {@code null} for anonymous requests. */
    public String getUsername() {
        if (identity.isAnonymous() || jwt.isUnsatisfied()) {
            return null;
        }
        String username = jwt.get().getClaim("preferred_username");
        if (username == null && identity.getAttribute("userinfo") instanceof UserInfo userInfo) {
            // Providers like Pocket ID keep profile claims out of the access token
            username = userInfo.getPreferredUserName();
        }
        return username;
    }

    /** Returns {@code true} if the current user holds the given realm role. */
    public boolean hasRole(String role) {
        return identity.hasRole(role);
    }
}
