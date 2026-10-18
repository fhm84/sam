package de.halbmann.sam.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Which provider group grants which SAM role ({@code sam.oidc.role-groups.<role>}).
 *
 * <p>Pocket ID groups are global and often shared by several self-hosted apps, so a group named
 * {@code admin} likely means admin elsewhere too. With a mapping, a user gets a SAM role only from
 * its SAM-specific group ({@code sam-admin}, {@code sam-music-librarian}). Empty for Keycloak, whose
 * realm roles already belong to SAM's own realm.
 */
@ApplicationScoped
public class RoleGroups {

    private final Map<String, String> groupByRole = new LinkedHashMap<>();

    RoleGroups(
            @ConfigProperty(name = "sam.oidc.role-groups.admin") Optional<String> adminGroup,
            @ConfigProperty(name = "sam.oidc.role-groups.music_librarian") Optional<String> librarianGroup) {
        adminGroup.ifPresent(group -> groupByRole.put(Roles.ADMIN, group));
        librarianGroup.ifPresent(group -> groupByRole.put(Roles.MUSIC_LIBRARIAN, group));
    }

    /** SAM role → provider group; empty when the provider's roles are SAM's own. */
    public Map<String, String> groupByRole() {
        return Map.copyOf(groupByRole);
    }

    public boolean isMapped() {
        return !groupByRole.isEmpty();
    }
}
