package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.instruments.Instrument;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class InstrumentImporter extends AbstractImporter<CreateInstrument> {

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
    protected boolean exists(final CreateInstrument instrument) {
        try {
            client.instruments().load(instrument.getId());
            return true;
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    protected void create(final CreateInstrument instrument) {
        Instrument created = client.instruments().add(instrument);
        System.out.println("  ✓ Imported: " + created.getDisplayName() + " (ID: " + created.getId() + ")");
    }
}
