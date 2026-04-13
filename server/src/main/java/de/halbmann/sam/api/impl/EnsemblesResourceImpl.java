package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.EnsembleMembershipsResource;
import de.halbmann.sam.api.boundary.EnsembleVoicesResource;
import de.halbmann.sam.api.boundary.EnsemblesResource;
import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.ensembles.Ensemble;
import de.halbmann.sam.api.entity.ensembles.EnsembleCoverageStatus;
import de.halbmann.sam.api.entity.ensembles.EnsembleFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.ensembles.controller.CoverageSnapshotService;
import de.halbmann.sam.business.ensembles.controller.EnsembleService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

@Authenticated
@RequestScoped
public class EnsemblesResourceImpl implements EnsemblesResource {

    @Context
    ResourceContext resourceContext;

    @Inject
    EnsembleService ensembleService;

    @Inject
    CoverageSnapshotService coverageSnapshotService;

    @Override
    public PaginatedResponse<Ensemble> findEnsembles(final EnsembleFilterRequest filterRequest) {
        return ensembleService.findEnsembles(filterRequest);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public Ensemble add(final CreateEnsemble ensemble) {
        return ensembleService.addEnsemble(ensemble);
    }

    @Override
    public Ensemble load(final String ensembleId) {
        return ensembleService.getEnsemble(ensembleId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void update(final String ensembleId, final Ensemble ensemble) {
        ensembleService.updateEnsemble(ensembleId, ensemble);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void delete(final String ensembleId) {
        ensembleService.deleteEnsemble(ensembleId);
    }

    @Override
    public EnsembleVoicesResource voices(final String ensembleId) {
        return resourceContext.getResource(EnsembleVoicesResourceImpl.class);
    }

    @Override
    public EnsembleMembershipsResource members(final String ensembleId) {
        return resourceContext.getResource(EnsembleMembershipsResourceImpl.class);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public EnsembleCoverageStatus computeCoverage(final String ensembleId) {
        return coverageSnapshotService.compute(ensembleId);
    }

    @Override
    public EnsembleCoverageStatus getCoverageStatus(final String ensembleId) {
        return coverageSnapshotService
                .getStatus(ensembleId)
                .orElseThrow(() -> new NotFoundException("No coverage snapshot found for ensemble " + ensembleId));
    }
}
