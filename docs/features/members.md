# Members

The band's members: the profile of a [person](people.md) who belongs to the band, with the
personal data that only the music librarian and the admin see. The profile shares the
person's ID, so a member who writes arrangements is credited on sheets under the same ID
([ADR-0015](../architecture/decisions/adr-0015-person-catalogue-and-member-profile.md)).

## Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Name | String (required) | The person's name; changing it renames the person everywhere it is credited |
| Status | Enum | `ACTIVE` · `PAUSED` (temporarily not playing; login and data stay) · `FORMER` (left the band). Setting `FORMER` sets the leaving date and removes the login link at once; a former member can't be linked to a login (409) |
| Left at | Date | When the member left; starts the retention period of their personal data. Only for `music_librarian` / `admin` |
| Contact (email, mobile, notes) | Strings | Returned only to `music_librarian` / `admin`, like the linked login, the last invite time and the onboarding state; other callers get them removed wherever a member appears (`MemberMapper`, `PersonalDataAccess`). Notes are organisational only — no health data |
| User ID | String | OIDC subject claim — links this member to a login. Admins link it in the member form by searching the login provider's users, or, with Pocket ID and no API key, by entering the user's ID ([security](../architecture/concepts/security.md)). See [ADR-0001](../architecture/decisions/adr-0001-musician-user-linking.md). |
| Onboarding | Enum, derived | `NONE` · `INVITED` (invite sent, no login yet) · `LINKED` (login linked) |
| Last invite sent | Timestamp | Tracks self-service onboarding invites |
| Instruments | List | Instruments the member *can* play (`member_instruments`, one marked primary), distinct from the instrument of an ensemble seat |

The role in an ensemble (member, guest, substitute, conductor) is per seat, see
[Ensembles & Coverage](ensembles-coverage.md#ensemble-members).

## Actions

- **Create / edit / delete** via a dialog on the Members page (`/members`), librarian and admin.
  A new member either gets a new person or becomes the profile of an existing person
  (form field "Existing person", "Make … a member" in the ensemble seat picker; API: `id` set),
  so a credited arranger who joins the band stays one record.
- **Delete erases** the profile, its instruments and ensemble seats and, after commit, their
  audit history (GDPR Art. 17). The person stays when it is credited on a sheet, otherwise it
  goes too. The login account at the login provider is deleted there by the admin.
- Paginated list sorted by name, filterable by name and status; shows instruments (primary
  highlighted) and status to everyone, contact data only in the librarian's form.
- Can be given seats in ensembles (see [Ensembles & Coverage](ensembles-coverage.md)).

## Related

- [People](people.md) — the catalogue of credited persons
- [My Parts](my-parts.md) — the personalised view unlocked by user linking
- [Roadmap](../roadmap.md#3-musician-facing) — further musician-facing ideas
