package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.boundary.InstrumentRepository;
import de.halbmann.sam.business.entity.InstrumentEntity;
import de.halbmann.sam.business.entity.PaginatedEntities;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@Transactional
public class InstrumentService {

    @Inject
    InstrumentRepository instrumentRepository;

    @Inject
    InstrumentMapper instrumentMapper;

    public PaginatedResponse<Instrument> findInstruments(final InstrumentFilterRequest filterRequest) {
        final Map<String, Object> parameters = new HashMap<>();
        if (filterRequest.getName() != null && !filterRequest.getName().isEmpty()) {
            parameters.put("name", filterRequest.getName());
        }
        if (filterRequest.getTransposition() != null) {
            parameters.put("transposition", filterRequest.getTransposition());
        }
        return findInstruments(filterRequest, parameters);
    }

    private PaginatedResponse<Instrument> findInstruments(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        PaginatedEntities<InstrumentEntity> result =
                instrumentRepository.findInstrumentEntities(paginationRequest, parameters);

        PaginatedResponse<Instrument> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(instrumentMapper::toDto).toList());
        response.setPage(paginationRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public Instrument getInstrument(final String instrumentId) {
        InstrumentEntity entity = instrumentRepository
                .findByIdOptional(instrumentId)
                .orElseThrow(() -> new EntityNotFoundException("Instrument", instrumentId));
        return instrumentMapper.toDto(entity);
    }

    public Instrument addInstrument(final CreateInstrument instrument) {
        InstrumentEntity entity = instrumentMapper.fromDto(instrument);
        instrumentRepository.persistAndFlush(entity);
        return instrumentMapper.toDto(entity);
    }

    public void updateInstrument(final String instrumentId, final Instrument instrument) {
        final InstrumentEntity entity = instrumentRepository
                .findByIdOptional(instrumentId)
                .orElseThrow(() -> new EntityNotFoundException("Instrument", instrumentId));
        instrumentMapper.update(entity, instrument);
    }

    public void deleteInstrument(final String instrumentId) {
        final InstrumentEntity entity = instrumentRepository
                .findByIdOptional(instrumentId)
                .orElseThrow(() -> new EntityNotFoundException("Instrument", instrumentId));
        instrumentRepository.delete(entity);
    }
}
