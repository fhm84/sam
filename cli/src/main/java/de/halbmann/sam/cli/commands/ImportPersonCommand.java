package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.AbstractImporter;
import de.halbmann.sam.cli.controller.PersonImporter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "importPerson",
        description = "Import person(s) from JSON file(s)",
        mixinStandardHelpOptions = true)
public class ImportPersonCommand extends AbstractImportCommand {

    @Inject
    PersonImporter personImporter;

    @Override
    protected AbstractImporter<?> importer() {
        return personImporter;
    }

    @Override
    protected String noun() {
        return "persons";
    }
}
