package de.halbmann.sam.business.musicians.boundary;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianMatch;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.shared.SortOrder;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.core.controller.SortFieldValidator;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class MusicianRepository implements PanacheRepositoryBase<MusicianEntity, UUID> {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "birthYear", "deathYear", "ipi");

    /**
     * Returns up to {@code limit} musicians whose name is similar to {@code name},
     * ordered by trigram similarity (best match first). Requires the {@code pg_trgm} extension.
     */
    @SuppressWarnings("unchecked")
    public List<MusicianMatch> findCandidates(String name, double threshold, int limit) {
        List<Object[]> rows = getEntityManager()
                .createNativeQuery("SELECT id, name, similarity(lower(name), lower(:name)) AS score"
                        + " FROM musicians"
                        + " WHERE similarity(lower(name), lower(:name)) >= :threshold"
                        + " ORDER BY score DESC"
                        + " LIMIT :limit")
                .setParameter("name", name)
                .setParameter("threshold", threshold)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(r -> new MusicianMatch((UUID) r[0], (String) r[1], ((Number) r[2]).doubleValue()))
                .collect(Collectors.toList());
    }

    /**
     * Multi-signal name search combining prefix wildcard, trigram word-similarity and phonetic
     * matching. Results are ordered by a weighted score (prefix match &gt; substring &gt;
     * word_similarity &gt; phonetic), so the most relevant hits always appear first.
     *
     * <p>Requires the {@code pg_trgm} and {@code fuzzystrmatch} extensions (created by migration
     * V1.0.1).
     */
    @SuppressWarnings("unchecked")
    public PaginatedEntities<MusicianEntity> searchByName(String name, int page, int size) {
        // NOTE: the WHERE clause is a plain String (not a text block) to avoid Java text-block
        // trailing-whitespace stripping which would produce invalid SQL like "WHERElower(...)".
        String whereClause = "lower(name) LIKE '%' || lower(:name) || '%'"
                + " OR word_similarity(lower(:name), lower(name)) >= 0.2"
                + " OR (dmetaphone(lower(:name)) != '' AND name_phonetic = dmetaphone(lower(:name)))";

        long total = ((Number) getEntityManager()
                        .createNativeQuery("SELECT COUNT(*) FROM musicians WHERE " + whereClause)
                        .setParameter("name", name)
                        .getSingleResult())
                .longValue();

        // Score: prefix match → 1.0, substring → 0.85, word_similarity, full similarity, phonetic
        String scoreSql = "SELECT id,"
                + " GREATEST("
                + "   CASE WHEN lower(name) LIKE lower(:name) || '%' THEN 1.0 ELSE 0.0 END,"
                + "   CASE WHEN lower(name) LIKE '%' || lower(:name) || '%' THEN 0.85 ELSE 0.0 END,"
                + "   word_similarity(lower(:name), lower(name)),"
                + "   similarity(lower(:name), lower(name)),"
                + "   CASE WHEN dmetaphone(lower(:name)) != ''"
                + "         AND name_phonetic = dmetaphone(lower(:name))"
                + "        THEN 0.6 ELSE 0.0 END"
                + " ) AS score"
                + " FROM musicians"
                + " WHERE " + whereClause
                + " ORDER BY score DESC, name ASC"
                + " LIMIT :limit OFFSET :offset";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = getEntityManager()
                .createNativeQuery(scoreSql)
                .setParameter("name", name)
                .setParameter("limit", size)
                .setParameter("offset", page * size)
                .getResultList();

        List<UUID> ids = rows.stream().map(r -> (UUID) r[0]).toList();
        if (ids.isEmpty()) {
            return new PaginatedEntities<>(List.of(), total);
        }

        Map<UUID, MusicianEntity> byId = find("id IN :ids", Map.of("ids", ids)).stream()
                .collect(Collectors.toMap(MusicianEntity::getId, e -> e));
        List<MusicianEntity> ordered =
                ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PaginatedEntities<>(ordered, total);
    }

    public Optional<MusicianEntity> findByUserId(String userId) {
        return find("userId = :userId", Map.of("userId", userId)).firstResultOptional();
    }

    public Optional<MusicianEntity> findMusicianByName(final String name) {
        try {
            return Optional.ofNullable(
                    find("name = :name", Map.of("name", name)).singleResult());
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }

    public MusicianEntity resolveMusician(final Musician dto) {
        if (dto == null || dto.getId() == null) {
            return null;
        }

        try {
            return findById(dto.getId());
        } catch (final NoResultException e) {
            return null;
        }
    }

    public PaginatedEntities<MusicianEntity> findMusicianEntities(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final String filter =
                nonNullParams.keySet().stream().map(o -> o + "=:" + o).collect(Collectors.joining(" and "));

        final Sort sort = prepareSort(paginationRequest);

        long totalItems;
        PanacheQuery<MusicianEntity> musicianQuery;
        if (nonNullParams.isEmpty()) {
            musicianQuery = findAll(sort);
            totalItems = count();
        } else {
            musicianQuery = find(filter, nonNullParams);
            totalItems = count(filter, nonNullParams);
        }
        final List<MusicianEntity> musicians = musicianQuery
                .page(paginationRequest.getPage(), paginationRequest.getSize())
                .list();

        return new PaginatedEntities<>(musicians, totalItems);
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
        return Sort.ascending("name");
    }
}
