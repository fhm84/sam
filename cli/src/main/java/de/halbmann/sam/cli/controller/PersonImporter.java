package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.persons.PersonFilterRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class PersonImporter extends AbstractImporter<Person> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected Class<Person> type() {
        return Person.class;
    }

    @Override
    protected String describe(final Person person) {
        return person.getName();
    }

    @Override
    protected boolean exists(final Person person) {
        // The server stores names trimmed, so compare them trimmed too
        String name = person.getName().trim();
        PersonFilterRequest filter = new PersonFilterRequest();
        filter.setName(name);
        filter.setSize(-1);
        return client.persons().findPersons(filter).getData().stream()
                .anyMatch(existing -> name.equalsIgnoreCase(existing.getName()));
    }

    @Override
    protected void create(final Person person) {
        person.setId(null);
        Person created = client.persons().add(person);
        System.out.println("  ✓ Imported: " + created.getName() + " (ID: " + created.getId() + ")");
    }
}
