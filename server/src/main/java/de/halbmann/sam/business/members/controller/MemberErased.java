package de.halbmann.sam.business.members.controller;

import java.util.UUID;

/**
 * Fired when a member profile was deleted, so its audit history can be purged once the deletion is
 * committed.
 *
 * @param memberId      the member's (and person's) ID
 * @param personDeleted whether the person was deleted too (it was not credited on any sheet)
 */
public record MemberErased(UUID memberId, boolean personDeleted) {}
