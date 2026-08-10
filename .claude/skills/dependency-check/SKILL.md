---
name: dependency-check
description: Scan SAM's Maven dependencies for known CVEs using OWASP Dependency-Check. Use when asked to check for vulnerable dependencies or run a CVE/security dependency scan.
---

# Dependency Check

```bash
./mvnw verify -Pdependency-check
```

First run downloads the NVD database (~200MB) — this can take a while.

With an NVD API key for faster DB updates (free key at https://nvd.nist.gov/developers/request-an-api-key):

```bash
./mvnw verify -Pdependency-check -DnvdApiKey=YOUR_KEY
```

Report output: `target/dependency-check-report/dependency-check-report.html`
