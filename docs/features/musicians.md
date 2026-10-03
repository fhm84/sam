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
| Contact (email, mobile, notes) | Strings | Notes are meant for the music librarian only. The API currently returns them to every authenticated user (see [role-aware access plan](../plans/rbac-role-aware-access.md)) |
| Status | Enum | `ACTIVE` · `INACTIVE` · `INVITED` · `PENDING` |
| Role | Enum | `MEMBER` · `GUEST` · `SUBSTITUTE` · `CONDUCTOR`, independent of the per-ensemble conductor flag on memberships |
| Last invite sent | Timestamp | Tracks self-service onboarding invites |
| Instruments | List | Instruments the musician *can* play (`musician_instruments`, one marked primary), distinct from the per-ensemble membership instrument |

## Actions

- **Create / edit / delete** via a dialog.
- Paginated list with search by name.
- Referenced from sheets as composer / arranger.
- Can be assigned to ensembles as members (see [Ensembles & Coverage](ensembles-coverage.md)).

## Related

- [My Parts](my-parts.md) — the personalised view unlocked by user linking
- [Roadmap](../roadmap.md#3-musician-facing) — further musician-facing ideas
