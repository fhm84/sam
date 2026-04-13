package de.halbmann.sam.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Provides information about the currently authenticated user derived from the OIDC JWT.
 *
 * <p>Group membership is expected as flat group names in the {@code groups} claim using the format
 * {@code ensemble:{UUID}} (e.g. {@code ensemble:550e8400-e29b-41d4-a716-446655440000}). The
 * Keycloak group membership mapper must be configured to emit non-full-path group names.
 */
@RequestScoped
public class CurrentUserService {

    private static final String ENSEMBLE_GROUP_PREFIX = "ensemble:";

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken jwt;

    /** Returns the OIDC subject claim (unique user ID), or {@code null} for anonymous requests. */
    public String getUserId() {
        if (identity.isAnonymous()) {
            return null;
        }
        return jwt.getSubject();
    }

    /** Returns {@code true} if the current user holds the given realm role. */
    public boolean hasRole(String role) {
        return identity.hasRole(role);
    }

    /**
     * Returns the set of ensemble UUIDs the current user has access to via Keycloak group
     * membership (groups prefixed with {@code ensemble:}).
     */
    public Set<UUID> getAccessibleEnsembleIds() {
        Set<String> groups = jwt.getClaim("groups");
        if (groups == null || groups.isEmpty()) {
            return Set.of();
        }
        return groups.stream()
                .filter(g -> g.startsWith(ENSEMBLE_GROUP_PREFIX))
                .map(g -> g.substring(ENSEMBLE_GROUP_PREFIX.length()))
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns {@code true} if the current user is a member of the given ensemble (via group
     * membership) or holds a privileged role ({@code admin} or {@code music_librarian}).
     */
    public boolean canAccessEnsemble(UUID ensembleId) {
        if (hasRole("admin") || hasRole("music_librarian")) {
            return true;
        }
        return getAccessibleEnsembleIds().contains(ensembleId);
    }
}
