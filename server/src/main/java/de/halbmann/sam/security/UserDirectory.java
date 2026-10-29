package de.halbmann.sam.security;

import de.halbmann.sam.api.entity.admin.UserInfo;
import java.util.List;
import java.util.Optional;

/**
 * The active OIDC provider's user accounts, for linking members to logins (ADR-0001). User IDs
 * are the provider's {@code sub}.
 *
 * <p>One implementation per provider, picked by {@code sam.oidc.provider} via
 * {@code @LookupIfProperty}; inject it as {@code Instance<UserDirectory>}.
 */
public interface UserDirectory {

    /** Whether {@link #search(String)} is available; Pocket ID needs an API key for it. */
    boolean searchable();

    /** Users whose name, username or email contain the query; at most 20. */
    List<UserInfo> search(String query);

    /** The user with the given ID, or empty if the provider has none. */
    Optional<UserInfo> find(String userId);
}
