package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.AbstractImporter;
import de.halbmann.sam.cli.controller.InstrumentImporter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "importInstrument",
        description = "Import instrument(s) from JSON file(s)",
        mixinStandardHelpOptions = true)
public class ImportInstrumentCommand extends AbstractImportCommand {

    @Inject
    InstrumentImporter instrumentImporter;

    @Override
    protected AbstractImporter<?> importer() {
        return instrumentImporter;
    }

    @Override
    protected String noun() {
        return "instruments";
    }
}
