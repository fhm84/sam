package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.controller.SheetMusicMapper;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class SheetRepository implements PanacheRepositoryBase<SheetMusicEntity, UUID> {

    @Inject
    SheetMusicMapper sheetMusicMapper;

    public PaginatedResponse<SheetMusic> getAllSheets(final PaginationRequest paginationRequest) {
        return findSheets(paginationRequest, Map.of());
    }

    public PaginatedResponse<SheetMusic> findSheets(final SheetFilterRequest filterRequest) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", filterRequest.getTitle());

        return findSheets(filterRequest, parameters);
    }

    private Sort prepareSort(PaginationRequest paginationRequest) {
        final Sort sort;
        if (paginationRequest.getSortBy() != null) {
            if (SortOrder.DESC == paginationRequest.getSortOrder()) {
                sort = Sort.descending(paginationRequest.getSortBy());
            } else {
                // default/fallback: ASC
                sort = Sort.ascending(paginationRequest.getSortBy());
            }
        } else {
            sort = Sort.ascending("title");
        }
        return sort;
    }

    PaginatedResponse<SheetMusic> findSheets(final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final String filter = nonNullParams.keySet().stream()
                .map(o -> o + "=:" + o)
                .collect(Collectors.joining(" and "));

        final Sort sort = prepareSort(paginationRequest);

        // get the total count
        long totalItems;
        PanacheQuery<SheetMusicEntity> sheetQuery;
        if (nonNullParams.isEmpty()) {
            sheetQuery = findAll(sort);
            totalItems = count();
        } else {
            sheetQuery = find(filter, nonNullParams);
            totalItems = count(filter, nonNullParams);
        }
        final List<SheetMusicEntity> sheets = sheetQuery
                .page(paginationRequest.getPage(), paginationRequest.getSize())
                .list();

        // Wrap the result into a PaginatedResponse
        PaginatedResponse<SheetMusic> response = new PaginatedResponse<>();
        response.setData(sheets.stream().map(sheetMusicMapper::toDto).toList());
        response.setSize(response.getData().size());
        response.setTotalCount(totalItems);

        return response;
    }

    public SheetMusic getSheet(final String sheetId) {
        final SheetMusicEntity sheetMusicEntity = findById(UUID.fromString(sheetId));

        return sheetMusicMapper.toDto(sheetMusicEntity);
    }

    public SheetMusic addSheet(final SheetMusic sheetMusic) {
        final SheetMusicEntity sheetMusicEntity = sheetMusicMapper.fromDto(sheetMusic);
        persistAndFlush(sheetMusicEntity);
        return sheetMusicMapper.toDto(sheetMusicEntity);
    }

    public void updateSheet(final String sheetId, final SheetMusic sheetMusic) {
        final SheetMusicEntity sheetMusicEntity = findById(UUID.fromString(sheetId));
        sheetMusicMapper.update(sheetMusicEntity, sheetMusic);
    }

    public void deleteSheet(final String sheetId) {
        final SheetMusicEntity sheetMusicEntity = findById(UUID.fromString(sheetId));
        delete(sheetMusicEntity);
    }

}
