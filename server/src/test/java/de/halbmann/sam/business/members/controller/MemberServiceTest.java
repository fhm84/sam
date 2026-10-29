package de.halbmann.sam.business.members.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MembershipStatus;
import de.halbmann.sam.business.ensembles.boundary.EnsembleMembershipRepository;
import de.halbmann.sam.business.members.boundary.MemberRepository;
import de.halbmann.sam.business.members.entity.MemberEntity;
import de.halbmann.sam.business.persons.boundary.PersonRepository;
import de.halbmann.sam.business.persons.controller.PersonService;
import de.halbmann.sam.business.persons.entity.PersonEntity;
import de.halbmann.sam.core.exception.ConflictException;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.ValidationException;
import jakarta.enterprise.event.Event;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PersonRepository personRepository;

    @Mock
    PersonService personService;

    @Mock
    EnsembleMembershipRepository membershipRepository;

    @Mock
    MemberMapper memberMapper;

    @Mock
    Event<MemberErased> memberErased;

    @InjectMocks
    MemberService service;

    private static MemberEntity member(UUID id) {
        PersonEntity person = new PersonEntity();
        person.setId(id);
        person.setName("Hans Example");
        MemberEntity member = new MemberEntity();
        member.setPerson(person);
        return member;
    }

    private static Member dto(MembershipStatus status) {
        Member dto = new Member();
        dto.setName("Hans Example");
        dto.setStatus(status);
        return dto;
    }

    // ── linkUser / unlinkUser ─────────────────────────────────────────────

    @Test
    void linkUser_setsUserIdOnEntity() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        service.linkUser(id.toString(), "user-abc");

        assertEquals("user-abc", entity.getUserId());
    }

    @Test
    void linkUser_unknownMember_throwsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.linkUser(id.toString(), "user-abc"));
    }

    @Test
    void linkUser_formerMember_isAConflict() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        entity.setMembershipStatus(MembershipStatus.FORMER);
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        assertThrows(ConflictException.class, () -> service.linkUser(id.toString(), "user-abc"));
        assertNull(entity.getUserId());
    }

    @Test
    void unlinkUser_clearsUserId() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        entity.setUserId("user-abc");
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        service.unlinkUser(id.toString());

        assertNull(entity.getUserId());
    }

    // ── updateMember ──────────────────────────────────────────────────────

    @Test
    void updateMember_doesNotClearUserId() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        entity.setUserId("user-abc");
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        service.updateMember(id.toString(), dto(MembershipStatus.ACTIVE));

        assertEquals(
                "user-abc",
                entity.getUserId(),
                "updateMember must not clear the userId set by the dedicated linkUser endpoint");
    }

    @Test
    void updateMember_writesTheNameThroughToThePerson() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));
        Member dto = dto(null);
        dto.setName("  Hans Renamed ");

        service.updateMember(id.toString(), dto);

        assertEquals("Hans Renamed", entity.getPerson().getName());
    }

    @Test
    void updateMember_leaving_setsLeftAtAndEndsTheLoginLink() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        entity.setUserId("user-abc");
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        service.updateMember(id.toString(), dto(MembershipStatus.FORMER));

        assertEquals(MembershipStatus.FORMER, entity.getMembershipStatus());
        assertEquals(LocalDate.now(), entity.getLeftAt());
        assertNull(entity.getUserId());
    }

    @Test
    void updateMember_returning_clearsLeftAt() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        entity.setMembershipStatus(MembershipStatus.FORMER);
        entity.setLeftAt(LocalDate.of(2026, 1, 31));
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        service.updateMember(id.toString(), dto(MembershipStatus.ACTIVE));

        assertNull(entity.getLeftAt());
    }

    @Test
    void updateMember_pausing_keepsTheLoginLink() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        entity.setUserId("user-abc");
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        service.updateMember(id.toString(), dto(MembershipStatus.PAUSED));

        assertEquals("user-abc", entity.getUserId());
        assertNull(entity.getLeftAt());
    }

    // ── addMember ─────────────────────────────────────────────────────────

    @Test
    void addMember_withoutPersonOrName_isRejected() {
        Member dto = dto(null);
        dto.setName(" ");

        assertThrows(ValidationException.class, () -> service.addMember(dto));
        verify(personRepository, never()).persist(any(PersonEntity.class));
    }

    @Test
    void addMember_forAPersonThatIsAlreadyAMember_isAConflict() {
        UUID id = UUID.randomUUID();
        MemberEntity existing = member(id);
        when(personRepository.findByIdOptional(id)).thenReturn(Optional.of(existing.getPerson()));
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(existing));
        Member dto = dto(null);
        dto.setId(id);

        assertThrows(ConflictException.class, () -> service.addMember(dto));
    }

    // ── deleteMember ──────────────────────────────────────────────────────

    @Test
    void deleteMember_keepsAPersonThatIsCredited() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));
        when(personService.countCredits(entity.getPerson())).thenReturn(2L);

        service.deleteMember(id.toString());

        verify(memberRepository).delete(entity);
        verify(personRepository, never()).delete(any(PersonEntity.class));
        verify(memberErased).fire(new MemberErased(id, false));
    }

    @Test
    void deleteMember_deletesAPersonThatIsNotCredited() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = member(id);
        when(memberRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));
        when(personService.countCredits(entity.getPerson())).thenReturn(0L);

        service.deleteMember(id.toString());

        verify(personRepository).delete(entity.getPerson());
        verify(memberErased).fire(new MemberErased(id, true));
    }
}
