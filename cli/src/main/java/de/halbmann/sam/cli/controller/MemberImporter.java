package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberFilterRequest;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.persons.PersonFilterRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Imports member profiles. A member whose name matches an existing person (e.g. an arranger
 * imported with the sheets) becomes that person's profile instead of a second person.
 */
@Singleton
public class MemberImporter extends AbstractImporter<Member, Member> {

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

    /** A person file parses as a member too (unknown fields are ignored), but has no status. */
    @Override
    protected List<String> extraValidation(final Member member) {
        return member.getStatus() == null
                ? List.of("status missing: not a member export (persons are imported with importPerson)")
                : List.of();
    }

    @Override
    protected Optional<Member> findExisting(final Member member) {
        if (member.getId() != null) {
            Optional<Member> byId =
                    loadIfExists(() -> client.members().load(member.getId().toString()));
            if (byId.isPresent()) {
                return byId;
            }
        }
        // The server stores names trimmed, so compare them trimmed too
        String name = member.getName().trim();
        MemberFilterRequest filter = new MemberFilterRequest();
        filter.setName(name);
        filter.setSize(-1);
        return single(
                client.members().findMembers(filter).getData().stream()
                        .filter(existing -> name.equalsIgnoreCase(existing.getName()))
                        .toList(),
                "the name " + name);
    }

    /**
     * The profile's own fields. Not the login link, invite and onboarding state: they belong to
     * one environment (see {@link #create}).
     */
    @Override
    protected List<String> comparedFields() {
        return List.of("name", "instruments", "contact", "status", "leftAt");
    }

    @Override
    protected void update(final Member existing) {
        client.members().update(existing.getId().toString(), existing);
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
        System.out.println("  ✓ Created: " + created.getName() + " (ID: " + created.getId() + ")");
    }

    /**
     * The person with exactly this name, if there is one. With several, picking one would be a
     * guess that can give a composer someone else's contact data, so the import stops instead.
     */
    private Optional<UUID> existingPersonId(final String name) {
        PersonFilterRequest filter = new PersonFilterRequest();
        filter.setName(name);
        filter.setSize(-1);
        List<UUID> ids = client.persons().findPersons(filter).getData().stream()
                .filter(person -> name.equalsIgnoreCase(person.getName()))
                .map(Person::getId)
                .toList();
        if (ids.size() > 1) {
            throw new IllegalStateException(ids.size() + " persons are named '" + name
                    + "'; create this member in the UI and pick the right person there");
        }
        return ids.stream().findFirst();
    }
}
