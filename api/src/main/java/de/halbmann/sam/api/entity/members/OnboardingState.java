package de.halbmann.sam.api.entity.members;

/** How far a member is with getting a login. Derived from the login link and the invite time. */
public enum OnboardingState {
    /** No login and no invite sent. */
    NONE,
    /** An invite was sent, but no login is linked yet. */
    INVITED,
    /** A login is linked. */
    LINKED
}
