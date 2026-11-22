package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.AbstractImporter;
import de.halbmann.sam.cli.controller.SheetImporter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "import",
        description = "Import music sheet(s) from JSON file(s)",
        mixinStandardHelpOptions = true)
public class ImportSheetCommand extends AbstractImportCommand {

    @Inject
    SheetImporter sheetImporter;

    @Override
    protected AbstractImporter<?, ?> importer() {
        return sheetImporter;
    }

    @Override
    protected String noun() {
        return "sheets";
    }
}
