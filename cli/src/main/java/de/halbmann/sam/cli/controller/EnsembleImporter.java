package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.EnsembleVoicesResource;
import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.ensembles.Ensemble;
import de.halbmann.sam.api.entity.ensembles.EnsembleFilterRequest;
import de.halbmann.sam.api.entity.ensembles.EnsembleVoice;
import de.halbmann.sam.api.entity.instruments.VoiceOption;
import de.halbmann.sam.cli.mapper.DataMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class EnsembleImporter extends AbstractImporter<Ensemble> {

    @Inject
    @RestClient
    SamResources client;

    @Inject
    DataMapper dataMapper;

    @Override
    protected Class<Ensemble> type() {
        return Ensemble.class;
    }

    @Override
    protected String describe(final Ensemble ensemble) {
        int voices = ensemble.getVoices() != null ? ensemble.getVoices().size() : 0;
        return ensemble.getName() + " (" + voices + " voice(s))";
    }

    @Override
    protected List<String> extraValidation(final Ensemble ensemble) {
        // Ensemble.voices carries no @Valid, so check the nested records ourselves
        List<String> problems = new ArrayList<>();
        if (ensemble.getVoices() == null) {
            return problems;
        }
        for (EnsembleVoice voice : ensemble.getVoices()) {
            if (voice.getLabel() == null || voice.getLabel().isBlank()) {
                problems.add("voice without label");
                continue;
            }
            if (voice.getOptions() != null) {
                for (VoiceOption option : voice.getOptions()) {
                    if (option.getInstrumentId() == null
                            || option.getInstrumentId().isBlank()) {
                        problems.add("voice '" + voice.getLabel() + "' has an option without instrumentId");
                    }
                }
            }
        }
        return problems;
    }

    @Override
    protected boolean exists(final Ensemble ensemble) {
        EnsembleFilterRequest filter = new EnsembleFilterRequest();
        filter.setName(ensemble.getName());
        filter.setSize(-1);
        return client.ensembles().findEnsembles(filter).getData().stream()
                .anyMatch(existing -> ensemble.getName().equalsIgnoreCase(existing.getName()));
    }

    @Override
    protected void create(final Ensemble ensemble) {
        Ensemble created = client.ensembles().add(dataMapper.createFromEnsemble(ensemble));
        System.out.println("  ✓ Imported: " + created.getName() + " (ID: " + created.getId() + ")");

        if (ensemble.getVoices() == null) {
            return;
        }
        EnsembleVoicesResource voices =
                client.ensembles().voices(created.getId().toString());
        for (EnsembleVoice voice : ensemble.getVoices()) {
            EnsembleVoice createdVoice = voices.add(dataMapper.createFromVoice(voice));
            System.out.println("      ✓ Voice: " + createdVoice.getLabel());
            if (voice.getOptions() == null) {
                continue;
            }
            for (VoiceOption option : voice.getOptions()) {
                voices.options(createdVoice.getId().toString()).add(dataMapper.createFromOption(option));
                System.out.println("          ✓ Option: " + option.getInstrumentId());
            }
        }
    }
}
