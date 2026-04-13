package de.halbmann.sam.business;

import de.halbmann.sam.api.entity.instruments.Clef;
import de.halbmann.sam.api.entity.instruments.InstrumentTransposing;
import de.halbmann.sam.api.entity.instruments.VoiceOptionType;
import de.halbmann.sam.business.ensembles.entity.EnsembleEntity;
import de.halbmann.sam.business.ensembles.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.business.sheets.entity.VoiceOptionEntity;
import java.util.UUID;

public class BusinessObjectsMother {

    public static SheetMusicEntity sheetWith(InstrumentationEntity... instrumentations) {
        SheetMusicEntity sheet = new SheetMusicEntity();
        if (instrumentations != null) {
            for (InstrumentationEntity instrumentation : instrumentations) {
                sheet.getInstrumentations().add(instrumentation);
            }
        }
        return sheet;
    }

    /**
     * Parsed as follows: {part-label}_{name}_{transposition}
     *
     * @param name
     * @return
     */
    public static InstrumentationEntity instrumentation(String name) {
        ParsedResult parsedResult = InstrumentParser.parseInstrumentation(name);
        InstrumentationEntity instrumentation = new InstrumentationEntity();
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(parsedResult.name() + "_" + parsedResult.transposition());
        instrument.setName(parsedResult.name());
        instrument.setTransposition(InstrumentTransposing.fromString(parsedResult.transposition()));
        instrument.setDisplayName(name);
        instrumentation.setInstrument(instrument);
        instrumentation.setClef(Clef.valueOf(parsedResult.clef()));
        instrumentation.setPartLabel(parsedResult.part());
        return instrumentation;
    }

    public static EnsembleEntity ensembleWith(EnsembleVoiceEntity ensembleVoice) {
        EnsembleEntity ensemble = new EnsembleEntity();
        ensemble.getVoices().add(ensembleVoice);
        return ensemble;
    }

    public static EnsembleVoiceEntity voice(
            String label, boolean required, int minCount, int targetCount, double weight) {
        EnsembleVoiceEntity entity = new EnsembleVoiceEntity();
        entity.setId(UUID.randomUUID());
        entity.setLabel(label);
        entity.setRequired(required);
        entity.setMinCount(minCount);
        entity.setTargetCount(targetCount);
        entity.setWeight(weight);
        entity.getOptions().add(option(label, 1.0));
        return entity;
    }

    public static EnsembleVoiceEntity voiceWithOption(
            String label, int minCount, int targetCount, VoiceOptionEntity... options) {
        final EnsembleVoiceEntity voice = new EnsembleVoiceEntity();
        voice.setId(UUID.randomUUID());
        voice.setLabel(label);
        voice.setMinCount(minCount);
        voice.setTargetCount(targetCount);
        if (options != null) {
            for (VoiceOptionEntity option : options) {
                voice.getOptions().add(option);
            }
        }
        return voice;
    }

    public static VoiceOptionEntity option(String instrumentName, double factor) {
        final VoiceOptionEntity option = new VoiceOptionEntity();
        ParsedResult parsedResult = InstrumentParser.parseVoice(instrumentName);
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(parsedResult.name() + "_" + parsedResult.transposition());
        instrument.setName(parsedResult.name());
        instrument.setTransposition(InstrumentTransposing.fromString(parsedResult.transposition()));
        instrument.setDisplayName(instrumentName);
        option.setInstrument(instrument);
        option.setType(VoiceOptionType.PRIMARY);
        option.setFactor(factor);
        return option;
    }
}
