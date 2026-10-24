import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AREA_ROLES } from '../../core/auth/roles';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

type Area = keyof typeof AREA_ROLES;

@Component({
  selector: 'app-home',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {
  private readonly auth = inject(AuthService);

  protected mayOpen(area: Area): boolean {
    return this.auth.hasAnyRole(AREA_ROLES[area]);
  }

  protected readonly showAdminLinks = computed(() =>
    (['ensembles', 'instruments', 'configuration', 'eventLogs'] as const).some((area) => this.mayOpen(area)),
  );
}
