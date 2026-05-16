package de.halbmann.sam.business.myparts;

import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.api.entity.sheets.SheetWithMyParts;
import de.halbmann.sam.business.ensembles.boundary.EnsembleMembershipRepository;
import de.halbmann.sam.business.ensembles.entity.EnsembleMembershipEntity;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.musicians.boundary.MusicianRepository;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.controller.InstrumentationMapper;
import de.halbmann.sam.business.sheets.controller.SheetMusicMapper;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
@Transactional
public class MyPartsService {

    @Inject
    MusicianRepository musicianRepository;

    @Inject
    EnsembleMembershipRepository membershipRepository;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    SheetMusicMapper sheetMusicMapper;

    @Inject
    InstrumentationMapper instrumentationMapper;

    public PaginatedResponse<SheetWithMyParts> getMyParts(String userId, PaginationRequest filter) {
        if (userId == null) {
            return emptyResponse(filter);
        }

        Optional<MusicianEntity> musicianOpt = musicianRepository.findByUserId(userId);
        if (musicianOpt.isEmpty()) {
            return emptyResponse(filter);
        }

        MusicianEntity musician = musicianOpt.get();

        List<String> instrumentIds =
                membershipRepository.list("musician = :musician", Map.of("musician", musician)).stream()
                        .map(EnsembleMembershipEntity::getInstrument)
                        .filter(Objects::nonNull)
                        .map(InstrumentEntity::getId)
                        .distinct()
                        .toList();

        if (instrumentIds.isEmpty()) {
            return emptyResponse(filter);
        }

        PaginatedEntities<SheetMusicEntity> result =
                sheetRepository.findSheetsForInstruments(instrumentIds, filter.getPage(), filter.getSize());

        Set<String> instrumentIdSet = new HashSet<>(instrumentIds);
        List<SheetWithMyParts> items = result.data().stream()
                .map(sheet -> {
                    SheetMusic sheetDto = sheetMusicMapper.toDto(sheet);
                    List<Instrumentation> myInstrs = sheet.getInstrumentations().stream()
                            .filter(i -> i.getInstrument() != null
                                    && instrumentIdSet.contains(
                                            i.getInstrument().getId()))
                            .map(instrumentationMapper::toDto)
                            .toList();
                    return new SheetWithMyParts(sheetDto, myInstrs);
                })
                .toList();

        PaginatedResponse<SheetWithMyParts> response = new PaginatedResponse<>();
        response.setData(items);
        response.setPage(filter.getPage());
        response.setSize(items.size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    private PaginatedResponse<SheetWithMyParts> emptyResponse(PaginationRequest filter) {
        PaginatedResponse<SheetWithMyParts> response = new PaginatedResponse<>();
        response.setData(List.of());
        response.setPage(filter.getPage());
        response.setSize(0);
        response.setTotalCount(0);
        return response;
    }
}
