package de.halbmann.sam.business.members.controller;

import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberContact;
import de.halbmann.sam.api.entity.members.MemberFilterRequest;
import de.halbmann.sam.api.entity.members.MemberInstrument;
import de.halbmann.sam.api.entity.members.MembershipStatus;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.ensembles.boundary.EnsembleMembershipRepository;
import de.halbmann.sam.business.instruments.boundary.InstrumentRepository;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.members.boundary.MemberRepository;
import de.halbmann.sam.business.members.entity.MemberEntity;
import de.halbmann.sam.business.members.entity.MemberInstrumentEntity;
import de.halbmann.sam.business.persons.boundary.PersonRepository;
import de.halbmann.sam.business.persons.controller.PersonService;
import de.halbmann.sam.business.persons.entity.PersonEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.ConflictException;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class MemberService {

    @Inject
    MemberRepository memberRepository;

    @Inject
    PersonRepository personRepository;

    @Inject
    PersonService personService;

    @Inject
    InstrumentRepository instrumentRepository;

    @Inject
    MemberMapper memberMapper;

    @Inject
    EnsembleMembershipRepository membershipRepository;

    @Inject
    Event<MemberErased> memberErased;

    public PaginatedResponse<Member> findMembers(final MemberFilterRequest filterRequest) {
        PaginatedEntities<MemberEntity> result = memberRepository.findPage(filterRequest);
        PaginatedResponse<Member> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(memberMapper::toDto).toList());
        response.setPage(filterRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public Member getMember(final String memberId) {
        return memberMapper.toDto(find(memberId));
    }

    /**
     * Creates the profile of the person with {@code member.id}, or of a new person named
     * {@code member.name}. A name is never matched against existing persons: two people can share
     * one, so the caller picks the existing person explicitly.
     */
    public Member addMember(final Member member) {
        final PersonEntity person;
        if (member.getId() != null) {
            person = personRepository
                    .findByIdOptional(member.getId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("Person", member.getId().toString()));
            if (memberRepository.findByIdOptional(person.getId()).isPresent()) {
                throw new ConflictException("Person '" + person.getName() + "' is already a member");
            }
            applyName(person, member.getName());
        } else {
            if (member.getName() == null || member.getName().isBlank()) {
                throw new ValidationException("A new member needs a name");
            }
            person = new PersonEntity();
            applyName(person, member.getName());
            personRepository.persist(person);
        }

        final MemberEntity entity = new MemberEntity();
        entity.setPerson(person);
        applyFields(entity, member);
        memberRepository.persistAndFlush(entity);
        applyInstruments(entity, member.getInstruments());
        return memberMapper.toDto(entity);
    }

    /** Updates everything but the login link, which only the dedicated link endpoints change. */
    public void updateMember(final String memberId, final Member member) {
        final MemberEntity entity = find(memberId);
        applyName(entity.getPerson(), member.getName());
        applyFields(entity, member);
        applyInstruments(entity, member.getInstruments());
    }

    /** A former member gets no login: leaving the band ends access (and every save would drop it). */
    public void linkUser(final String memberId, final String userId) {
        final MemberEntity entity = find(memberId);
        if (entity.getMembershipStatus() == MembershipStatus.FORMER) {
            throw new ConflictException("Member '" + entity.getPerson().getName()
                    + "' has left the band; set them active or paused before linking a login");
        }
        entity.setUserId(userId);
    }

    public void unlinkUser(final String memberId) {
        find(memberId).setUserId(null);
    }

    /**
     * Erases a member: the profile, its instruments and ensemble seats, and (after commit) their
     * audit history. The person stays when it is credited on a sheet, since the credit is printed on
     * the music; otherwise it goes too.
     */
    public void deleteMember(final String memberId) {
        final MemberEntity entity = find(memberId);
        final PersonEntity person = entity.getPerson();
        membershipRepository.list("member", entity).forEach(membershipRepository::delete);
        memberRepository.delete(entity);
        final boolean deletePerson = personService.countCredits(person) == 0;
        if (deletePerson) {
            personRepository.delete(person);
        }
        memberErased.fire(new MemberErased(person.getId(), deletePerson));
    }

    private void applyName(final PersonEntity person, final String name) {
        if (name != null && !name.isBlank()) {
            person.setName(name.trim());
        }
    }

    private void applyFields(final MemberEntity entity, final Member member) {
        final MemberContact contact = member.getContact();
        entity.setEmail(contact != null ? contact.getEmail() : null);
        entity.setMobile(contact != null ? contact.getMobile() : null);
        entity.setNotes(contact != null ? contact.getNotes() : null);
        if (member.getStatus() != null) {
            entity.setMembershipStatus(member.getStatus());
        }
        if (entity.getMembershipStatus() == MembershipStatus.FORMER) {
            // Leaving starts the retention clock and ends access at once
            if (member.getLeftAt() != null) {
                entity.setLeftAt(member.getLeftAt());
            } else if (entity.getLeftAt() == null) {
                entity.setLeftAt(LocalDate.now());
            }
            entity.setUserId(null);
        } else {
            entity.setLeftAt(null);
        }
    }

    private void applyInstruments(final MemberEntity entity, final List<MemberInstrument> instruments) {
        entity.getInstruments().clear();
        if (instruments == null || instruments.isEmpty()) {
            return;
        }
        for (MemberInstrument mi : instruments) {
            final InstrumentEntity instrumentEntity = instrumentRepository
                    .findByIdOptional(mi.getInstrumentId())
                    .orElseThrow(() -> new EntityNotFoundException("Instrument", mi.getInstrumentId()));
            final MemberInstrumentEntity mie = new MemberInstrumentEntity();
            mie.setMember(entity);
            mie.setInstrument(instrumentEntity);
            mie.setPrimary(mi.isPrimary());
            entity.getInstruments().add(mie);
        }
    }

    private MemberEntity find(final String memberId) {
        return memberRepository
                .findByIdOptional(UUID.fromString(memberId))
                .orElseThrow(() -> new EntityNotFoundException("Member", memberId));
    }
}
