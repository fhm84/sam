package de.halbmann.sam.cli;

import de.halbmann.sam.cli.diagnostics.StaticArgsBridge;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class CliLauncher {

    public static void main(String[] args) {
        System.out.println("CliLauncher.main - received args: " + java.util.Arrays.toString(args));
        // record args in static bridge to support dev-mode and launcher paths
        StaticArgsBridge.setArgs(args);
        try {
            Quarkus.run(args);
        } catch (Throwable t) {
            System.err.println("CliLauncher.main - Quarkus.run failed: " + t.getMessage());
            t.printStackTrace();
            throw t;
        } finally {
            System.out.println("CliLauncher.main - Quarkus.run returned");
        }
    }
}
