package de.halbmann.sam.cli.controller;

import de.halbmann.sam.api.boundary.EnsembleVoicesResource;
import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.ensembles.Ensemble;
import de.halbmann.sam.api.entity.ensembles.EnsembleFilterRequest;
import de.halbmann.sam.api.entity.ensembles.EnsembleVoice;
import de.halbmann.sam.api.entity.instruments.VoiceOption;
import de.halbmann.sam.cli.controller.sync.Addition;
import de.halbmann.sam.cli.mapper.DataMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class EnsembleImporter extends AbstractImporter<Ensemble, Ensemble> {

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
    protected Optional<Ensemble> findExisting(final Ensemble ensemble) {
        if (ensemble.getId() != null) {
            Optional<Ensemble> byId =
                    loadIfExists(() -> client.ensembles().load(ensemble.getId().toString()));
            if (byId.isPresent()) {
                return byId;
            }
        }
        EnsembleFilterRequest filter = new EnsembleFilterRequest();
        filter.setName(ensemble.getName());
        filter.setSize(-1);
        return single(
                client.ensembles().findEnsembles(filter).getData().stream()
                        .filter(existing -> ensemble.getName().strip().equalsIgnoreCase(existing.getName()))
                        .toList(),
                "the name " + ensemble.getName());
    }

    @Override
    protected void create(final Ensemble ensemble) {
        Ensemble created = client.ensembles().add(dataMapper.createFromEnsemble(ensemble));
        System.out.println("  ✓ Created: " + created.getName() + " (ID: " + created.getId() + ")");
        if (ensemble.getVoices() == null) {
            return;
        }
        EnsembleVoicesResource voices =
                client.ensembles().voices(created.getId().toString());
        for (EnsembleVoice voice : ensemble.getVoices()) {
            createVoice(voices, voice);
            System.out.println("      ✓ Voice: " + voice.getLabel() + " (" + optionCount(voice) + " option(s))");
        }
    }

    @Override
    protected List<String> comparedFields() {
        return List.of("name", "description");
    }

    @Override
    protected void update(final Ensemble existing) {
        client.ensembles().update(existing.getId().toString(), existing);
    }

    @Override
    protected List<Addition> missingChildren(final Ensemble ensemble, final Ensemble existing) {
        if (ensemble.getVoices() == null || ensemble.getVoices().isEmpty()) {
            return List.of();
        }
        EnsembleVoicesResource voices =
                client.ensembles().voices(existing.getId().toString());
        Map<String, EnsembleVoice> present = voices.listAll().stream()
                .collect(Collectors.toMap(v -> labelKey(v.getLabel()), Function.identity(), (a, b) -> a));
        List<Addition> additions = new ArrayList<>();
        for (EnsembleVoice voice : ensemble.getVoices()) {
            EnsembleVoice match = present.get(labelKey(voice.getLabel()));
            if (match == null) {
                additions.add(new Addition(
                        "voice " + voice.getLabel() + " (" + optionCount(voice) + " option(s))",
                        () -> createVoice(voices, voice)));
                present.put(labelKey(voice.getLabel()), voice); // a repeated label in the file adds once
                continue;
            }
            List<VoiceOption> missing = missingOptions(voice, voices, match);
            if (!missing.isEmpty()) {
                String voiceId = match.getId().toString();
                additions.add(new Addition(
                        "option(s) "
                                + missing.stream()
                                        .map(VoiceOption::getInstrumentId)
                                        .collect(Collectors.joining(", "))
                                + " on voice " + match.getLabel(),
                        () -> missing.forEach(
                                option -> voices.options(voiceId).add(dataMapper.createFromOption(option)))));
            }
        }
        return additions;
    }

    private List<VoiceOption> missingOptions(
            final EnsembleVoice voice, final EnsembleVoicesResource voices, final EnsembleVoice match) {
        if (voice.getOptions() == null || voice.getOptions().isEmpty()) {
            return List.of();
        }
        Set<String> present = voices.options(match.getId().toString()).listAll().stream()
                .map(VoiceOption::getInstrumentId)
                .collect(Collectors.toSet());
        return voice.getOptions().stream()
                .filter(option -> present.add(option.getInstrumentId()))
                .toList();
    }

    private void createVoice(final EnsembleVoicesResource voices, final EnsembleVoice voice) {
        EnsembleVoice created = voices.add(dataMapper.createFromVoice(voice));
        if (voice.getOptions() != null) {
            for (VoiceOption option : voice.getOptions()) {
                voices.options(created.getId().toString()).add(dataMapper.createFromOption(option));
            }
        }
    }

    private static int optionCount(final EnsembleVoice voice) {
        return voice.getOptions() == null ? 0 : voice.getOptions().size();
    }

    private static String labelKey(final String label) {
        return label.strip().toLowerCase(Locale.ROOT);
    }
}
