package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.InstrumentsResource;
import de.halbmann.sam.api.entity.CreateInstrument;
import de.halbmann.sam.api.entity.Instrument;
import de.halbmann.sam.api.entity.InstrumentFilterRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.business.controller.InstrumentService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@Authenticated
@RequestScoped
public class InstrumentsResourceImpl implements InstrumentsResource {

    @Inject
    InstrumentService instrumentService;

    @Override
    public PaginatedResponse<Instrument> findInstruments(InstrumentFilterRequest filterRequest) {
        return instrumentService.findInstruments(filterRequest);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public Instrument add(CreateInstrument instrument) {
        return instrumentService.addInstrument(instrument);
    }

    @Override
    public Instrument load(String instrumentId) {
        return instrumentService.getInstrument(instrumentId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void update(String instrumentId, Instrument instrument) {
        instrumentService.updateInstrument(instrumentId, instrument);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void delete(String instrumentId) {
        instrumentService.deleteInstrument(instrumentId);
    }
}
