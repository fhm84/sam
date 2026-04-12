import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Toolbar } from 'primeng/toolbar';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService } from '../../core/api';
import { Ensemble } from '../../model/datamodels';
import { EnsembleVoices } from './ensemble-voices';
import { EnsembleMemberships } from './ensemble-memberships';
import { EnsembleForm } from './ensemble-form';

@Component({
  selector: 'app-ensemble-detail-page',
  imports: [EnsembleVoices, EnsembleMemberships, EnsembleForm, Button, Dialog, Toolbar],
  templateUrl: './ensemble-detail-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnsembleDetailPage implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(EnsemblesApiService);
  private readonly messageService = inject(MessageService);

  protected readonly ensembleId = signal('');
  protected readonly ensemble = signal<Ensemble | null>(null);
  protected editDialogVisible = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.ensembleId.set(id);
    this.loadEnsemble(id);
  }

  protected goBack(): void {
    this.router.navigate(['/admin/ensembles']);
  }

  protected openEdit(): void {
    this.editDialogVisible = true;
  }

  protected onEditSaved(): void {
    this.editDialogVisible = false;
    this.messageService.add({
      severity: 'success',
      summary: this.t.t('ensembles.messages.updated'),
    });
    this.loadEnsemble(this.ensembleId());
  }

  private loadEnsemble(id: string): void {
    this.api.load(id).subscribe({
      next: (e) => this.ensemble.set(e),
      error: () => {},
    });
  }
}
