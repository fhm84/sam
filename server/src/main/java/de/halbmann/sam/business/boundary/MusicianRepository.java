package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.controller.MusicianMapper;
import de.halbmann.sam.business.entity.MusicianEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class MusicianRepository implements PanacheRepositoryBase<MusicianEntity, UUID> {

    @Inject
    MusicianMapper musicianMapper;

    public PaginatedResponse<Musician> getAllMusicians(final PaginationRequest paginationRequest) {
        return findMusicians(paginationRequest, Map.of());
    }

    public Optional<MusicianEntity> findMusicianByName(final String name) {
        try {
            return Optional.ofNullable(find("name = :name", Parameters.with("name", name)).singleResult());
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

    public PaginatedResponse<Musician> findMusicians(final MusicianFilterRequest filterRequest) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", filterRequest.getName());

        return findMusicians(filterRequest, parameters);
    }

    PaginatedResponse<Musician> findMusicians(final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final String filter = nonNullParams.keySet().stream()
                .map(o -> o + "=:" + o)
                .collect(Collectors.joining(" and "));

        final Sort sort = prepareSort(paginationRequest);

        // get the total count
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

        // Wrap the result into a PaginatedResponse
        PaginatedResponse<Musician> response = new PaginatedResponse<>();
        response.setData(musicians.stream().map(musicianMapper::toDto).toList());
        response.setSize(response.getData().size());
        response.setTotalCount(totalItems);

        return response;
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
            sort = Sort.ascending("name");
        }
        return sort;
    }

}
