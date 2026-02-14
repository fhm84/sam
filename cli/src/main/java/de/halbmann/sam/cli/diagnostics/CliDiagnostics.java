package de.halbmann.sam.cli.diagnostics;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.Arrays;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import picocli.CommandLine;

@ApplicationScoped
public class CliDiagnostics {

    @Inject
    CommandLine commandLine;

    @Inject
    ArgsHolder argsHolder;

    @ConfigProperty(name = "sam.cli.diagnostics.enabled", defaultValue = "false")
    boolean enabled;

    private static final Logger LOG = Logger.getLogger("de.halbmann.sam.cli.diagnostics");

    void onStart(@Observes StartupEvent ev) {
        if (!enabled) {
            LOG.debug("CliDiagnostics: disabled");
            return;
        }
        try {
            var subcommands = commandLine.getSubcommands().keySet();
            String[] args = argsHolder.getArgs();
            if (args.length == 0) {
                args = StaticArgsBridge.getArgs();
                LOG.debugf(
                        "CliDiagnostics: no args in ArgsHolder, falling back to StaticArgsBridge: %s",
                        Arrays.toString(args));
            }
            LOG.infof("CliDiagnostics: subcommands=%s args=%s", subcommands, Arrays.toString(args));

            if (args.length > 0) {
                LOG.infof("CliDiagnostics: executing=%s", Arrays.toString(args));
                int rc = commandLine.execute(args);
                LOG.infof("CliDiagnostics: rc=%d %s", rc, rc == 0 ? "(OK)" : "(FAIL)");
                // defensive: clear stored args to avoid re-execution in long-running dev sessions
                argsHolder.setArgs();
                LOG.debug("CliDiagnostics: cleared args");
            } else {
                LOG.debug("CliDiagnostics: no args");
            }
        } catch (Throwable t) {
            LOG.errorf(t, "CliDiagnostics: error: %s", t.getMessage());
        }
    }
}
