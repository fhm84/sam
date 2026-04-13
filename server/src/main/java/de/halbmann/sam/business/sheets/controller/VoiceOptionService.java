package de.halbmann.sam.business.sheets.controller;

import de.halbmann.sam.api.entity.instruments.CreateVoiceOption;
import de.halbmann.sam.api.entity.instruments.VoiceOption;
import de.halbmann.sam.business.ensembles.boundary.EnsembleVoiceRepository;
import de.halbmann.sam.business.ensembles.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.instruments.boundary.InstrumentRepository;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.sheets.boundary.VoiceOptionRepository;
import de.halbmann.sam.business.sheets.entity.VoiceOptionEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class VoiceOptionService {

    @Inject
    VoiceOptionRepository voiceOptionRepository;

    @Inject
    VoiceOptionMapper voiceOptionMapper;

    @Inject
    EnsembleVoiceRepository ensembleVoiceRepository;

    @Inject
    InstrumentRepository instrumentRepository;

    public List<VoiceOption> getOptions(final String voiceId) {
        Sort sort = Sort.ascending("instrument.name");
        return voiceOptionRepository
                .find("voice.id = :voiceId", sort, Map.of("voiceId", UUID.fromString(voiceId)))
                .list()
                .stream()
                .map(voiceOptionMapper::toDto)
                .toList();
    }

    public VoiceOption addOption(final String voiceId, final CreateVoiceOption option) {
        final EnsembleVoiceEntity voice = ensembleVoiceRepository
                .findByIdOptional(UUID.fromString(voiceId))
                .orElseThrow(() -> new EntityNotFoundException("EnsembleVoice", voiceId));
        final InstrumentEntity instrument = instrumentRepository
                .findByIdOptional(option.getInstrumentId())
                .orElseThrow(() -> new EntityNotFoundException("Instrument", option.getInstrumentId()));
        final VoiceOptionEntity entity = voiceOptionMapper.fromDto(option);
        entity.setVoice(voice);
        entity.setInstrument(instrument);
        voiceOptionRepository.persistAndFlush(entity);
        return voiceOptionMapper.toDto(entity);
    }

    public void updateOption(final String optionId, final VoiceOption option) {
        final VoiceOptionEntity entity = voiceOptionRepository
                .findByIdOptional(UUID.fromString(optionId))
                .orElseThrow(() -> new EntityNotFoundException("VoiceOption", optionId));
        voiceOptionMapper.update(entity, option);
        if (option.getInstrumentId() != null) {
            final InstrumentEntity instrument = instrumentRepository
                    .findByIdOptional(option.getInstrumentId())
                    .orElseThrow(() -> new EntityNotFoundException("Instrument", option.getInstrumentId()));
            entity.setInstrument(instrument);
        }
    }

    public void deleteOption(final String optionId) {
        final VoiceOptionEntity entity = voiceOptionRepository
                .findByIdOptional(UUID.fromString(optionId))
                .orElseThrow(() -> new EntityNotFoundException("VoiceOption", optionId));
        voiceOptionRepository.delete(entity);
    }
}
