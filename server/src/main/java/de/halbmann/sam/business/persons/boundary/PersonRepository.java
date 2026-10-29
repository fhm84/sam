package de.halbmann.sam.business.persons.boundary;

import de.halbmann.sam.api.entity.persons.PersonMatch;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.shared.SortOrder;
import de.halbmann.sam.business.persons.entity.PersonEntity;
import de.halbmann.sam.core.controller.LikePatterns;
import de.halbmann.sam.core.controller.SortFieldValidator;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class PersonRepository implements PanacheRepositoryBase<PersonEntity, UUID> {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "birthYear", "deathYear", "ipi");

    /**
     * Returns up to {@code limit} persons whose name is similar to {@code name},
     * ordered by trigram similarity (best match first). Requires the {@code pg_trgm} extension.
     */
    @SuppressWarnings("unchecked")
    public List<PersonMatch> findCandidates(String name, double threshold, int limit) {
        List<Object[]> rows = getEntityManager()
                .createNativeQuery("SELECT id, name, similarity(lower(name), lower(:name)) AS score"
                        + " FROM persons"
                        + " WHERE similarity(lower(name), lower(:name)) >= :threshold"
                        + " ORDER BY score DESC"
                        + " LIMIT :limit")
                .setParameter("name", name)
                .setParameter("threshold", threshold)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(r -> new PersonMatch((UUID) r[0], (String) r[1], ((Number) r[2]).doubleValue()))
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
    public PaginatedEntities<PersonEntity> searchByName(String name, int page, int size) {
        // NOTE: the WHERE clause is a plain String (not a text block) to avoid Java text-block
        // trailing-whitespace stripping which would produce invalid SQL like "WHERElower(...)".
        // :namePattern is :name with LIKE wildcards escaped, so "%" or "_" in the input match literally;
        // the similarity and phonetic signals keep using the raw :name.
        String whereClause = nameMatches("name", "name_phonetic");

        long total = ((Number) getEntityManager()
                        .createNativeQuery("SELECT COUNT(*) FROM persons WHERE " + whereClause)
                        .setParameter("name", name)
                        .setParameter("namePattern", LikePatterns.escape(name))
                        .getSingleResult())
                .longValue();

        // Score: prefix match → 1.0, substring → 0.85, word_similarity, full similarity, phonetic
        String scoreSql = "SELECT id,"
                + " GREATEST("
                + "   CASE WHEN lower(name) LIKE lower(:namePattern) || '%'" + LikePatterns.ESCAPE_CLAUSE
                + " THEN 1.0 ELSE 0.0 END,"
                + "   CASE WHEN lower(name) LIKE '%' || lower(:namePattern) || '%'" + LikePatterns.ESCAPE_CLAUSE
                + " THEN 0.85 ELSE 0.0 END,"
                + "   word_similarity(lower(:name), lower(name)),"
                + "   similarity(lower(:name), lower(name)),"
                + "   CASE WHEN dmetaphone(lower(:name)) != ''"
                + "         AND name_phonetic = dmetaphone(lower(:name))"
                + "        THEN 0.6 ELSE 0.0 END"
                + " ) AS score"
                + " FROM persons"
                + " WHERE " + whereClause
                + " ORDER BY score DESC, name ASC"
                + " LIMIT :limit OFFSET :offset";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = getEntityManager()
                .createNativeQuery(scoreSql)
                .setParameter("name", name)
                .setParameter("namePattern", LikePatterns.escape(name))
                .setParameter("limit", size)
                .setParameter("offset", page * size)
                .getResultList();

        List<UUID> ids = rows.stream().map(r -> (UUID) r[0]).toList();
        if (ids.isEmpty()) {
            return new PaginatedEntities<>(List.of(), total);
        }

        Map<UUID, PersonEntity> byId =
                find("id IN :ids", Map.of("ids", ids)).stream().collect(Collectors.toMap(PersonEntity::getId, e -> e));
        List<PersonEntity> ordered =
                ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PaginatedEntities<>(ordered, total);
    }

    /**
     * SQL condition matching a name column against the parameters {@code :name} (raw input) and
     * {@code :namePattern} (the input escaped with {@link LikePatterns#escape}): substring,
     * trigram word similarity or the same phonetic code. Shared with the member search, which
     * matches on the person's name.
     */
    public static String nameMatches(final String nameColumn, final String phoneticColumn) {
        return "(lower(" + nameColumn + ") LIKE '%' || lower(:namePattern) || '%'" + LikePatterns.ESCAPE_CLAUSE
                + " OR word_similarity(lower(:name), lower(" + nameColumn + ")) >= 0.2"
                + " OR (dmetaphone(lower(:name)) != '' AND " + phoneticColumn + " = dmetaphone(lower(:name))))";
    }

    /**
     * Exact (trimmed) name lookup. {@code name} isn't unique, so when duplicates exist the oldest
     * record wins — deterministically, instead of failing with a non-unique-result error.
     */
    public Optional<PersonEntity> findByName(final String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return find("name = :name order by created, id", Map.of("name", name.trim()))
                .firstResultOptional();
    }

    public PaginatedEntities<PersonEntity> findPage(final PaginationRequest paginationRequest) {
        final List<PersonEntity> persons = findAll(prepareSort(paginationRequest))
                .page(paginationRequest.getPage(), paginationRequest.getSize())
                .list();
        return new PaginatedEntities<>(persons, count());
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
