package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.AbstractImporter;
import de.halbmann.sam.cli.controller.MemberImporter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "importMember",
        description = "Import member(s) from JSON file(s)",
        mixinStandardHelpOptions = true)
public class ImportMemberCommand extends AbstractImportCommand {

    @Inject
    MemberImporter memberImporter;

    @Override
    protected AbstractImporter<?> importer() {
        return memberImporter;
    }

    @Override
    protected String noun() {
        return "members";
    }
}
