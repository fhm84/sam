package de.halbmann.sam.security;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.api.entity.admin.UserInfo;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Searches Keycloak's users through the admin client, as the {@code sam-backend} service account
 * (role {@code view-users} only).
 */
@ApplicationScoped
@LookupIfProperty(name = EnvConsts.OIDC_PROVIDER, stringValue = "keycloak")
public class KeycloakUserDirectory implements UserDirectory {

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = EnvConsts.KEYCLOAK_ADMIN_REALM)
    String realm;

    @Override
    public boolean searchable() {
        return true;
    }

    @Override
    public List<UserInfo> search(String query) {
        return keycloak.realm(realm).users().search(query, 0, 20).stream()
                .map(KeycloakUserDirectory::toUserInfo)
                .toList();
    }

    @Override
    public Optional<UserInfo> find(String userId) {
        try {
            return Optional.of(
                    toUserInfo(keycloak.realm(realm).users().get(userId).toRepresentation()));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private static UserInfo toUserInfo(UserRepresentation rep) {
        return new UserInfo(rep.getId(), rep.getUsername(), rep.getEmail(), rep.getFirstName(), rep.getLastName());
    }
}
