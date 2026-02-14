# CLI module — Diagnostics

A small, opt-in diagnostics utility is included to help debug Picocli/Quarkus CLI startup and command execution.

## Enable diagnostics ✅
Diagnostics are disabled by default. To enable them when running in Quarkus dev mode:

- Windows (example):

```powershell
mvnw.cmd -pl cli quarkus:dev -Dsam.cli.diagnostics.enabled=true -Dquarkus.args="list -v"
```

- Unix:

```bash
./mvnw -pl cli quarkus:dev -Dsam.cli.diagnostics.enabled=true -Dquarkus.args="list -v"
```

## Log category and verbosity 🔧
Diagnostics use a dedicated log category so you can control verbosity independently:

- **Category:** `de.halbmann.sam.cli.diagnostics`
- Default level is INFO. You can control it via the **namespaced** system property `sam.cli.diagnostics.level` (recommended):

```bash
# set via system property
mvnw.cmd -pl cli quarkus:dev -Dsam.cli.diagnostics.level=DEBUG -Dsam.cli.diagnostics.enabled=true -Dquarkus.args="list -v"

# Windows (PowerShell) alternative:
$env:sam.cli.diagnostics.level = 'DEBUG'; mvnw.cmd -pl cli quarkus:dev -Dsam.cli.diagnostics.enabled=true -Dquarkus.args="list -v"
# Unix:
export sam.cli.diagnostics.level=DEBUG && ./mvnw -pl cli quarkus:dev -Dsam.cli.diagnostics.enabled=true -Dquarkus.args="list -v"
```

The configuration is implemented using a property placeholder in `application.properties`:

```properties
quarkus.log.category."de.halbmann.sam.cli.diagnostics".level=${sam.cli.diagnostics.level:INFO}
```

## Behavior notes 💡
- Diagnostics will log registered subcommands and will attempt to execute the original CLI args once on startup (the args are cleared after execution to avoid re-running during long dev sessions).
- The toggle property is declared in `src/main/resources/application.properties` as:

```properties
sam.cli.diagnostics.enabled=false
quarkus.log.category."de.halbmann.sam.cli.diagnostics".level=INFO
```

## When to use
- Use these diagnostics when commands are not executed as expected in dev mode (e.g., Picocli commands created without CDI injection, missing beans, etc.).
- Keep disabled during normal operation and CI unless troubleshooting is required.

---

If you'd like, I can also add an example `--help` output or a short troubleshooting checklist to this doc.