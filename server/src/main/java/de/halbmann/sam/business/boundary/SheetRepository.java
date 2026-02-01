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

    public PaginatedResponse<SheetMusicSearchResult> findSheets(final SheetFilterRequest filterRequest) {
        if (filterRequest.getQuery() != null) {
            String sql =
                    """
                            WITH q AS (
                                SELECT
                                    plainto_tsquery('simple', :query) AS tsq,
                                    :query AS raw,
                                    dmetaphone(:query) AS phonetic
                            )
                            SELECT s.*,
                                   -- metrics
                                   ts_rank(s.search_vector, q.tsq)    AS fts_rank,
                                   similarity(s.title, q.raw)         AS title_similarity,
                                   similarity(s.composer_name, q.raw) AS composer_similarity,
                                   (s.composer_phonetic = q.phonetic) AS phonetic_match,
                            
                                   -- final score
                                   (
                                       ts_rank(s.search_vector, q.tsq) * 0.70
                                     + similarity(s.title, q.raw) * 0.20
                                     + similarity(s.composer_name, q.raw) * 0.10
                                     + CASE
                                           WHEN s.composer_phonetic = q.phonetic THEN 0.05
                                           ELSE 0
                                       END
                                   ) AS final_rank
                            FROM sheets s, q
                            WHERE
                                  s.search_vector @@ q.tsq
                               OR s.title % q.raw
                               OR s.composer_name % q.raw
                               OR s.composer_phonetic = q.phonetic
                            ORDER BY
                                (
                                    ts_rank(s.search_vector, q.tsq) * 0.70
                                  + similarity(s.title, q.raw) * 0.20
                                  + similarity(s.composer_name, q.raw) * 0.10
                                  + CASE
                                        WHEN s.composer_phonetic = q.phonetic THEN 0.05
                                        ELSE 0
                                    END
                                ) DESC
                            """;

            List<Object[]> results = getEntityManager()
                    .createNativeQuery(sql, "SheetWithMetrics")
                    .setParameter("query", filterRequest.getQuery())
                    .setFirstResult(filterRequest.getPage() * filterRequest.getSize())
                    .setMaxResults(filterRequest.getSize())
                    .getResultList();

            PaginatedResponse<SheetMusicSearchResult> response = new PaginatedResponse<>();
            response.setData(results.stream()
                    .map(r -> new SheetMusicSearchResult(
                            sheetMusicMapper.toDto((SheetMusicEntity) r[0]),
                            new SearchResultMetrics(
                                    ((Number) r[1]).doubleValue(),
                                    ((Number) r[2]).doubleValue(),
                                    ((Number) r[3]).doubleValue(),
                                    (Boolean) r[4],
                                    ((Number) r[5]).doubleValue())))
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

            PaginatedResponse<SheetMusic> sheets = findSheets(filterRequest, parameters);
            PaginatedResponse<SheetMusicSearchResult> response = new PaginatedResponse<>();
            response.setPage(filterRequest.getPage());
            response.setSize(sheets.getSize());
            response.setTotalCount(sheets.getTotalCount());
            response.setData(
                    sheets.getData().stream().map(SheetMusicSearchResult::new).toList());
            return response;
        }
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

    PaginatedResponse<SheetMusic> findSheets(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final String filter =
                nonNullParams.keySet().stream().map(o -> o + "=:" + o).collect(Collectors.joining(" and "));

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
        response.setPage(paginationRequest.getPage());
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
