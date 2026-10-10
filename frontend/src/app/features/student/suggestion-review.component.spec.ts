import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { AssistantSuggestion } from '../../core/api.models';
import { SuggestionReviewComponent } from './suggestion-review.component';

describe('SuggestionReviewComponent', () => {
  it('shows every proposed exercise and its prescription before application', () => {
    const fixture: ComponentFixture<SuggestionReviewComponent> = TestBed.createComponent(SuggestionReviewComponent);
    const suggestion: AssistantSuggestion = {
      id:'suggestion-1', planId:'plan-1', basePlanVersion:1, contextFingerprint:'fp', status:'AVAILABLE',
      source:'DEMO', kind:'WORKOUT_COMPLETION', explanation:'Plano proposto', observations:[], changes:[],
      completion:{newDays:[{position:1,name:'Pernas',items:[{exerciseId:'exercise-1',position:1,sets:3,
        repetitionMin:8,repetitionMax:12,durationSeconds:null,restSeconds:60,reason:'Movimento principal'}]}],existingDayAdditions:[]},
      createdAt:new Date().toISOString(), expiresAt:new Date(Date.now()+60_000).toISOString()
    };
    fixture.componentRef.setInput('suggestion', suggestion);
    fixture.componentRef.setInput('exercises', [{id:'exercise-1',name:'Agachamento',kind:'STRENGTH',
      primaryMuscleGroups:['PERNAS'],difficulty:'BEGINNER',instructions:''}]);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Agachamento');
    expect(text).toContain('3 séries');
    expect(text).toContain('8–12 repetições');
    expect(text).toContain('Movimento principal');
  });
});
