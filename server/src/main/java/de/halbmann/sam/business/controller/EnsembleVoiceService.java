package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CreateEnsembleVoice;
import de.halbmann.sam.api.entity.EnsembleVoice;
import de.halbmann.sam.business.boundary.EnsembleRepository;
import de.halbmann.sam.business.boundary.EnsembleVoiceRepository;
import de.halbmann.sam.business.entity.EnsembleEntity;
import de.halbmann.sam.business.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class EnsembleVoiceService {

    @Inject
    EnsembleVoiceRepository ensembleVoiceRepository;

    @Inject
    EnsembleVoiceMapper ensembleVoiceMapper;

    @Inject
    EnsembleRepository ensembleRepository;

    public List<EnsembleVoice> getVoices(final String ensembleId) {
        Sort sort = Sort.ascending("label");
        return ensembleVoiceRepository
                .find("ensemble.id = :ensembleId", sort, Map.of("ensembleId", UUID.fromString(ensembleId)))
                .list()
                .stream()
                .map(ensembleVoiceMapper::toDto)
                .toList();
    }

    public EnsembleVoice getVoice(final String voiceId) {
        final EnsembleVoiceEntity entity = ensembleVoiceRepository
                .findByIdOptional(UUID.fromString(voiceId))
                .orElseThrow(() -> new EntityNotFoundException("EnsembleVoice", voiceId));
        return ensembleVoiceMapper.toDto(entity);
    }

    public EnsembleVoice addVoice(final String ensembleId, final CreateEnsembleVoice voice) {
        final EnsembleEntity ensemble = ensembleRepository
                .findByIdOptional(UUID.fromString(ensembleId))
                .orElseThrow(() -> new EntityNotFoundException("Ensemble", ensembleId));
        final EnsembleVoiceEntity entity = ensembleVoiceMapper.fromDto(voice);
        entity.setEnsemble(ensemble);
        ensembleVoiceRepository.persistAndFlush(entity);
        return ensembleVoiceMapper.toDto(entity);
    }

    public void updateVoice(final String voiceId, final EnsembleVoice voice) {
        final EnsembleVoiceEntity entity = ensembleVoiceRepository
                .findByIdOptional(UUID.fromString(voiceId))
                .orElseThrow(() -> new EntityNotFoundException("EnsembleVoice", voiceId));
        ensembleVoiceMapper.update(entity, voice);
    }

    public void deleteVoice(final String voiceId) {
        final EnsembleVoiceEntity entity = ensembleVoiceRepository
                .findByIdOptional(UUID.fromString(voiceId))
                .orElseThrow(() -> new EntityNotFoundException("EnsembleVoice", voiceId));
        ensembleVoiceRepository.delete(entity);
    }
}
