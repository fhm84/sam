package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.boundary.SheetRepository;
import de.halbmann.sam.business.entity.PaginatedEntities;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class SheetService {

    @Inject
    SheetRepository sheetRepository;

    @Inject
    SheetMusicMapper sheetMusicMapper;

    public PaginatedResponse<SheetMusicSearchResult> findSheets(final SheetFilterRequest filterRequest) {
        if (filterRequest.getQuery() != null) {
            List<Object[]> results = sheetRepository.searchSheets(
                    filterRequest.getQuery(), filterRequest.getPage(), filterRequest.getSize());

            PaginatedResponse<SheetMusicSearchResult> response = new PaginatedResponse<>();
            response.setData(results.stream()
                    .map(r -> new SheetMusicSearchResult(
                            sheetMusicMapper.toDto((SheetMusicEntity) r[0]),
                            new SearchResultMetrics(
                                    Optional.ofNullable((Number) r[1])
                                            .map(Number::doubleValue)
                                            .orElse(0.0),
                                    Optional.ofNullable((Number) r[2])
                                            .map(Number::doubleValue)
                                            .orElse(0.0),
                                    Optional.ofNullable((Number) r[3])
                                            .map(Number::doubleValue)
                                            .orElse(0.0),
                                    (Boolean) r[4],
                                    Optional.ofNullable((Number) r[5])
                                            .map(Number::doubleValue)
                                            .orElse(0.0))))
                    .toList());
            response.setPage(filterRequest.getPage());
            response.setSize(response.getData().size());
            return response;
        } else {
            final Map<String, Object> parameters = new HashMap<>();
            if (filterRequest.getTitle() != null) {
                parameters.put("title", filterRequest.getTitle());
            }
            if (filterRequest.getComposer() != null) {
                parameters.put("composer.name", filterRequest.getComposer());
            }
            if (filterRequest.getGenre() != null) {
                parameters.put("genre", filterRequest.getGenre());
            }

            PaginatedResponse<SheetMusic> sheets = getAllSheets(filterRequest, parameters);
            PaginatedResponse<SheetMusicSearchResult> response = new PaginatedResponse<>();
            response.setPage(filterRequest.getPage());
            response.setSize(sheets.getSize());
            response.setTotalCount(sheets.getTotalCount());
            response.setData(
                    sheets.getData().stream().map(SheetMusicSearchResult::new).toList());
            return response;
        }
    }

    private PaginatedResponse<SheetMusic> getAllSheets(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        PaginatedEntities<SheetMusicEntity> result = sheetRepository.findSheetEntities(paginationRequest, parameters);

        PaginatedResponse<SheetMusic> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(sheetMusicMapper::toDto).toList());
        response.setPage(paginationRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public SheetMusic getSheet(final String sheetId) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        return sheetMusicMapper.toDto(entity);
    }

    public SheetMusic addSheet(final CreateSheetMusic sheetMusic) {
        final SheetMusicEntity entity = sheetMusicMapper.fromDto(sheetMusic);
        sheetRepository.persistAndFlush(entity);
        return sheetMusicMapper.toDto(entity);
    }

    public void updateSheet(final String sheetId, final SheetMusic sheetMusic) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        sheetMusicMapper.update(entity, sheetMusic);
    }

    public List<String> getDistinctGenres() {
        return sheetRepository.listDistinctGenres();
    }

    public void deleteSheet(final String sheetId) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        sheetRepository.delete(entity);
    }
}
