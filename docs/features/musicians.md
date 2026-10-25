# Musicians

A shared reference catalogue of composers, arrangers, and ensemble members.

## Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Name | String (required) | Single full-name field — see [ADR-0005](../architecture/decisions/adr-0005-single-name-field.md) |
| IPI | String | Interested Party Information code (9-digit rights holder ID) |
| Birth year | Integer | |
| Death year | Integer | |
| User ID | String | OIDC subject claim — links this musician to a system user account. Null for external/historical musicians with no login. Admins link it in the musician form by searching the login provider's users, or, with Pocket ID and no API key, by entering the user's ID ([security](../architecture/concepts/security.md)). See [ADR-0001](../architecture/decisions/adr-0001-musician-user-linking.md). |
| Contact (email, mobile, notes) | Strings | Returned only to `music_librarian` / `admin`, like the linked login and the last invite time; other callers get them removed wherever a musician appears (`MusicianMapper`, `PersonalDataAccess`) |
| Status | Enum | `ACTIVE` · `INACTIVE` · `INVITED` · `PENDING` |
| Role | Enum | `MEMBER` · `GUEST` · `SUBSTITUTE` · `CONDUCTOR`, independent of the per-ensemble conductor flag on memberships |
| Last invite sent | Timestamp | Tracks self-service onboarding invites |
| Instruments | List | Instruments the musician *can* play (`musician_instruments`, one marked primary), distinct from the per-ensemble membership instrument |

## Actions

- **Create / edit / delete** via a dialog.
- Paginated list with search by name; it shows instruments (primary highlighted), role and status to everyone, contact data only in the librarian's form.
- Referenced from sheets as composer / arranger.
- Can be assigned to ensembles as members (see [Ensembles & Coverage](ensembles-coverage.md)).

## Related

- [My Parts](my-parts.md) — the personalised view unlocked by user linking
- [Roadmap](../roadmap.md#3-musician-facing) — further musician-facing ideas
