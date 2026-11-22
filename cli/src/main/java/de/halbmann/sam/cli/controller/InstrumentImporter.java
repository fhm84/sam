package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.instruments.Instrument;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class InstrumentImporter extends AbstractImporter<CreateInstrument, Instrument> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected Class<CreateInstrument> type() {
        return CreateInstrument.class;
    }

    @Override
    protected String describe(final CreateInstrument instrument) {
        return instrument.getDisplayName() != null ? instrument.getDisplayName() : instrument.getId();
    }

    @Override
    protected Optional<Instrument> findExisting(final CreateInstrument instrument) {
        return loadIfExists(() -> client.instruments().load(instrument.getId()));
    }

    @Override
    protected List<String> comparedFields() {
        return List.of(
                "name",
                "displayName",
                "family",
                "defaultClef",
                "aliases",
                "catalogSection",
                "catalogPosition",
                "transposition");
    }

    @Override
    protected void update(final Instrument existing) {
        client.instruments().update(existing.getId(), existing);
    }

    @Override
    protected void create(final CreateInstrument instrument) {
        Instrument created = client.instruments().add(instrument);
        System.out.println("  ✓ Created: " + created.getDisplayName() + " (ID: " + created.getId() + ")");
    }
}
