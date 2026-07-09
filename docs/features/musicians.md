# Musicians

A shared reference catalogue of composers, arrangers, and ensemble members.

## Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Name | String (required) | Single full-name field — see [ADR-0005](../architecture/decisions/adr-0005-single-name-field.md) |
| IPI | String | Interested Party Information code (9-digit rights holder ID) |
| Birth year | Integer | |
| Death year | Integer | |
| User ID | String | OIDC subject claim — links this musician to a system user account. Null for external/historical musicians with no login. See [ADR-0001](../architecture/decisions/adr-0001-musician-user-linking.md). |

## Actions

- **Create / edit / delete** via a dialog.
- Paginated list with search by name.
- Referenced from sheets as composer / arranger.
- Can be assigned to ensembles as members (see [Ensembles & Coverage](ensembles-coverage.md)).

## Related

- [My Parts](my-parts.md) — the personalised view unlocked by user linking
- [Roadmap](../roadmap.md#3-musician-facing) — musician profile enrichment is planned
