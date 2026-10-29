package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberFilterRequest;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.persons.PersonFilterRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Imports member profiles. A member whose name matches an existing person (e.g. an arranger
 * imported with the sheets) becomes that person's profile instead of a second person.
 */
@Singleton
public class MemberImporter extends AbstractImporter<Member> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected Class<Member> type() {
        return Member.class;
    }

    @Override
    protected String describe(final Member member) {
        return member.getName();
    }

    @Override
    protected boolean exists(final Member member) {
        // The server stores names trimmed, so compare them trimmed too
        String name = member.getName().trim();
        MemberFilterRequest filter = new MemberFilterRequest();
        filter.setName(name);
        filter.setSize(-1);
        return client.members().findMembers(filter).getData().stream()
                .anyMatch(existing -> name.equalsIgnoreCase(existing.getName()));
    }

    @Override
    protected void create(final Member member) {
        // environment-specific links must not cross environments: userId is an OIDC subject from
        // the source login provider, the invite went out from the source system
        member.setUserId(null);
        member.setLastInviteSentAt(null);
        member.setOnboarding(null);
        member.setId(existingPersonId(member.getName().trim()).orElse(null));
        Member created = client.members().add(member);
        System.out.println("  ✓ Imported: " + created.getName() + " (ID: " + created.getId() + ")");
    }

    private Optional<UUID> existingPersonId(final String name) {
        PersonFilterRequest filter = new PersonFilterRequest();
        filter.setName(name);
        filter.setSize(-1);
        return client.persons().findPersons(filter).getData().stream()
                .filter(person -> name.equalsIgnoreCase(person.getName()))
                .map(Person::getId)
                .findFirst();
    }
}
