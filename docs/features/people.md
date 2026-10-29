# People

The catalogue of everyone credited on sheets: composers, arrangers, and band members who
write arrangements. A person holds only what is printed on the music, so anyone who can read
sheets can read it ([ADR-0015](../architecture/decisions/adr-0015-person-catalogue-and-member-profile.md)).
A band member is a person with a [member profile](members.md) under the same ID.

## Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Name | String (required) | Single full-name field — see [ADR-0005](../architecture/decisions/adr-0005-single-name-field.md) |
| IPI | String | Interested Party Information code (9-digit rights holder ID) |
| Birth year | Integer | |
| Death year | Integer | |

## Actions

- **Create / edit / delete** via a dialog on the People page (`/persons`), librarian and admin.
- Paginated list with name search (prefix, substring, trigram and phonetic matching).
- Referenced from sheets as composer / arranger. Saving a sheet or applying an AI
  classification with a new name creates the person; an existing one is reused by exact name.
- A person credited on a sheet, or one with a member profile, can't be deleted (409); the error
  toast shows why.
- **Credits:** the edit dialog lists the sheets that credit the person (composer / arranger),
  each linking to the sheet (`GET /api/persons/{id}/credits`).
- The sheet form's composer and arranger fields search persons on the server as you type.

## Related

- [Members](members.md) — the band members' profiles
- [Sheets](sheets.md) — composer and arranger fields
