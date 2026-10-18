package de.halbmann.sam.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleGroupsAugmentorTest {

    private static final RoleGroups POCKET_ID =
            new RoleGroups(Optional.of("sam-admin"), Optional.of("sam-music-librarian"));

    @Test
    void grantsRolesOnlyFromSamGroups() {
        SecurityIdentity user = identity("Default", "sam-member", "sam-music-librarian", "admin");

        assertEquals(Set.of(Roles.MUSIC_LIBRARIAN), augment(POCKET_ID, user).getRoles());
    }

    @Test
    void otherAppsGroupsGrantNothing() {
        SecurityIdentity user = identity("Default", "admin", "music_librarian");

        assertEquals(Set.of(), augment(POCKET_ID, user).getRoles());
    }

    @Test
    void keepsPrincipalAndAttributes() {
        SecurityIdentity user = identity("Default", "sam-admin");

        SecurityIdentity augmented = augment(POCKET_ID, user);
        assertEquals("user1", augmented.getPrincipal().getName());
        assertEquals("Default", augmented.getAttribute(OidcUtils.TENANT_ID_ATTRIBUTE));
        assertEquals(Set.of(Roles.ADMIN), augmented.getRoles());
    }

    @Test
    void leavesM2mTokensAlone() {
        // The m2m tenant's roles are API permissions of SAM's own API resource
        SecurityIdentity cli = identity(ClientCredentialsTenantResolver.M2M_TENANT, Roles.MUSIC_LIBRARIAN);

        assertSame(cli, augment(POCKET_ID, cli));
    }

    @Test
    void leavesIdentitiesWithoutTenantAlone() {
        // e.g. @TestSecurity users
        SecurityIdentity test = identity(null, Roles.ADMIN);

        assertSame(test, augment(POCKET_ID, test));
    }

    @Test
    void inactiveWithoutMapping() {
        SecurityIdentity keycloakUser = identity("Default", Roles.ADMIN);

        assertSame(keycloakUser, augment(new RoleGroups(Optional.empty(), Optional.empty()), keycloakUser));
    }

    private static SecurityIdentity augment(RoleGroups roleGroups, SecurityIdentity identity) {
        RoleGroupsAugmentor augmentor = new RoleGroupsAugmentor();
        augmentor.roleGroups = roleGroups;
        return augmentor.augment(identity, null).await().indefinitely();
    }

    private static SecurityIdentity identity(String tenant, String... roles) {
        QuarkusSecurityIdentity.Builder builder =
                QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal("user1"));
        if (tenant != null) {
            builder.addAttribute(OidcUtils.TENANT_ID_ATTRIBUTE, tenant);
        }
        for (String role : roles) {
            builder.addRole(role);
        }
        return builder.build();
    }
}
