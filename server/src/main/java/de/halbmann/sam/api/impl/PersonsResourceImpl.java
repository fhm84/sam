package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.PersonsResource;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.persons.PersonCredit;
import de.halbmann.sam.api.entity.persons.PersonFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.persons.controller.PersonService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;

@Authenticated
@RequestScoped
public class PersonsResourceImpl implements PersonsResource {

    @Inject
    PersonService personService;

    @Override
    public PaginatedResponse<Person> findPersons(PersonFilterRequest filterRequest) {
        return personService.findPersons(filterRequest);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public Person add(Person person) {
        return personService.addPerson(person);
    }

    @Override
    public Person load(String personId) {
        return personService.getPerson(personId);
    }

    @Override
    public List<PersonCredit> credits(String personId) {
        return personService.getCredits(personId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void update(String personId, Person person) {
        personService.updatePerson(personId, person);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void delete(String personId) {
        personService.deletePerson(personId);
    }
}
