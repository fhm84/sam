package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianFilterRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class MusicianImporter extends AbstractImporter<Musician> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected Class<Musician> type() {
        return Musician.class;
    }

    @Override
    protected String describe(final Musician musician) {
        return musician.getName();
    }

    @Override
    protected boolean exists(final Musician musician) {
        MusicianFilterRequest filter = new MusicianFilterRequest();
        filter.setName(musician.getName());
        filter.setSize(-1);
        return client.musicians().findMusicians(filter).getData().stream()
                .anyMatch(existing -> musician.getName().equalsIgnoreCase(existing.getName()));
    }

    @Override
    protected void create(final Musician musician) {
        // environment-specific links must not cross environments: userId is an OIDC subject from
        // the source Keycloak, membership references a source-side ensemble
        musician.setUserId(null);
        musician.setMembership(null);
        musician.setId(null);
        Musician created = client.musicians().add(musician);
        System.out.println("  ✓ Imported: " + created.getName() + " (ID: " + created.getId() + ")");
    }
}
