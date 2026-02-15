package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.PaginationRequest;
import de.halbmann.sam.api.entity.SortOrder;
import de.halbmann.sam.business.entity.PaginatedEntities;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class SheetRepository implements PanacheRepositoryBase<SheetMusicEntity, UUID> {

    @SuppressWarnings("unchecked")
    public List<Object[]> searchSheets(final String query, final int page, final int size) {
        String sql = """
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

        return getEntityManager()
                .createNativeQuery(sql, "SheetWithMetrics")
                .setParameter("query", query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public PaginatedEntities<SheetMusicEntity> findSheetEntities(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final String filter =
                nonNullParams.keySet().stream().map(o -> o + "=:" + o).collect(Collectors.joining(" and "));

        final Sort sort = prepareSort(paginationRequest);

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

        return new PaginatedEntities<>(sheets, totalItems);
    }

    private Sort prepareSort(PaginationRequest paginationRequest) {
        if (paginationRequest.getSortBy() != null) {
            if (SortOrder.DESC == paginationRequest.getSortOrder()) {
                return Sort.descending(paginationRequest.getSortBy());
            } else {
                return Sort.ascending(paginationRequest.getSortBy());
            }
        }
        return Sort.ascending("title");
    }
}
