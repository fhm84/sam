package de.halbmann.sam.business.ensembles.controller;

import de.halbmann.sam.api.entity.ensembles.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.ensembles.EnsembleMembership;
import de.halbmann.sam.api.entity.ensembles.EnsembleRole;
import de.halbmann.sam.business.ensembles.boundary.EnsembleMembershipRepository;
import de.halbmann.sam.business.ensembles.boundary.EnsembleRepository;
import de.halbmann.sam.business.ensembles.boundary.EnsembleVoiceRepository;
import de.halbmann.sam.business.ensembles.entity.EnsembleEntity;
import de.halbmann.sam.business.ensembles.entity.EnsembleMembershipEntity;
import de.halbmann.sam.business.ensembles.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.instruments.boundary.InstrumentRepository;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.members.boundary.MemberRepository;
import de.halbmann.sam.business.members.entity.MemberEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    MemberRepository memberRepository;

    @Inject
    EnsembleVoiceRepository voiceRepository;

    @Inject
    InstrumentRepository instrumentRepository;

    public List<EnsembleMembership> getMembers(final String ensembleId) {
        return membershipRepository
                .find(
                        "ensemble.id = :ensembleId",
                        Sort.ascending("member.person.name"),
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

        final MemberEntity member = memberRepository
                .findByIdOptional(dto.getMemberId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Member", dto.getMemberId().toString()));

        final EnsembleMembershipEntity entity = membershipMapper.fromCreateDto(dto);
        entity.setEnsemble(ensemble);
        entity.setMember(member);
        entity.setVoice(resolveVoice(dto.getVoiceId()));
        entity.setInstrument(resolveInstrument(dto.getInstrumentId()));
        entity.setRole(dto.getRole() != null ? dto.getRole() : EnsembleRole.MEMBER);
        membershipRepository.persistAndFlush(entity);

        final List<EnsembleMembershipEntity> otherVoiceSeats = otherVoiceSeats(entity);
        if (dto.getRole() == null) {
            // Without an explicit role a new voice seat joins the role its voice siblings share
            sharedRole(entity, otherVoiceSeats).ifPresent(entity::setRole);
        } else if (entity.getVoice() != null) {
            // An explicit role on a new voice seat applies to the member's other voice seats
            otherVoiceSeats.forEach(other -> other.setRole(dto.getRole()));
        }
        return membershipMapper.toDto(entity);
    }

    public EnsembleMembership updateMember(final String memberId, final EnsembleMembership dto) {
        final EnsembleMembershipEntity entity = membershipRepository
                .findByIdOptional(UUID.fromString(memberId))
                .orElseThrow(() -> new EntityNotFoundException("EnsembleMembership", memberId));

        final EnsembleRole previousRole = entity.getRole();
        membershipMapper.update(entity, dto);
        entity.setVoice(resolveVoice(dto.getVoiceId()));
        entity.setInstrument(resolveInstrument(dto.getInstrumentId()));
        if (dto.getRole() == null) {
            // Restore before querying: the query flushes the seat, and the role column is not null
            entity.setRole(previousRole);
        }
        final List<EnsembleMembershipEntity> otherVoiceSeats = otherVoiceSeats(entity);
        if (entity.getRole() != previousRole) {
            // A changed role on a voice seat applies to the member's other voice seats
            if (entity.getVoice() != null) {
                otherVoiceSeats.forEach(other -> other.setRole(entity.getRole()));
            }
        } else {
            // A seat that moves onto a voice joins the role its voice siblings already share
            sharedRole(entity, otherVoiceSeats).ifPresent(entity::setRole);
        }
        return membershipMapper.toDto(entity);
    }

    public void deleteMember(final String memberId) {
        final EnsembleMembershipEntity entity = membershipRepository
                .findByIdOptional(UUID.fromString(memberId))
                .orElseThrow(() -> new EntityNotFoundException("EnsembleMembership", memberId));
        membershipRepository.delete(entity);
    }

    /**
     * The member's other voice seats in the seat's ensemble. A member's voice seats share one role;
     * the seat without a voice (e.g. the conductor's) keeps its own.
     */
    private List<EnsembleMembershipEntity> otherVoiceSeats(final EnsembleMembershipEntity seat) {
        return membershipRepository.list(
                "member = ?1 and ensemble = ?2 and voice is not null and id != ?3",
                seat.getMember(),
                seat.getEnsemble(),
                seat.getId());
    }

    /** The role the member's other voice seats share, if the seat is a voice seat and has siblings. */
    private static Optional<EnsembleRole> sharedRole(
            final EnsembleMembershipEntity seat, final List<EnsembleMembershipEntity> otherVoiceSeats) {
        if (seat.getVoice() == null || otherVoiceSeats.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(otherVoiceSeats.getFirst().getRole());
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
