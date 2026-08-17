package de.halbmann.sam.business.sheets.boundary;

import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.api.entity.ensembles.CoverageStatus;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.shared.SortOrder;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.TagCount;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.controller.SortFieldValidator;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    /** Explore view shelf thresholds. */
    private static final Duration QUICK_FILLER_MAX_DURATION =
            Duration.ofMinutes(3).plusSeconds(30);

    private static final Duration BIG_FINISH_MIN_DURATION = Duration.ofMinutes(5);

    private static final int RECENTLY_ADDED_WINDOW_DAYS = 30;

    private static final int CROWD_PLEASER_WINDOW_MONTHS = 12;

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
            final String titleStartsWith,
            final String tag) {
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

        if (tag != null && !tag.isBlank()) {
            conditions.add(":tag member of tags");
            queryParams.put("tag", tag);
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

    /**
     * Sheets short enough to use as warm-ups, encores, or to fit one more piece into a program.
     */
    public List<SheetMusicEntity> findQuickFillers(int limit) {
        return find("duration is not null and duration < ?1 order by duration", QUICK_FILLER_MAX_DURATION)
                .page(0, limit)
                .list();
    }

    /**
     * Sheets long enough to serve as closers, overtures, or medleys.
     */
    public List<SheetMusicEntity> findBigFinishes(int limit) {
        return find("duration is not null and duration >= ?1 order by duration desc", BIG_FINISH_MIN_DURATION)
                .page(0, limit)
                .list();
    }

    /**
     * Sheets added within the last {@value #RECENTLY_ADDED_WINDOW_DAYS} days.
     */
    public List<SheetMusicEntity> findRecentlyAdded(int limit) {
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusDays(RECENTLY_ADDED_WINDOW_DAYS);
        return find("created >= ?1 order by created desc", since).page(0, limit).list();
    }

    /**
     * Sheets with the most setlist appearances within the last
     * {@value #CROWD_PLEASER_WINDOW_MONTHS} months (rolling). Only collections of type
     * {@link CollectionType#SETLIST} with a date count as a "pull"; folders don't.
     */
    public List<SheetMusicEntity> findCrowdPleasers(int limit) {
        LocalDate since = LocalDate.now(ZoneOffset.UTC).minusMonths(CROWD_PLEASER_WINDOW_MONTHS);
        List<UUID> sheetIds = getEntityManager()
                .createQuery("""
                        SELECT i.sheet.id FROM SheetCollectionEntity c
                        JOIN TREAT(c.items AS SheetCollectionItemEntity) i
                        WHERE c.type = :type AND c.date >= :since
                        GROUP BY i.sheet.id
                        ORDER BY COUNT(c.id) DESC
                        """, UUID.class)
                .setParameter("type", CollectionType.SETLIST)
                .setParameter("since", since)
                .setMaxResults(limit)
                .getResultList();
        return findInOrder(sheetIds);
    }

    /**
     * Sheets that have never appeared in any setlist, longest-neglected (oldest {@code created})
     * first.
     */
    public List<SheetMusicEntity> findHiddenGems(int limit) {
        return getEntityManager()
                .createQuery("""
                        SELECT s FROM SheetMusicEntity s
                        WHERE NOT EXISTS (
                            SELECT 1 FROM SheetCollectionEntity c
                            JOIN TREAT(c.items AS SheetCollectionItemEntity) i
                            WHERE i.sheet = s AND c.type = :type
                        )
                        ORDER BY s.created
                        """, SheetMusicEntity.class)
                .setParameter("type", CollectionType.SETLIST)
                .setMaxResults(limit)
                .getResultList();
    }

    /** Fetches the given sheets, preserving the order of {@code sheetIds}. */
    private List<SheetMusicEntity> findInOrder(List<UUID> sheetIds) {
        if (sheetIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, SheetMusicEntity> byId = find("id IN :ids", Map.of("ids", sheetIds)).stream()
                .collect(Collectors.toMap(SheetMusicEntity::getId, e -> e));
        return sheetIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    /**
     * The most frequently used tags across all sheets, for the Explore view's tag cloud.
     */
    @SuppressWarnings("unchecked")
    public List<TagCount> findTopTags(int limit) {
        List<Object[]> rows = getEntityManager()
                .createNativeQuery(
                        "select tag, count(*) as cnt from sheet_music_tags group by tag order by cnt desc limit :limit")
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(row -> new TagCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    /**
     * A single random sheet, for the Explore view's "surprise pick".
     */
    public Optional<SheetMusicEntity> findRandomSheet() {
        List<UUID> ids = getEntityManager()
                .createNativeQuery("select id from sheets order by random() limit 1", UUID.class)
                .getResultList();
        return ids.isEmpty() ? Optional.empty() : findByIdOptional(ids.get(0));
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

    /**
     * Sheets matching the given optional filters, for the AI setlist assistant's candidate
     * search. All filters are ANDed; a {@code null}/empty filter is skipped. Only sheets with a
     * {@code COMPLETE} or {@code PLAYABLE} coverage snapshot for the given ensemble are returned,
     * and the result is capped at {@code limit} rows in SQL — the LLM tool may probe several
     * times per request, so this must never hydrate the whole sheets table.
     */
    public List<SheetMusicEntity> findAiAssistantCandidates(
            UUID ensembleId, Genre genre, Duration maxDuration, Set<String> tags, int limit) {
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT s FROM CoverageSnapshotEntity c JOIN c.sheet s");
        Map<String, Object> params = new HashMap<>();
        if (tags != null && !tags.isEmpty()) {
            jpql.append(" JOIN s.tags t");
        }
        jpql.append(" WHERE c.ensemble.id = :ensembleId AND c.status IN :playableStatuses");
        params.put("ensembleId", ensembleId);
        params.put("playableStatuses", List.of(CoverageStatus.COMPLETE, CoverageStatus.PLAYABLE));
        if (tags != null && !tags.isEmpty()) {
            jpql.append(" AND t IN :tags");
            params.put("tags", tags);
        }
        if (genre != null) {
            jpql.append(" AND s.genre = :genre");
            params.put("genre", genre);
        }
        if (maxDuration != null) {
            jpql.append(" AND s.duration IS NOT NULL AND s.duration <= :maxDuration");
            params.put("maxDuration", maxDuration);
        }
        jpql.append(" ORDER BY s.title");
        var query = getEntityManager().createQuery(jpql.toString(), SheetMusicEntity.class);
        params.forEach(query::setParameter);
        return query.setMaxResults(limit).getResultList();
    }

    public void removeAttachment(AttachmentEntity attachment) {
        find("SELECT s FROM SheetMusicEntity s JOIN s.attachments a WHERE a.id = :id", Map.of("id", attachment.getId()))
                .list()
                .forEach(s -> s.getAttachments().remove(attachment));
    }
}
