package de.halbmann.sam.business.sheets.boundary;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.shared.SortOrder;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.controller.SortFieldValidator;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class SheetRepository implements PanacheRepositoryBase<SheetMusicEntity, UUID> {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title",
            "subtitle",
            "publisher",
            "publisherIpi",
            "originalBy",
            "genre",
            "style",
            "difficultyLevel",
            "duration",
            "favorite",
            "yearOfComposition",
            "edition",
            "copyright",
            "rating",
            "iswc",
            "gemaWorkNumber",
            "additionalNotes");

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

    public Optional<SheetMusicEntity> findByTitle(String title) {
        return find("lower(title) = lower(:title)", Map.of("title", title)).firstResultOptional();
    }

    /**
     * Returns up to {@code limit} sheets whose title is similar to {@code title},
     * ordered by trigram similarity. Requires the {@code pg_trgm} extension.
     */
    public List<SheetMusicEntity> findCandidatesByTitle(String title, double threshold, int limit) {
        return getEntityManager()
                .createQuery("""
                        SELECT s FROM SheetMusicEntity s
                        WHERE FUNCTION('similarity', LOWER(s.title), LOWER(:title)) >= :threshold
                        ORDER BY FUNCTION('similarity', LOWER(s.title), LOWER(:title)) DESC
                        """, SheetMusicEntity.class)
                .setParameter("title", title)
                .setParameter("threshold", threshold)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<String> listAvailableFirstLetters(String genre) {
        String jpql = genre != null
                ? "SELECT DISTINCT UPPER(SUBSTRING(s.title, 1, 1)) FROM SheetMusicEntity s WHERE s.genre.name = :genre"
                : "SELECT DISTINCT UPPER(SUBSTRING(s.title, 1, 1)) FROM SheetMusicEntity s";
        var query = getEntityManager().createQuery(jpql, String.class);
        if (genre != null) {
            query.setParameter("genre", genre);
        }
        return query.getResultList().stream().sorted().toList();
    }

    public List<String> listDistinctGenres() {
        return Arrays.stream(Genre.values()).map(Genre::name).toList();
    }

    public PaginatedEntities<SheetMusicEntity> findSheetEntities(
            final PaginationRequest paginationRequest,
            final Map<String, Object> parameters,
            final String titleStartsWith) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final List<String> conditions = new ArrayList<>(
                nonNullParams.keySet().stream().map(o -> o + "=:" + o).toList());
        final Map<String, Object> queryParams = new HashMap<>(nonNullParams);

        if (titleStartsWith != null && !titleStartsWith.isBlank()) {
            conditions.add("lower(title) like :titlePrefix");
            queryParams.put("titlePrefix", titleStartsWith.toLowerCase(Locale.ROOT) + "%");
        }

        final String filter = String.join(" and ", conditions);
        final Sort sort = prepareSort(paginationRequest);

        long totalItems;
        PanacheQuery<SheetMusicEntity> sheetQuery;
        if (queryParams.isEmpty()) {
            sheetQuery = findAll(sort);
            totalItems = count();
        } else {
            sheetQuery = find(filter, sort, queryParams);
            totalItems = count(filter, queryParams);
        }
        final List<SheetMusicEntity> sheets = sheetQuery
                .page(paginationRequest.getPage(), paginationRequest.getSize())
                .list();

        return new PaginatedEntities<>(sheets, totalItems);
    }

    private Sort prepareSort(PaginationRequest paginationRequest) {
        if (paginationRequest.getSortBy() != null) {
            SortFieldValidator.validate(paginationRequest.getSortBy(), ALLOWED_SORT_FIELDS);
            if (SortOrder.DESC == paginationRequest.getSortOrder()) {
                return Sort.descending(paginationRequest.getSortBy());
            } else {
                return Sort.ascending(paginationRequest.getSortBy());
            }
        }
        return Sort.ascending("title");
    }

    /**
     * Returns sheets that contain at least one instrumentation whose instrument matches any of the
     * given IDs. Results are ordered alphabetically by title. Uses a two-step ID-then-fetch
     * approach to avoid Hibernate's HHH90003004 warning about DISTINCT + JOIN FETCH pagination.
     */
    public PaginatedEntities<SheetMusicEntity> findSheetsForInstruments(
            List<String> instrumentIds, int page, int size) {
        if (instrumentIds.isEmpty()) {
            return new PaginatedEntities<>(List.of(), 0);
        }

        String existsClause =
                "EXISTS (SELECT 1 FROM InstrumentationEntity i WHERE i.sheet = s AND i.instrument.id IN :ids)";

        long count = ((Number) getEntityManager()
                        .createQuery("SELECT COUNT(s) FROM SheetMusicEntity s WHERE " + existsClause)
                        .setParameter("ids", instrumentIds)
                        .getSingleResult())
                .longValue();

        List<UUID> sheetIds = getEntityManager()
                .createQuery(
                        "SELECT s.id FROM SheetMusicEntity s WHERE " + existsClause + " ORDER BY s.title ASC",
                        UUID.class)
                .setParameter("ids", instrumentIds)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        if (sheetIds.isEmpty()) {
            return new PaginatedEntities<>(List.of(), count);
        }

        Map<UUID, SheetMusicEntity> byId = find("id IN :ids", Map.of("ids", sheetIds)).stream()
                .collect(Collectors.toMap(SheetMusicEntity::getId, e -> e));
        List<SheetMusicEntity> ordered =
                sheetIds.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PaginatedEntities<>(ordered, count);
    }

    public void removeAttachment(AttachmentEntity attachment) {
        find("SELECT s FROM SheetMusicEntity s JOIN s.attachments a WHERE a.id = :id", Map.of("id", attachment.getId()))
                .list()
                .forEach(s -> s.getAttachments().remove(attachment));
    }
}
