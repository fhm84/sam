package de.halbmann.sam.business.persons.controller;

import de.halbmann.sam.api.entity.persons.CreditRole;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.persons.PersonCredit;
import de.halbmann.sam.api.entity.persons.PersonFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.members.boundary.MemberRepository;
import de.halbmann.sam.business.persons.boundary.PersonRepository;
import de.halbmann.sam.business.persons.entity.PersonEntity;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.ConflictException;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
@Transactional
public class PersonService {

    @Inject
    PersonRepository personRepository;

    @Inject
    PersonMapper personMapper;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    MemberRepository memberRepository;

    public PaginatedResponse<Person> findPersons(final PersonFilterRequest filterRequest) {
        PaginatedEntities<PersonEntity> result =
                filterRequest.getName() != null && !filterRequest.getName().isBlank()
                        ? personRepository.searchByName(
                                filterRequest.getName(), filterRequest.getPage(), filterRequest.getSize())
                        : personRepository.findPage(filterRequest);
        PaginatedResponse<Person> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(personMapper::toDto).toList());
        response.setPage(filterRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public Person getPerson(final String personId) {
        return personMapper.toDto(find(personId));
    }

    public Person addPerson(final Person person) {
        PersonEntity entity = personMapper.fromDto(person);
        entity.setName(requireName(person));
        personRepository.persistAndFlush(entity);
        return personMapper.toDto(entity);
    }

    public void updatePerson(final String personId, final Person person) {
        final PersonEntity entity = find(personId);
        personMapper.update(entity, person);
        entity.setName(requireName(person));
    }

    public void deletePerson(final String personId) {
        final PersonEntity entity = find(personId);
        // Refuse with a clear 409 instead of letting the foreign keys fail with a 500
        long sheets = countCredits(entity);
        if (sheets > 0) {
            throw new ConflictException("Person '" + entity.getName() + "' is still credited on " + sheets
                    + " sheet(s) as composer/arranger");
        }
        if (memberRepository.findByIdOptional(entity.getId()).isPresent()) {
            throw new ConflictException(
                    "Person '" + entity.getName() + "' is a member; delete the member profile instead");
        }
        personRepository.delete(entity);
    }

    /**
     * The person a credit refers to: by ID while that person exists, otherwise by exact (trimmed)
     * name, otherwise a new person with that name. {@code null} when there is neither.
     */
    public PersonEntity resolveOrCreate(final UUID id, final String name) {
        if (id != null) {
            final Optional<PersonEntity> found = personRepository.findByIdOptional(id);
            if (found.isPresent()) {
                return found.get();
            }
        }
        if (name == null || name.isBlank()) {
            return null;
        }
        return personRepository.findByName(name).orElseGet(() -> {
            final PersonEntity person = new PersonEntity();
            person.setName(name.trim());
            personRepository.persist(person);
            return person;
        });
    }

    /** The sheets that credit the person, one entry per credit, sorted by title. */
    public List<PersonCredit> getCredits(final String personId) {
        final PersonEntity person = find(personId);
        return sheetRepository.list("composer = ?1 or arranger = ?1 order by title", person).stream()
                .flatMap(sheet -> Stream.of(
                                isPerson(sheet.getComposer(), person) ? credit(sheet, CreditRole.COMPOSER) : null,
                                isPerson(sheet.getArranger(), person) ? credit(sheet, CreditRole.ARRANGER) : null)
                        .filter(Objects::nonNull))
                .toList();
    }

    private static boolean isPerson(final PersonEntity credited, final PersonEntity person) {
        return credited != null && credited.getId().equals(person.getId());
    }

    private static PersonCredit credit(final SheetMusicEntity sheet, final CreditRole role) {
        final PersonCredit credit = new PersonCredit();
        credit.setSheetId(sheet.getId());
        credit.setTitle(sheet.getTitle());
        credit.setSubtitle(sheet.getSubtitle());
        credit.setRole(role);
        return credit;
    }

    /** Number of sheets that credit the person as composer or arranger. */
    public long countCredits(final PersonEntity person) {
        return sheetRepository.count("composer = ?1 or arranger = ?1", person);
    }

    private static String requireName(final Person person) {
        if (person.getName() == null || person.getName().isBlank()) {
            throw new ValidationException("A person needs a name");
        }
        return person.getName().trim();
    }

    private PersonEntity find(final String personId) {
        return personRepository
                .findByIdOptional(UUID.fromString(personId))
                .orElseThrow(() -> new EntityNotFoundException("Person", personId));
    }
}
