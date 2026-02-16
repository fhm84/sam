package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.boundary.MusicianRepository;
import de.halbmann.sam.business.entity.MusicianEntity;
import de.halbmann.sam.business.entity.PaginatedEntities;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class MusicianService {

    @Inject
    MusicianRepository musicianRepository;

    @Inject
    MusicianMapper musicianMapper;

    public PaginatedResponse<Musician> findMusicians(final MusicianFilterRequest filterRequest) {
        if (filterRequest.getName() != null && !filterRequest.getName().isEmpty()) {
            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", filterRequest.getName());
            return findMusicians(filterRequest, parameters);
        } else {
            return findMusicians(filterRequest, Map.of());
        }
    }

    private PaginatedResponse<Musician> findMusicians(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        PaginatedEntities<MusicianEntity> result =
                musicianRepository.findMusicianEntities(paginationRequest, parameters);

        PaginatedResponse<Musician> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(musicianMapper::toDto).toList());
        response.setPage(paginationRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public Musician getMusician(final String musicianId) {
        MusicianEntity entity = musicianRepository
                .findByIdOptional(UUID.fromString(musicianId))
                .orElseThrow(() -> new EntityNotFoundException("Musician", musicianId));
        return musicianMapper.toDto(entity);
    }

    public Musician addMusician(final Musician musician) {
        // FIXME: implement!
        // we don't want to duplicate musicians, so first search for already existing one ...
        MusicianEntity entity = musicianMapper.fromDto(musician);
        musicianRepository.persistAndFlush(entity);
        return musicianMapper.toDto(entity);
    }

    public void updateMusician(final String musicianId, final Musician musician) {
        final MusicianEntity entity = musicianRepository
                .findByIdOptional(UUID.fromString(musicianId))
                .orElseThrow(() -> new EntityNotFoundException("Musician", musicianId));
        musicianMapper.update(entity, musician);
    }

    public void deleteMusician(final String musicianId) {
        final MusicianEntity entity = musicianRepository
                .findByIdOptional(UUID.fromString(musicianId))
                .orElseThrow(() -> new EntityNotFoundException("Musician", musicianId));
        // TODO: check for links -> if the musician is still in use -> do NOT delete!
        musicianRepository.delete(entity);
    }
}
