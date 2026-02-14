package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.controller.InstrumentMapper;
import de.halbmann.sam.business.entity.InstrumentEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class InstrumentRepository implements PanacheRepositoryBase<InstrumentEntity, String> {

    @Inject
    InstrumentMapper instrumentMapper;

    public Instrument addInstrument(final CreateInstrument instrument) {
        InstrumentEntity entity = instrumentMapper.fromDto(instrument);
        persistAndFlush(entity);
        return instrumentMapper.toDto(entity);
    }

    public PaginatedResponse<Instrument> getAllInstruments(final PaginationRequest paginationRequest) {
        return findInstruments(paginationRequest, Map.of());
    }

    public Instrument getInstrument(final String instrumentId) {
        InstrumentEntity entity = findById(instrumentId);
        return instrumentMapper.toDto(entity);
    }

    public PaginatedResponse<Instrument> findInstruments(final InstrumentFilterRequest filterRequest) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", filterRequest.getName());
        return findInstruments(filterRequest, parameters);
    }

    PaginatedResponse<Instrument> findInstruments(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final String filter =
                nonNullParams.keySet().stream().map(o -> o + "=:" + o).collect(Collectors.joining(" and "));

        final Sort sort = prepareSort(paginationRequest);

        long totalItems;
        PanacheQuery<InstrumentEntity> query;
        if (nonNullParams.isEmpty()) {
            query = findAll(sort);
            totalItems = count();
        } else {
            query = find(filter, nonNullParams);
            totalItems = count(filter, nonNullParams);
        }
        final List<InstrumentEntity> instruments = query.page(paginationRequest.getPage(), paginationRequest.getSize())
                .list();

        PaginatedResponse<Instrument> response = new PaginatedResponse<>();
        response.setData(instruments.stream().map(instrumentMapper::toDto).toList());
        response.setPage(paginationRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(totalItems);

        return response;
    }

    private Sort prepareSort(PaginationRequest paginationRequest) {
        if (paginationRequest.getSortBy() != null) {
            if (SortOrder.DESC == paginationRequest.getSortOrder()) {
                return Sort.descending(paginationRequest.getSortBy());
            } else {
                return Sort.ascending(paginationRequest.getSortBy());
            }
        }
        return Sort.ascending("name");
    }

    public void updateInstrument(final String instrumentId, final Instrument instrument) {
        final InstrumentEntity entity = findById(instrumentId);
        instrumentMapper.update(entity, instrument);
    }

    public void deleteInstrument(final String instrumentId) {
        final InstrumentEntity entity = findById(instrumentId);
        delete(entity);
    }
}
