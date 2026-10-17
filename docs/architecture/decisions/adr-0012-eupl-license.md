# ADR-0012: SAM is licensed under the EUPL-1.2

**Status:** accepted

## Context

The repository was public on GitHub but had no licence, which legally means
"all rights reserved": nobody could use, modify, or redistribute the code.

SAM is a complete, self-hosted application rather than a library, so the usual
argument for a permissive licence (easy embedding in other products) carries
little weight. The realistic reuse scenarios are running it or forking it, and
the one worth guarding against is a closed-source hosted fork sold to bands and
choirs. Its likely operators are clubs and associations in Germany and the EU.

All dependencies are permissive (Apache-2.0, MIT, BSD, ISC) or weak copyleft
(Flying Saucer, LGPL-2.1, used unmodified), so none constrains the choice.

## Decision

License SAM under the **EUPL-1.2**.

Considered alternatives: Apache-2.0 / BSD / MIT (permissive — would allow closed
hosted forks) and AGPL-3.0 (equivalent network copyleft, more widely known, but
drafted for US law and English-only).

## Consequences

- Anyone may use, modify, and distribute SAM, including commercially; whoever
  distributes a modified version or offers it over a network must publish the
  source under the EUPL or a compatible licence (GPL, AGPL, LGPL, MPL, … per the
  licence appendix).
- Official texts exist in all EU languages with equal legal validity, which
  helps non-technical operators such as club boards.
- Contributions are accepted under the same licence (inbound = outbound, stated
  in the README). Code contributed before the licence was added needs its
  author's consent to be covered.
- Some companies avoid copyleft code; accepted for a community project.
- Offering SAM under a second, commercial licence later would require consent
  from every contributor.
