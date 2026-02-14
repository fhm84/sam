package de.halbmann.sam.cli.diagnostics;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CliQuarkusApp implements QuarkusApplication {

    @Inject
    ArgsHolder argsHolder;

    @Override
    public int run(String... args) throws Exception {
        // store args for diagnostics
        argsHolder.setArgs(args);

        // keep the application running (like Quarkus.run would do)
        Quarkus.waitForExit();
        return 0;
    }
}
