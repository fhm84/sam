package de.halbmann.sam.security;

import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Decides whether the caller may see other people's personal data (musician contact details,
 * linked login, invite timestamps). Only the music librarian and the admin may; every other
 * login, and the anonymous share-link path, gets it removed.
 */
@ApplicationScoped
public class PersonalDataAccess {

    @Inject
    CurrentUserService currentUserService;

    public boolean callerMaySee() {
        if (!Arc.container().requestContext().isActive()) {
            // Startup and background jobs return nothing to a user
            return true;
        }
        return currentUserService.hasRole(Roles.MUSIC_LIBRARIAN) || currentUserService.hasRole(Roles.ADMIN);
    }
}
