package de.halbmann.sam.security;

/** Keycloak realm role names used in {@code @RolesAllowed} across all resource implementations. */
public final class Roles {

    public static final String ADMIN = "admin";
    public static final String MUSIC_LIBRARIAN = "music_librarian";

    private Roles() {}
}
