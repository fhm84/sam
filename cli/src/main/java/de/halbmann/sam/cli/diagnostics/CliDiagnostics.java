package de.halbmann.sam.cli.diagnostics;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import picocli.CommandLine;

/**
 * Opt-in startup diagnostics: logs the registered Picocli subcommands so CDI/registration problems
 * are visible. Purely observational — command execution happens in {@code CliLauncher}.
 */
@ApplicationScoped
public class CliDiagnostics {

    @Inject
    CommandLine commandLine;

    @ConfigProperty(name = "sam.cli.diagnostics.enabled", defaultValue = "false")
    boolean enabled;

    private static final Logger LOG = Logger.getLogger("de.halbmann.sam.cli.diagnostics");

    void onStart(@Observes StartupEvent ev) {
        if (!enabled) {
            return;
        }
        LOG.infof("CliDiagnostics: subcommands=%s", commandLine.getSubcommands().keySet());
    }
}
