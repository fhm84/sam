/** SAM's role names, as the server's `Roles` and `@RolesAllowed` use them. */
export const Roles = {
  ADMIN: 'admin',
  MUSIC_LIBRARIAN: 'music_librarian',
} as const;

/** Roles that may change the archive (the server's write endpoints). */
export const WRITE_ROLES: readonly string[] = [Roles.MUSIC_LIBRARIAN, Roles.ADMIN];

/**
 * Who may open each restricted area. The route guard, the menu and the home page all read this,
 * so they can't drift apart; areas not listed are open to every login. Matches the UI routes in
 * `docs/features/api-surface.md`.
 */
export const AREA_ROLES = {
  uploads: WRITE_ROLES,
  sheetForms: WRITE_ROLES,
  ensembles: WRITE_ROLES,
  instruments: WRITE_ROLES,
  configuration: [Roles.ADMIN],
  eventLogs: [Roles.ADMIN],
} as const satisfies Record<string, readonly string[]>;
