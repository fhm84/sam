import { RoleMenuItem, withRoles } from './app-menu';

describe('withRoles', () => {
  const menu: RoleMenuItem[] = [
    {
      label: 'Main',
      items: [
        { label: 'Sheets', routerLink: ['/sheets'] },
        { label: 'Uploads', routerLink: ['/uploads'], roles: ['music_librarian', 'admin'] },
      ],
    },
    {
      label: 'Administration',
      items: [{ label: 'Event log', routerLink: ['/admin/event-logs'], roles: ['admin'] }],
    },
  ];

  const as = (...userRoles: string[]) =>
    withRoles(menu, (roles) => roles.some((r) => userRoles.includes(r)));
  const visibleLabels = (items: ReturnType<typeof as>) =>
    items.flatMap((group) =>
      group.visible === false ? [] : [group.label, ...(group.items ?? []).filter((i) => i.visible !== false).map((i) => i.label)],
    );

  it('shows only unrestricted items, and hides empty groups, for a user without roles', () => {
    expect(visibleLabels(as())).toEqual(['Main', 'Sheets']);
  });

  it('shows the librarian the write areas but not admin-only items', () => {
    expect(visibleLabels(as('music_librarian'))).toEqual(['Main', 'Sheets', 'Uploads']);
  });

  it('shows the admin everything', () => {
    expect(visibleLabels(as('admin'))).toEqual(['Main', 'Sheets', 'Uploads', 'Administration', 'Event log']);
  });

  it('does not pass the roles list on to the menu', () => {
    expect(as('admin')[0].items?.[1]).not.toHaveProperty('roles');
  });
});
