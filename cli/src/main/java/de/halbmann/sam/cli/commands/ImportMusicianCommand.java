package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.AbstractImporter;
import de.halbmann.sam.cli.controller.MusicianImporter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "importMusician",
        description = "Import musician(s) from JSON file(s)",
        mixinStandardHelpOptions = true)
public class ImportMusicianCommand extends AbstractImportCommand {

    @Inject
    MusicianImporter musicianImporter;

    @Override
    protected AbstractImporter<?> importer() {
        return musicianImporter;
    }

    @Override
    protected String noun() {
        return "musicians";
    }
}
