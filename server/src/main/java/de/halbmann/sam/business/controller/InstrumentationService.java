package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CreateInstrumentation;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.business.boundary.InstrumentRepository;
import de.halbmann.sam.business.boundary.InstrumentationRepository;
import de.halbmann.sam.business.boundary.SheetRepository;
import de.halbmann.sam.business.entity.InstrumentEntity;
import de.halbmann.sam.business.entity.InstrumentationEntity;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
        final InstrumentationEntity entity = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        return instrumentationMapper.toDto(entity);
    }

    public Instrumentation addInstrumentation(final String sheetId, final CreateInstrumentation instrumentation) {
        final SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        final InstrumentEntity instrument = instrumentRepository.findById(instrumentation.getInstrumentId());
        final InstrumentationEntity entity = instrumentationMapper.fromDto(instrumentation);
        entity.setSheet(sheet);
        entity.setInstrument(instrument);
        instrumentationRepository.persistAndFlush(entity);
        return instrumentationMapper.toDto(entity);
    }

    public void addInstrumentations(final String sheetId, final List<CreateInstrumentation> instrumentations) {
        final SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        List<InstrumentationEntity> instrumentationEntities = instrumentations.stream()
                .map(dto -> {
                    InstrumentEntity instrument = instrumentRepository.findById(dto.getInstrumentId());
                    InstrumentationEntity entity = instrumentationMapper.fromDto(dto);
                    entity.setSheet(sheet);
                    entity.setInstrument(instrument);
                    return entity;
                })
                .toList();
        sheet.getInstrumentations().addAll(instrumentationEntities);
    }

    public void updateInstrumentation(final String instrumentationId, final Instrumentation instrumentation) {
        final InstrumentationEntity entity = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        instrumentationMapper.update(entity, instrumentation);
    }

    public void deleteInstrumentation(final String instrumentationId) {
        final InstrumentationEntity entity = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        instrumentationRepository.delete(entity);
    }
}
