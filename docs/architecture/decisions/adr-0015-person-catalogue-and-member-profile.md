# ADR-0015: Person catalogue plus optional member profile

**Status:** accepted

## Context

One `musicians` table holds two kinds of record: the people credited on sheets
(composers, arrangers, mostly external or historical, often created by AI
classification) and the members of the band (contact details, status, role,
instruments, ensemble memberships, a login). Their fields barely overlap, and they need
different access rules: credits are public facts, member data is personal data. With
one table, the personal-data filter runs wherever a composer is embedded, and the
fields of the band roster are empty for every composer.

Both kinds overlap in one common case: a band member who writes arrangements for the
band (often the conductor). Two unrelated tables would turn that person into two
records.

Composer, arranger etc. are not types of person. Someone composes one piece and
arranges another; the role belongs to the credit on the sheet.

## Decision

- `persons` (`Person`) is the catalogue: name, birth and death year, IPI. Sheets
  reference it as composer and arranger. Anyone who can read sheets can read it.
- `members` (`Member`) is an optional profile of a person who belongs to the band:
  contact details, notes, membership status, invite time, login (`userId`),
  instruments and ensemble memberships. It shares the person's primary key (one profile
  per person at most). Only librarians and the admin see its personal data, and GDPR
  deletion and retention apply to it.
- No type or role field on the person. Credit roles stay on the sheet; the role in an
  ensemble (member, guest, substitute, conductor) is on the ensemble membership.
- "Musician" is no longer a model name; it stays the everyday word for a member.

## Consequences

- The arranging conductor is one person with a member profile; nothing to deduplicate.
- Personal data lives in the member tables only: access control, deletion and retention
  apply there, and sheets never carry personal data.
- AI classification and sheet forms only create persons, never member profiles.
- Every member has a person row; members show up in the composer/arranger picker, which
  is what the overlapping case needs.
- [ADR-0001](adr-0001-musician-user-linking.md) still holds with the new name: `userId`
  is on the member, and a login always belongs to a band member.
- [ADR-0005](adr-0005-single-name-field.md) still holds; the single name moves to the
  person.

Plan: [Person catalogue and member profiles](../../plans/people-catalogue.md).
