package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.ensembles.Ensemble;
import de.halbmann.sam.api.entity.ensembles.EnsembleFilterRequest;
import de.halbmann.sam.cli.util.FilenameUtils;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/**
 * Exports all ensembles — including voices and their instrument options — as JSON files readable
 * by {@code importEnsemble}. Voice options reference instruments by natural ID, so the files are
 * portable across environments. Memberships are not part of the ensemble DTO and are not exported.
 */
@Unremovable
@Singleton
@CommandLine.Command(
        name = "exportEnsemble",
        description = "Export all ensembles (with voices and options) to JSON file(s), one per ensemble",
        mixinStandardHelpOptions = true)
public class ExportEnsemblesCommand extends AbstractExportCommand<Ensemble> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected List<Ensemble> fetchAll() {
        EnsembleFilterRequest request = new EnsembleFilterRequest();
        request.setSize(-1);
        // the list endpoint may omit voices — load each ensemble individually to include them
        return client.ensembles().findEnsembles(request).getData().stream()
                .map(e -> client.ensembles().load(e.getId().toString()))
                .toList();
    }

    @Override
    protected String filenameFor(final Ensemble ensemble, final Set<String> usedNames) {
        return FilenameUtils.uniqueFilename(ensemble.getName(), ensemble.getId(), usedNames);
    }

    @Override
    protected String describe(final Ensemble ensemble) {
        int voices = ensemble.getVoices() != null ? ensemble.getVoices().size() : 0;
        return ensemble.getName() + " (" + voices + " voice(s))";
    }

    @Override
    protected String noun() {
        return "ensemble(s)";
    }
}
