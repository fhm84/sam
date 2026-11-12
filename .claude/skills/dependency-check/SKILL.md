---
name: dependency-check
description: Scan SAM's Maven dependencies and the Angular project's npm packages for known CVEs using OWASP Dependency-Check. Use when asked to check for vulnerable dependencies or run a CVE/security dependency scan.
---

# Dependency Check

```bash
./mvnw dependency-check:aggregate -Pdependency-check
```

Run the goal itself, not `verify -Pdependency-check`: the goal is not bound to the build. One
report for all modules plus the UI's `package-lock.json` (about 230 Maven and 350 npm packages).
The modules' own artifacts must be in the local repository: after changes to `api`, `core` or
`storage`, run `./mvnw install -DskipTests` first.

First run downloads the NVD database (about 650 MB in `~/.m2`): this can take 10 minutes or more.
Later runs take about a minute.

With an NVD API key for faster database updates (free key at
https://nvd.nist.gov/developers/request-an-api-key):

```bash
./mvnw dependency-check:aggregate -Pdependency-check -DnvdApiKey=YOUR_KEY
```

Report output: `target/dependency-check-report/dependency-check-report.html` (and `.json`). The
build fails on a finding with a CVSS score of 7 or more.

Check the result before trusting it: the report's dependency count should be in the hundreds. A
report with a few dozen entries covers one module only.

Known finding that doesn't apply: CVE-2026-0871 on `keycloak-admin-client` is a flaw in the
Keycloak server, matched to the client library by name.
