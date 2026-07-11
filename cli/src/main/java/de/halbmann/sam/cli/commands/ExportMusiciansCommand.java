package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianFilterRequest;
import de.halbmann.sam.cli.util.FilenameUtils;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/**
 * Exports all musicians as JSON files readable by {@code importMusician}. Environment-specific
 * links (OIDC user ID, ensemble membership) are stripped — they reference the source system's
 * Keycloak users and ensembles and would be wrong anywhere else.
 */
@Unremovable
@Singleton
@CommandLine.Command(
        name = "exportMusician",
        description = "Export all musicians to JSON file(s), one per musician",
        mixinStandardHelpOptions = true)
public class ExportMusiciansCommand extends AbstractExportCommand<Musician> {

    @Inject
    @RestClient
    SamResources client;

    @Override
    protected List<Musician> fetchAll() {
        MusicianFilterRequest request = new MusicianFilterRequest();
        request.setSize(-1);
        return client.musicians().findMusicians(request).getData();
    }

    @Override
    protected String filenameFor(final Musician musician, final Set<String> usedNames) {
        return FilenameUtils.uniqueFilename(musician.getName(), musician.getId(), usedNames);
    }

    @Override
    protected String describe(final Musician musician) {
        return musician.getName();
    }

    @Override
    protected String noun() {
        return "musician(s)";
    }

    @Override
    protected Object toExport(final Musician musician) {
        musician.setUserId(null);
        musician.setMembership(null);
        return musician;
    }
}
