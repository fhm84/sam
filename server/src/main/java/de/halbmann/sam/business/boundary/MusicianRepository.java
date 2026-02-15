package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.Musician;
import de.halbmann.sam.api.entity.PaginationRequest;
import de.halbmann.sam.api.entity.SortOrder;
import de.halbmann.sam.business.entity.MusicianEntity;
import de.halbmann.sam.business.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
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

    public Optional<MusicianEntity> findMusicianByName(final String name) {
        try {
            return Optional.ofNullable(
                    find("name = :name", Parameters.with("name", name)).singleResult());
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
