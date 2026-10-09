import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-exercise-media',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.compact]': 'compact' },
  template: `
    <div class="exercise-media" [class.is-compact]="compact">
      @if (!failed && exerciseId) {
        <img
          [src]="mediaUrl"
          [alt]="'Demonstração de execução: ' + exerciseName"
          loading="lazy"
          (error)="failed = true"
        >
      }
      @if (failed || !exerciseId) {
        <div class="exercise-media-placeholder" aria-label="Espaço reservado para GIF de execução">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M8 5.5v13l10-6.5z" fill="currentColor"/>
          </svg>
          @if (!compact) {
            <span>GIF de execução</span>
            <small>Em breve</small>
          }
        </div>
      }
    </div>
  `
})
export class ExerciseMediaComponent {
  @Input() exerciseId = '';
  @Input() exerciseName = 'Exercício';
  @Input() compact = false;
  failed = false;

  get mediaUrl(): string {
    return `/exercises/${encodeURIComponent(this.exerciseId)}.gif`;
  }
}
