/** SAM's role names, as the server's `Roles` and `@RolesAllowed` use them. */
export const Roles = {
  ADMIN: 'admin',
  MUSIC_LIBRARIAN: 'music_librarian',
} as const;

/** Roles that may change the archive (the server's write endpoints). */
export const WRITE_ROLES: readonly string[] = [Roles.MUSIC_LIBRARIAN, Roles.ADMIN];
