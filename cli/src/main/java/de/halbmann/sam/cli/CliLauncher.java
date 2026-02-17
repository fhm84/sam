package de.halbmann.sam.cli;

import de.halbmann.sam.cli.diagnostics.StaticArgsBridge;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.extern.slf4j.Slf4j;

@QuarkusMain
@Slf4j
public class CliLauncher {

    public static void main(String[] args) {
        log.debug("Received args: {}", java.util.Arrays.toString(args));
        StaticArgsBridge.setArgs(args);
        try {
            Quarkus.run(args);
        } catch (Throwable t) {
            log.error("Quarkus.run failed: {}", t.getMessage(), t);
            throw t;
        } finally {
            log.debug("Quarkus.run returned");
        }
    }
}
