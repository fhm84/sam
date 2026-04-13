package de.halbmann.sam.business.ensembles.controller;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.ensembles.Ensemble;
import de.halbmann.sam.api.entity.ensembles.EnsembleFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.ensembles.boundary.EnsembleRepository;
import de.halbmann.sam.business.ensembles.entity.EnsembleEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class EnsembleService {

    @Inject
    EnsembleRepository ensembleRepository;

    @Inject
    EnsembleMapper ensembleMapper;

    public PaginatedResponse<Ensemble> findEnsembles(final EnsembleFilterRequest filterRequest) {
        if (filterRequest.getName() != null && !filterRequest.getName().isEmpty()) {
            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", filterRequest.getName());
            return findEnsembles(filterRequest, parameters);
        } else {
            return findEnsembles(filterRequest, Map.of());
        }
    }

    private PaginatedResponse<Ensemble> findEnsembles(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        PaginatedEntities<EnsembleEntity> result =
                ensembleRepository.findEnsembleEntities(paginationRequest, parameters);

        PaginatedResponse<Ensemble> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(ensembleMapper::toDto).toList());
        response.setPage(paginationRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public Ensemble getEnsemble(final String ensembleId) {
        EnsembleEntity entity = ensembleRepository
                .findByIdOptional(UUID.fromString(ensembleId))
                .orElseThrow(() -> new EntityNotFoundException("Ensemble", ensembleId));
        return ensembleMapper.toDto(entity);
    }

    public Ensemble addEnsemble(final CreateEnsemble ensemble) {
        EnsembleEntity entity = ensembleMapper.fromDto(ensemble);
        ensembleRepository.persistAndFlush(entity);
        return ensembleMapper.toDto(entity);
    }

    public void updateEnsemble(final String ensembleId, final Ensemble ensemble) {
        final EnsembleEntity entity = ensembleRepository
                .findByIdOptional(UUID.fromString(ensembleId))
                .orElseThrow(() -> new EntityNotFoundException("Ensemble", ensembleId));
        ensembleMapper.update(entity, ensemble);
    }

    public void deleteEnsemble(final String ensembleId) {
        final EnsembleEntity entity = ensembleRepository
                .findByIdOptional(UUID.fromString(ensembleId))
                .orElseThrow(() -> new EntityNotFoundException("Ensemble", ensembleId));
        ensembleRepository.delete(entity);
    }
}
