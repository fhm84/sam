package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.AdminStorageResource;
import de.halbmann.sam.api.entity.admin.StorageIntegrityReport;
import de.halbmann.sam.business.documents.controller.StorageIntegrityCheck;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@Authenticated
@RequestScoped
public class AdminStorageResourceImpl implements AdminStorageResource {

    @Inject
    StorageIntegrityCheck storageIntegrityCheck;

    @Override
    @RolesAllowed(Roles.ADMIN)
    public StorageIntegrityReport checkIntegrity() {
        return storageIntegrityCheck.run();
    }
}
