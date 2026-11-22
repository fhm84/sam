package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.persons.PersonFilterRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class PersonImporter extends AbstractImporter<Person, Person> {

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
    protected Optional<Person> findExisting(final Person person) {
        if (person.getId() != null) {
            Optional<Person> byId =
                    loadIfExists(() -> client.persons().load(person.getId().toString()));
            if (byId.isPresent()) {
                return byId;
            }
        }
        // The server stores names trimmed, so compare them trimmed too
        String name = person.getName().trim();
        PersonFilterRequest filter = new PersonFilterRequest();
        filter.setName(name);
        filter.setSize(-1);
        return single(
                client.persons().findPersons(filter).getData().stream()
                        .filter(existing -> name.equalsIgnoreCase(existing.getName()))
                        .toList(),
                "the name " + name);
    }

    @Override
    protected List<String> comparedFields() {
        return List.of("name", "birthYear", "deathYear", "ipi");
    }

    @Override
    protected void update(final Person existing) {
        client.persons().update(existing.getId().toString(), existing);
    }

    @Override
    protected void create(final Person person) {
        person.setId(null);
        Person created = client.persons().add(person);
        System.out.println("  ✓ Created: " + created.getName() + " (ID: " + created.getId() + ")");
    }
}
