package de.halbmann.sam.cli.commands;

import io.quarkus.arc.Unremovable;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Singleton;
import picocli.CommandLine;

@Unremovable
@Singleton
@TopCommand
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "CLI tool for managing music sheets",
        subcommands = {
            ListSheetsCommand.class,
            ShowSheetCommand.class,
            ImportSheetCommand.class,
            ImportInstrumentCommand.class,
            ExportSheetsCommand.class
        })
public class SamCliCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Use --help to see available commands");
    }
}
