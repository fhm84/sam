package de.halbmann.sam.business.sheets.controller;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.ensembles.CoverageSnapshotSummary;
import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.shared.SearchResultMetrics;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.api.entity.sheets.ExploreShelves;
import de.halbmann.sam.api.entity.sheets.SheetFilterRequest;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.api.entity.sheets.SheetMusicSearchResult;
import de.halbmann.sam.api.entity.sheets.TagCount;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.documents.controller.AttachmentLinkService;
import de.halbmann.sam.business.ensembles.controller.CoverageSnapshotService;
import de.halbmann.sam.business.musicians.boundary.MusicianRepository;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.*;

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
    AttachmentLinkService attachmentLinkService;

    @Inject
    SheetCollectionService sheetCollectionService;

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
                    getAllSheets(filterRequest, parameters, filterRequest.getTitleStartsWith(), filterRequest.getTag());
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
        attachCoverage(response.getData(), ensembleId);
    }

    private void attachCoverage(List<SheetMusicSearchResult> results, String ensembleId) {
        if (results.isEmpty()) {
            return;
        }
        List<UUID> sheetIds = results.stream().map(SheetMusic::getId).toList();
        Map<UUID, CoverageSnapshotSummary> coverageMap =
                coverageSnapshotService.findSummaries(UUID.fromString(ensembleId), sheetIds);
        results.forEach(r -> r.setCoverage(coverageMap.get(r.getId())));
    }

    private static final int EXPLORE_SHELF_LIMIT = 12;

    private static final int EXPLORE_TAG_CLOUD_LIMIT = 30;

    /**
     * Curated shelves for the Sheets Overview's Explore view. When {@code ensembleId} is given,
     * coverage snapshots are attached so shelves can show ensemble-relative coverage badges.
     */
    public ExploreShelves getExploreShelves(final String ensembleId) {
        List<SheetMusicSearchResult> quickFillers =
                mapToSearchResults(sheetRepository.findQuickFillers(EXPLORE_SHELF_LIMIT));
        List<SheetMusicSearchResult> bigFinishes =
                mapToSearchResults(sheetRepository.findBigFinishes(EXPLORE_SHELF_LIMIT));
        List<SheetMusicSearchResult> recentlyAdded =
                mapToSearchResults(sheetRepository.findRecentlyAdded(EXPLORE_SHELF_LIMIT));
        List<SheetMusicSearchResult> crowdPleasers =
                mapToSearchResults(sheetRepository.findCrowdPleasers(EXPLORE_SHELF_LIMIT));
        List<SheetMusicSearchResult> hiddenGems =
                mapToSearchResults(sheetRepository.findHiddenGems(EXPLORE_SHELF_LIMIT));
        List<SheetMusicSearchResult> needsAttention = ensembleId == null
                ? List.of()
                : mapToSearchResults(
                        coverageSnapshotService.findIncompleteSheets(UUID.fromString(ensembleId), EXPLORE_SHELF_LIMIT));
        List<TagCount> tagCloud = sheetRepository.findTopTags(EXPLORE_TAG_CLOUD_LIMIT);

        if (ensembleId != null) {
            List<SheetMusicSearchResult> all = new ArrayList<>();
            all.addAll(quickFillers);
            all.addAll(bigFinishes);
            all.addAll(recentlyAdded);
            all.addAll(crowdPleasers);
            all.addAll(hiddenGems);
            all.addAll(needsAttention);
            attachCoverage(all, ensembleId);
        }

        return new ExploreShelves(
                quickFillers, bigFinishes, recentlyAdded, crowdPleasers, hiddenGems, needsAttention, tagCloud);
    }

    /**
     * A single random sheet for the Explore view's "surprise pick", or empty if the archive has
     * no sheets yet.
     */
    public Optional<SheetMusicSearchResult> getSurprisePick(final String ensembleId) {
        Optional<SheetMusicSearchResult> pick =
                sheetRepository.findRandomSheet().map(sheetMusicMapper::toDto).map(SheetMusicSearchResult::new);
        pick.ifPresent(result -> {
            if (ensembleId != null) {
                attachCoverage(List.of(result), ensembleId);
            }
        });
        return pick;
    }

    private List<SheetMusicSearchResult> mapToSearchResults(List<SheetMusicEntity> entities) {
        return entities.stream()
                .map(sheetMusicMapper::toDto)
                .map(SheetMusicSearchResult::new)
                .toList();
    }

    private PaginatedResponse<SheetMusic> getAllSheets(
            final PaginationRequest paginationRequest,
            final Map<String, Object> parameters,
            final String titleStartsWith,
            final String tag) {
        PaginatedEntities<SheetMusicEntity> result =
                sheetRepository.findSheetEntities(paginationRequest, parameters, titleStartsWith, tag);

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
        if (sheetMusic.getCollectionId() != null) {
            final CreateCollectionItem item = new CreateCollectionItem();
            item.setType(CollectionItemType.SHEET);
            item.setSheetId(entity.getId());
            sheetCollectionService.addItem(sheetMusic.getCollectionId().toString(), item);
        }
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
            MusicianEntity found =
                    musicianRepository.findByIdOptional(dto.getId()).orElse(null);
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
            attachmentLinkService.unlinkAttachments(new ArrayList<>(entity.getAttachments()));
        }
        // Unlink instrumentation-level attachments (instrumentations are cascade-deleted next)
        for (final var instr : entity.getInstrumentations()) {
            if (instr.getAttachments() != null) {
                attachmentLinkService.unlinkAttachments(new ArrayList<>(instr.getAttachments()));
            }
        }
        // Drop collection memberships — otherwise the collection_items FK blocks the delete
        sheetCollectionService.removeSheetFromAllCollections(entity.getId());
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
}
