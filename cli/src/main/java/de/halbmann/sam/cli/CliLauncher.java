package de.halbmann.sam.cli;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * Entry point: executes the Picocli top command exactly once and exits with its return code.
 * The injected {@link CommandLine} comes from the quarkus-picocli extension and creates
 * subcommands via CDI, so {@code @Inject} works inside commands.
 */
@QuarkusMain
public class CliLauncher implements QuarkusApplication {

    @Inject
    CommandLine commandLine;

    public static void main(String[] args) {
        Quarkus.run(CliLauncher.class, args);
    }

    @Override
    public int run(String... args) {
        return commandLine.execute(args);
    }
}
