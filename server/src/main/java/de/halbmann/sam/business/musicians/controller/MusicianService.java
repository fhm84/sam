package de.halbmann.sam.business.musicians.controller;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianFilterRequest;
import de.halbmann.sam.api.entity.musicians.MusicianInstrument;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.instruments.boundary.InstrumentRepository;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.musicians.boundary.MusicianRepository;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.business.musicians.entity.MusicianInstrumentEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class MusicianService {

    @Inject
    MusicianRepository musicianRepository;

    @Inject
    InstrumentRepository instrumentRepository;

    @Inject
    MusicianMapper musicianMapper;

    public PaginatedResponse<Musician> findMusicians(final MusicianFilterRequest filterRequest) {
        if (filterRequest.getName() != null && !filterRequest.getName().isBlank()) {
            PaginatedEntities<MusicianEntity> result = musicianRepository.searchByName(
                    filterRequest.getName(), filterRequest.getPage(), filterRequest.getSize());
            PaginatedResponse<Musician> response = new PaginatedResponse<>();
            response.setData(result.data().stream().map(musicianMapper::toDto).toList());
            response.setPage(filterRequest.getPage());
            response.setSize(response.getData().size());
            response.setTotalCount(result.totalCount());
            return response;
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
        applyInstruments(entity, musician.getInstruments());
        return musicianMapper.toDto(entity);
    }

    public void updateMusician(final String musicianId, final Musician musician) {
        final MusicianEntity entity = musicianRepository
                .findByIdOptional(UUID.fromString(musicianId))
                .orElseThrow(() -> new EntityNotFoundException("Musician", musicianId));
        musicianMapper.update(entity, musician);
        applyInstruments(entity, musician.getInstruments());
    }

    public void linkUser(final String musicianId, final String userId) {
        final MusicianEntity entity = musicianRepository
                .findByIdOptional(UUID.fromString(musicianId))
                .orElseThrow(() -> new EntityNotFoundException("Musician", musicianId));
        entity.setUserId(userId);
    }

    public void unlinkUser(final String musicianId) {
        final MusicianEntity entity = musicianRepository
                .findByIdOptional(UUID.fromString(musicianId))
                .orElseThrow(() -> new EntityNotFoundException("Musician", musicianId));
        entity.setUserId(null);
    }

    public void deleteMusician(final String musicianId) {
        final MusicianEntity entity = musicianRepository
                .findByIdOptional(UUID.fromString(musicianId))
                .orElseThrow(() -> new EntityNotFoundException("Musician", musicianId));
        // TODO: check for links -> if the musician is still in use -> do NOT delete!
        musicianRepository.delete(entity);
    }

    private void applyInstruments(final MusicianEntity entity, final List<MusicianInstrument> instruments) {
        entity.getInstruments().clear();
        if (instruments == null || instruments.isEmpty()) {
            return;
        }
        for (MusicianInstrument mi : instruments) {
            final InstrumentEntity instrumentEntity = instrumentRepository
                    .findByIdOptional(mi.getInstrumentId())
                    .orElseThrow(() -> new EntityNotFoundException("Instrument", mi.getInstrumentId()));
            final MusicianInstrumentEntity mie = new MusicianInstrumentEntity();
            mie.setMusician(entity);
            mie.setInstrument(instrumentEntity);
            mie.setPrimary(mi.isPrimary());
            entity.getInstruments().add(mie);
        }
    }
}
