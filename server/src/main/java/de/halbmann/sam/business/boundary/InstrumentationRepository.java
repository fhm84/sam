package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.CreateInstrumentation;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.business.controller.InstrumentationMapper;
import de.halbmann.sam.business.entity.InstrumentationEntity;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class InstrumentationRepository implements PanacheRepositoryBase<InstrumentationEntity, UUID> {

    @Inject
    InstrumentationMapper instrumentationMapper;

    @Inject
    SheetRepository sheetRepository;

    public List<Instrumentation> getInstrumentations(final String sheetId) {
        Sort sort = Sort.ascending("instrumentName", "partLabel", "transposition", "clef");
        return find("sheet.id = :sheet_id", sort, Parameters.with("sheet_id", UUID.fromString(sheetId))).list().stream()
                .map(instrumentationMapper::toDto)
                .toList();
    }

    public Instrumentation getInstrumentation(final String instrumentationId) {
        final InstrumentationEntity instrumentationEntity = findById(UUID.fromString(instrumentationId));
        return instrumentationMapper.toDto(instrumentationEntity);
    }

    public Instrumentation addInstrumentation(final String sheetId, final CreateInstrumentation instrumentation) {
        final SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        final InstrumentationEntity instrumentationEntity = instrumentationMapper.fromDto(instrumentation);
        instrumentationEntity.setSheet(sheet);
        persistAndFlush(instrumentationEntity);
        return instrumentationMapper.toDto(instrumentationEntity);
    }

    public void addInstrumentations(final String sheetId, final List<CreateInstrumentation> instrumentations) {
        final SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        List<InstrumentationEntity> instrumentationEntities = instrumentations.stream()
                .map(instrumentationMapper::fromDto)
                .peek(instrumentationEntity -> instrumentationEntity.setSheet(sheet))
                .toList();
        sheet.getInstrumentations().addAll(instrumentationEntities);
    }

    public void updateInstrumentation(final String instrumentationId, final Instrumentation instrumentation) {
        final InstrumentationEntity instrumentationEntity = findById(UUID.fromString(instrumentationId));
        instrumentationMapper.update(instrumentationEntity, instrumentation);
    }

    public void deleteInstrumentation(final String instrumentationId) {
        final InstrumentationEntity instrumentationEntity = findById(UUID.fromString(instrumentationId));
        delete(instrumentationEntity);
    }
}
