package de.halbmann.sam.api.entity.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Which OIDC provider holds the user accounts, and whether SAM can search them. Without search
 * (Pocket ID without an API key), an admin links a musician by entering the user's ID.
 *
 * @param provider the active OIDC provider, {@code pocket-id} or {@code keycloak}
 * @param searchable whether {@code GET /admin/users?search=} is available
 */
public record UserDirectoryInfo(@NotNull String provider, boolean searchable) {}
