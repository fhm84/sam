package de.halbmann.sam.business.ensembles.controller;

import de.halbmann.sam.api.entity.ensembles.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.ensembles.EnsembleMembership;
import de.halbmann.sam.business.ensembles.boundary.EnsembleMembershipRepository;
import de.halbmann.sam.business.ensembles.boundary.EnsembleRepository;
import de.halbmann.sam.business.ensembles.boundary.EnsembleVoiceRepository;
import de.halbmann.sam.business.ensembles.entity.EnsembleEntity;
import de.halbmann.sam.business.ensembles.entity.EnsembleMembershipEntity;
import de.halbmann.sam.business.ensembles.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.instruments.boundary.InstrumentRepository;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.musicians.boundary.MusicianRepository;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class EnsembleMembershipService {

    @Inject
    EnsembleMembershipRepository membershipRepository;

    @Inject
    EnsembleMembershipMapper membershipMapper;

    @Inject
    EnsembleRepository ensembleRepository;

    @Inject
    MusicianRepository musicianRepository;

    @Inject
    EnsembleVoiceRepository voiceRepository;

    @Inject
    InstrumentRepository instrumentRepository;

    public List<EnsembleMembership> getMembers(final String ensembleId) {
        return membershipRepository
                .find(
                        "ensemble.id = :ensembleId",
                        Sort.ascending("musician.name"),
                        Map.of("ensembleId", UUID.fromString(ensembleId)))
                .list()
                .stream()
                .map(membershipMapper::toDto)
                .toList();
    }

    public EnsembleMembership addMember(final String ensembleId, final CreateEnsembleMembership dto) {
        final EnsembleEntity ensemble = ensembleRepository
                .findByIdOptional(UUID.fromString(ensembleId))
                .orElseThrow(() -> new EntityNotFoundException("Ensemble", ensembleId));

        final MusicianEntity musician = musicianRepository
                .findByIdOptional(dto.getMusicianId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Musician", dto.getMusicianId().toString()));

        final EnsembleMembershipEntity entity = membershipMapper.fromCreateDto(dto);
        entity.setEnsemble(ensemble);
        entity.setMusician(musician);
        entity.setVoice(resolveVoice(dto.getVoiceId()));
        entity.setInstrument(resolveInstrument(dto.getInstrumentId()));

        membershipRepository.persistAndFlush(entity);
        return membershipMapper.toDto(entity);
    }

    public EnsembleMembership updateMember(final String memberId, final EnsembleMembership dto) {
        final EnsembleMembershipEntity entity = membershipRepository
                .findByIdOptional(UUID.fromString(memberId))
                .orElseThrow(() -> new EntityNotFoundException("EnsembleMembership", memberId));

        membershipMapper.update(entity, dto);
        entity.setVoice(resolveVoice(dto.getVoiceId()));
        entity.setInstrument(resolveInstrument(dto.getInstrumentId()));
        return membershipMapper.toDto(entity);
    }

    public void deleteMember(final String memberId) {
        final EnsembleMembershipEntity entity = membershipRepository
                .findByIdOptional(UUID.fromString(memberId))
                .orElseThrow(() -> new EntityNotFoundException("EnsembleMembership", memberId));
        membershipRepository.delete(entity);
    }

    private EnsembleVoiceEntity resolveVoice(final UUID voiceId) {
        if (voiceId == null) {
            return null;
        }
        return voiceRepository
                .findByIdOptional(voiceId)
                .orElseThrow(() -> new EntityNotFoundException("EnsembleVoice", voiceId.toString()));
    }

    private InstrumentEntity resolveInstrument(final String instrumentId) {
        if (instrumentId == null) {
            return null;
        }
        return instrumentRepository
                .findByIdOptional(instrumentId)
                .orElseThrow(() -> new EntityNotFoundException("Instrument", instrumentId));
    }
}
