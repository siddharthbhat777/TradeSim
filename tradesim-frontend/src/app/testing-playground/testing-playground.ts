import { Component, inject, signal } from '@angular/core';
import { Button } from '../shared/components/button/button';
import { Badge } from '../shared/components/badge/badge';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomInput } from '../shared/components/input/input';
import { InputDirective } from '../shared/directives/input';
import { CardBorder, CardComponent, CardVariant } from '../shared/components/card/card';
import { Dialog } from '../shared/components/dialog/dialog';
import { PriceIndicator } from '../shared/components/price-indicator/price-indicator';
import { EmptyState } from '../shared/components/empty-state/empty-state';
import { Router } from '@angular/router';

@Component({
  selector: 'app-testing-playground',
  imports: [Button, Badge, ReactiveFormsModule, InputDirective, CustomInput, CardComponent, Dialog, PriceIndicator, EmptyState],
  templateUrl: './testing-playground.html',
  styleUrl: './testing-playground.scss'
})
export class TestingPlayground {
  readonly router = inject(Router);

  readonly inputDemoForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
    search: new FormControl('', {
      nonNullable: true
    }),
    notes: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(10)]
    })
  });

  protected readonly variants: CardVariant[] = [
    'default',
    'surface',
    'outline',
    'accent',
    'success',
    'danger',
    'warning',
  ];

  protected readonly borders: CardBorder[] = [
    'primary',
    'secondary',
    'accent',
    'success',
    'danger',
    'warning',
  ];

  protected readonly clickCount = signal(0);

  protected readonly handleCardClick = () => {
    this.clickCount.update((count) => count + 1);
  };

  protected readonly isBasicDialogOpen = signal(false);
  protected readonly isBodyOnlyDialogOpen = signal(false);
  protected readonly isHeaderOnlyDialogOpen = signal(false);
  protected readonly isForcedDialogOpen = signal(false);
  protected readonly isCustomContentDialogOpen = signal(false);
}
