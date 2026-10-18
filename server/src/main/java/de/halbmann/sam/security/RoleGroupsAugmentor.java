package de.halbmann.sam.security;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Replaces a user's provider groups with the SAM roles they map to ({@link RoleGroups}), so groups
 * of other apps on a shared Pocket ID grant nothing in SAM.
 *
 * <p>Applies to OIDC user tokens only: the {@code m2m} tenant's roles are API permissions of SAM's
 * own API resource, and identities without a tenant (e.g. {@code @TestSecurity}) are left alone.
 */
@ApplicationScoped
public class RoleGroupsAugmentor implements SecurityIdentityAugmentor {

    @Inject
    RoleGroups roleGroups;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        String tenant = identity.getAttribute(OidcUtils.TENANT_ID_ATTRIBUTE);
        if (!roleGroups.isMapped()
                || identity.isAnonymous()
                || tenant == null
                || ClientCredentialsTenantResolver.M2M_TENANT.equals(tenant)) {
            return Uni.createFrom().item(identity);
        }
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                .setPrincipal(identity.getPrincipal())
                .addCredentials(identity.getCredentials())
                .addAttributes(identity.getAttributes())
                .addPermissionChecker(identity::checkPermission)
                .setAnonymous(false);
        roleGroups.groupByRole().forEach((role, group) -> {
            if (identity.getRoles().contains(group)) {
                builder.addRole(role);
            }
        });
        return Uni.createFrom().item(builder.build());
    }
}
