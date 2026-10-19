package de.halbmann.sam.api.impl;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.api.boundary.AdminUsersResource;
import de.halbmann.sam.api.entity.admin.UserDirectoryInfo;
import de.halbmann.sam.api.entity.admin.UserInfo;
import de.halbmann.sam.security.Roles;
import de.halbmann.sam.security.UserDirectory;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Authenticated
@RequestScoped
public class AdminUsersResourceImpl implements AdminUsersResource {

    // The active provider's directory (@LookupIfProperty on sam.oidc.provider)
    @Inject
    Instance<UserDirectory> directory;

    @ConfigProperty(name = EnvConsts.OIDC_PROVIDER)
    String provider;

    @Override
    @RolesAllowed(Roles.ADMIN)
    public List<UserInfo> searchUsers(String search) {
        if (!directory.get().searchable()) {
            throw new ServerErrorException("User search is not configured", Response.Status.NOT_IMPLEMENTED);
        }
        return directory.get().search(search);
    }

    @Override
    @RolesAllowed(Roles.ADMIN)
    public UserDirectoryInfo getDirectory() {
        return new UserDirectoryInfo(provider, directory.get().searchable());
    }

    @Override
    @RolesAllowed(Roles.ADMIN)
    public UserInfo getUser(String userId) {
        return directory.get().find(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
