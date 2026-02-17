package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.EnsembleVoicesResource;
import de.halbmann.sam.api.boundary.EnsemblesResource;
import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.controller.EnsembleService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

@RequestScoped
public class EnsemblesResourceImpl implements EnsemblesResource {

    @Context
    ResourceContext resourceContext;

    @Inject
    EnsembleService ensembleService;

    @Override
    public PaginatedResponse<Ensemble> findEnsembles(final EnsembleFilterRequest filterRequest) {
        return ensembleService.findEnsembles(filterRequest);
    }

    @Override
    public Ensemble add(final CreateEnsemble ensemble) {
        return ensembleService.addEnsemble(ensemble);
    }

    @Override
    public Ensemble load(final String ensembleId) {
        return ensembleService.getEnsemble(ensembleId);
    }

    @Override
    public void update(final String ensembleId, final Ensemble ensemble) {
        ensembleService.updateEnsemble(ensembleId, ensemble);
    }

    @Override
    public void delete(final String ensembleId) {
        ensembleService.deleteEnsemble(ensembleId);
    }

    @Override
    public EnsembleVoicesResource voices(final String ensembleId) {
        return resourceContext.getResource(EnsembleVoicesResourceImpl.class);
    }
}
