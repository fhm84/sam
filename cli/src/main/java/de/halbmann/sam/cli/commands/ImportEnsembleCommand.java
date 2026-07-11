package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.AbstractImporter;
import de.halbmann.sam.cli.controller.EnsembleImporter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "importEnsemble",
        description = "Import ensemble(s) with voices and options from JSON file(s)",
        mixinStandardHelpOptions = true)
public class ImportEnsembleCommand extends AbstractImportCommand {

    @Inject
    EnsembleImporter ensembleImporter;

    @Override
    protected AbstractImporter<?> importer() {
        return ensembleImporter;
    }

    @Override
    protected String noun() {
        return "ensembles";
    }
}
