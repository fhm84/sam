package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.CliErrorReporter;
import io.quarkus.arc.Unremovable;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
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
            ImportMusicianCommand.class,
            ImportEnsembleCommand.class,
            ExportSheetsCommand.class,
            ExportInstrumentsCommand.class,
            ExportMusiciansCommand.class,
            ExportEnsemblesCommand.class
        })
public class SamCliCommand implements Runnable {

    @Inject
    CliErrorReporter errorReporter;

    /**
     * Inherited by all subcommands (Picocli {@code ScopeType.INHERIT}). The value is forwarded to
     * the {@link CliErrorReporter} singleton, where commands and importers read it — the top
     * command itself carries the CDI qualifier {@code @TopCommand} and cannot be injected plainly.
     */
    @CommandLine.Option(
            names = "--stacktrace",
            scope = CommandLine.ScopeType.INHERIT,
            description = "Print full stack traces for errors")
    void setStacktrace(final boolean stacktrace) {
        errorReporter.setStacktrace(stacktrace);
    }

    @Override
    public void run() {
        System.out.println("Use --help to see available commands");
    }
}
