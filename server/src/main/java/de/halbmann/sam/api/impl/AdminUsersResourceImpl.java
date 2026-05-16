package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.AdminUsersResource;
import de.halbmann.sam.api.entity.admin.UserInfo;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;

@Authenticated
@RequestScoped
public class AdminUsersResourceImpl implements AdminUsersResource {

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "sam.admin.keycloak.realm")
    String realm;

    @Override
    @RolesAllowed(Roles.ADMIN)
    public List<UserInfo> searchUsers(String search) {
        return keycloak.realm(realm).users().search(search, 0, 20).stream()
                .map(this::toUserInfo)
                .toList();
    }

    @Override
    @RolesAllowed(Roles.ADMIN)
    public UserInfo getUser(String userId) {
        try {
            UserRepresentation rep = keycloak.realm(realm).users().get(userId).toRepresentation();
            return toUserInfo(rep);
        } catch (NotFoundException e) {
            throw new NotFoundException("User not found: " + userId);
        }
    }

    private UserInfo toUserInfo(UserRepresentation rep) {
        return new UserInfo(rep.getId(), rep.getUsername(), rep.getEmail(), rep.getFirstName(), rep.getLastName());
    }
}
