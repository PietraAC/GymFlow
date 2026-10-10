import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { AssistantSuggestion, Exercise, SuggestedItem, WorkoutDay } from '../../core/api.models';
import { ExerciseMediaComponent } from '../../shared/exercise-media.component';

@Component({
  selector: 'app-suggestion-review',
  imports: [ExerciseMediaComponent],
  templateUrl: './suggestion-review.component.html',
  styleUrl: './suggestion-review.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SuggestionReviewComponent {
  @Input({ required: true }) suggestion!: AssistantSuggestion;
  @Input() exercises: Exercise[] = [];
  @Input() currentDays: WorkoutDay[] = [];
  @Input() applying = false;
  @Output() readonly discard = new EventEmitter<void>();
  @Output() readonly apply = new EventEmitter<void>();

  exerciseName(exerciseId: string | null): string {
    if (!exerciseId) return 'Exercício removido';
    return this.exercises.find(exercise => exercise.id === exerciseId)?.name ?? `Exercício ${exerciseId}`;
  }

  dayName(dayId: string): string {
    return this.currentDays.find(day => day.id === dayId)?.name ?? 'Dia existente';
  }

  itemSummary(item: SuggestedItem): string {
    if (item.durationSeconds != null) return `${item.durationSeconds}s · descanso ${item.restSeconds ?? 0}s`;
    return `${item.sets ?? '—'} séries · ${item.repetitionMin ?? '—'}–${item.repetitionMax ?? '—'} repetições · descanso ${item.restSeconds ?? 0}s`;
  }

  operationLabel(operation: string): string {
    return { ADD: 'Adicionar', REPLACE: 'Substituir', REMOVE: 'Remover' }[operation] ?? operation;
  }
}
