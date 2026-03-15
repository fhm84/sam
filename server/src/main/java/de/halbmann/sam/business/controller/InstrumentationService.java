package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CreateInstrumentation;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.business.boundary.InstrumentRepository;
import de.halbmann.sam.business.boundary.InstrumentationRepository;
import de.halbmann.sam.business.boundary.SheetRepository;
import de.halbmann.sam.business.entity.InstrumentEntity;
import de.halbmann.sam.business.entity.InstrumentationEntity;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class InstrumentationService {

    @Inject
    InstrumentationRepository instrumentationRepository;

    @Inject
    InstrumentationMapper instrumentationMapper;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    InstrumentRepository instrumentRepository;

    @Inject
    DocumentsService documentsService;

    public List<Instrumentation> getInstrumentations(final String sheetId) {
        Sort sort = Sort.ascending("instrument.name", "partLabel");
        return instrumentationRepository
                .find("sheet.id = :sheet_id", sort, Parameters.with("sheet_id", UUID.fromString(sheetId)))
                .list()
                .stream()
                .map(instrumentationMapper::toDto)
                .toList();
    }

    public Instrumentation getInstrumentation(final String instrumentationId) {
        final InstrumentationEntity entity = instrumentationRepository
                .findByIdOptional(UUID.fromString(instrumentationId))
                .orElseThrow(() -> new EntityNotFoundException("Instrumentation", instrumentationId));
        return instrumentationMapper.toDto(entity);
    }

    public Instrumentation addInstrumentation(final String sheetId, final CreateInstrumentation instrumentation) {
        final SheetMusicEntity sheet = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        final InstrumentEntity instrument = instrumentRepository
                .findByIdOptional(instrumentation.getInstrumentId())
                .orElseThrow(() -> new EntityNotFoundException("Instrument", instrumentation.getInstrumentId()));
        final InstrumentationEntity entity = instrumentationMapper.fromDto(instrumentation);
        entity.setSheet(sheet);
        entity.setInstrument(instrument);
        instrumentationRepository.persistAndFlush(entity);
        return instrumentationMapper.toDto(entity);
    }

    public void addInstrumentations(final String sheetId, final List<CreateInstrumentation> instrumentations) {
        final SheetMusicEntity sheet = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));
        List<InstrumentationEntity> instrumentationEntities = instrumentations.stream()
                .map(dto -> {
                    InstrumentEntity instrument = instrumentRepository
                            .findByIdOptional(dto.getInstrumentId())
                            .orElseThrow(() -> new EntityNotFoundException("Instrument", dto.getInstrumentId()));
                    InstrumentationEntity entity = instrumentationMapper.fromDto(dto);
                    entity.setSheet(sheet);
                    entity.setInstrument(instrument);
                    return entity;
                })
                .toList();
        sheet.getInstrumentations().addAll(instrumentationEntities);
    }

    public void updateInstrumentation(final String instrumentationId, final Instrumentation instrumentation) {
        final InstrumentationEntity entity = instrumentationRepository
                .findByIdOptional(UUID.fromString(instrumentationId))
                .orElseThrow(() -> new EntityNotFoundException("Instrumentation", instrumentationId));
        instrumentationMapper.update(entity, instrumentation);
    }

    public void deleteInstrumentation(final String instrumentationId) {
        final InstrumentationEntity entity = instrumentationRepository
                .findByIdOptional(UUID.fromString(instrumentationId))
                .orElseThrow(() -> new EntityNotFoundException("Instrumentation", instrumentationId));
        if (entity.getAttachments() != null) {
            documentsService.unlinkAttachments(new ArrayList<>(entity.getAttachments()));
        }
        instrumentationRepository.delete(entity);
    }
}
