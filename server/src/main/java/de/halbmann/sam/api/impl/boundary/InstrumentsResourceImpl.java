package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.InstrumentsResource;
import de.halbmann.sam.api.entity.CreateInstrument;
import de.halbmann.sam.api.entity.Instrument;
import de.halbmann.sam.api.entity.InstrumentFilterRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.business.boundary.InstrumentRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class InstrumentsResourceImpl implements InstrumentsResource {

    @Inject
    InstrumentRepository instrumentRepository;

    @Override
    public PaginatedResponse<Instrument> findInstruments(InstrumentFilterRequest filterRequest) {
        if (filterRequest.getName() != null && !filterRequest.getName().isEmpty()) {
            return instrumentRepository.findInstruments(filterRequest);
        } else {
            return instrumentRepository.getAllInstruments(filterRequest);
        }
    }

    @Override
    public Instrument add(CreateInstrument instrument) {
        return instrumentRepository.addInstrument(instrument);
    }

    @Override
    public Instrument load(String instrumentId) {
        return instrumentRepository.getInstrument(instrumentId);
    }

    @Override
    public void update(String instrumentId, Instrument instrument) {
        instrumentRepository.updateInstrument(instrumentId, instrument);
    }

    @Override
    public void delete(String instrumentId) {
        instrumentRepository.deleteInstrument(instrumentId);
    }
}
