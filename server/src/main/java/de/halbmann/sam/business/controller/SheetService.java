package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.boundary.MusicianRepository;
import de.halbmann.sam.business.boundary.SheetRepository;
import de.halbmann.sam.business.entity.MusicianEntity;
import de.halbmann.sam.business.entity.PaginatedEntities;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class SheetService {

    @Inject
    SheetRepository sheetRepository;

    @Inject
    MusicianRepository musicianRepository;

    @Inject
    SheetMusicMapper sheetMusicMapper;

    @Inject
    CoverageSnapshotService coverageSnapshotService;

    @Inject
    DocumentsService documentsService;

    public PaginatedResponse<SheetMusicSearchResult> findSheets(final SheetFilterRequest filterRequest) {
        PaginatedResponse<SheetMusicSearchResult> response;

        if (filterRequest.getQuery() != null) {
            List<Object[]> results = sheetRepository.searchSheets(
                    filterRequest.getQuery(), filterRequest.getPage(), filterRequest.getSize());

            response = new PaginatedResponse<>();
            response.setData(results.stream()
                    .map(r -> new SheetMusicSearchResult(
                            sheetMusicMapper.toDto((SheetMusicEntity) r[0]),
                            new SearchResultMetrics(
                                    Optional.ofNullable((Number) r[1])
                                            .map(Number::doubleValue)
                                            .orElse(0.0),
                                    Optional.ofNullable((Number) r[2])
                                            .map(Number::doubleValue)
                                            .orElse(0.0),
                                    Optional.ofNullable((Number) r[3])
                                            .map(Number::doubleValue)
                                            .orElse(0.0),
                                    (Boolean) r[4],
                                    Optional.ofNullable((Number) r[5])
                                            .map(Number::doubleValue)
                                            .orElse(0.0))))
                    .toList());
            response.setPage(filterRequest.getPage());
            response.setSize(response.getData().size());
        } else {
            final Map<String, Object> parameters = new HashMap<>();
            if (filterRequest.getTitle() != null) {
                parameters.put("title", filterRequest.getTitle());
            }
            if (filterRequest.getComposer() != null) {
                parameters.put("composer.name", filterRequest.getComposer());
            }
            if (filterRequest.getGenre() != null) {
                parameters.put("genre", filterRequest.getGenre());
            }
            if (filterRequest.getFavorite() != null) {
                parameters.put("favorite", filterRequest.getFavorite());
            }

            PaginatedResponse<SheetMusic> sheets =
                    getAllSheets(filterRequest, parameters, filterRequest.getTitleStartsWith());
            response = new PaginatedResponse<>();
            response.setPage(filterRequest.getPage());
            response.setSize(sheets.getSize());
            response.setTotalCount(sheets.getTotalCount());
            response.setData(
                    sheets.getData().stream().map(SheetMusicSearchResult::new).toList());
        }

        if (filterRequest.getEnsemble() != null) {
            attachCoverage(response, filterRequest.getEnsemble());
        }

        return response;
    }

    private void attachCoverage(PaginatedResponse<SheetMusicSearchResult> response, String ensembleId) {
        List<UUID> sheetIds = response.getData().stream().map(SheetMusic::getId).toList();
        Map<UUID, CoverageSnapshotSummary> coverageMap =
                coverageSnapshotService.findSummaries(UUID.fromString(ensembleId), sheetIds);
        response.getData().forEach(r -> r.setCoverage(coverageMap.get(r.getId())));
    }

    private PaginatedResponse<SheetMusic> getAllSheets(
            final PaginationRequest paginationRequest,
            final Map<String, Object> parameters,
            final String titleStartsWith) {
        PaginatedEntities<SheetMusicEntity> result =
                sheetRepository.findSheetEntities(paginationRequest, parameters, titleStartsWith);

        PaginatedResponse<SheetMusic> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(sheetMusicMapper::toDto).toList());
        response.setPage(paginationRequest.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public SheetMusic getSheet(final String sheetId) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        return sheetMusicMapper.toDto(entity);
    }

    public SheetMusic addSheet(final CreateSheetMusic sheetMusic) {
        final SheetMusicEntity entity = sheetMusicMapper.fromDto(sheetMusic);
        entity.setComposer(resolveOrCreateMusician(sheetMusic.getComposer()));
        entity.setArranger(resolveOrCreateMusician(sheetMusic.getArranger()));
        sheetRepository.persistAndFlush(entity);
        return sheetMusicMapper.toDto(entity);
    }

    public void updateSheet(final String sheetId, final SheetMusic sheetMusic) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        sheetMusicMapper.update(entity, sheetMusic);
        entity.setComposer(resolveOrCreateMusician(sheetMusic.getComposer()));
        entity.setArranger(resolveOrCreateMusician(sheetMusic.getArranger()));
    }

    private MusicianEntity resolveOrCreateMusician(final Musician dto) {
        if (dto == null) {
            return null;
        }
        if (dto.getId() != null) {
            MusicianEntity found = musicianRepository.findByIdOptional(dto.getId()).orElse(null);
            if (found != null) {
                return found;
            }
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            return musicianRepository.findMusicianByName(dto.getName()).orElseGet(() -> {
                MusicianEntity m = new MusicianEntity();
                m.setName(dto.getName().trim());
                musicianRepository.persist(m);
                return m;
            });
        }
        return null;
    }

    public List<String> getDistinctGenres() {
        return sheetRepository.listDistinctGenres();
    }

    public List<String> getAvailableLetters(String genre) {
        return sheetRepository.listAvailableFirstLetters(genre);
    }

    public void deleteSheet(final String sheetId) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        // Unlink sheet-level attachments
        if (entity.getAttachments() != null) {
            documentsService.unlinkAttachments(new ArrayList<>(entity.getAttachments()));
        }
        // Unlink instrumentation-level attachments (instrumentations are cascade-deleted next)
        for (final var instr : entity.getInstrumentations()) {
            if (instr.getAttachments() != null) {
                documentsService.unlinkAttachments(new ArrayList<>(instr.getAttachments()));
            }
        }
        sheetRepository.delete(entity);
    }

    public void addTags(String sheetId, Set<String> newTags) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        entity.getTags().addAll(newTags);
        sheetRepository.persistAndFlush(entity);
    }

    public void removeTags(String sheetId, Set<String> tagsToRemove) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        entity.getTags().removeAll(tagsToRemove);
        sheetRepository.persistAndFlush(entity);
    }

    public void favorite(String sheetId, boolean favorite) {
        final SheetMusicEntity entity = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        entity.setFavorite(favorite);
        sheetRepository.persistAndFlush(entity);
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
