package de.halbmann.sam.business.musicians.boundary;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianMatch;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.shared.SortOrder;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class MusicianRepository implements PanacheRepositoryBase<MusicianEntity, UUID> {

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
            if (SortOrder.DESC == paginationRequest.getSortOrder()) {
                return Sort.descending(paginationRequest.getSortBy());
            } else {
                return Sort.ascending(paginationRequest.getSortBy());
            }
        }
        return Sort.ascending("name");
    }
}
