import { Component } from '@angular/core';
import { Button } from '../shared/components/button/button';
import { Badge } from '../shared/components/badge/badge';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomInput } from '../shared/components/input/input';
import { InputDirective } from '../shared/directives/input';

@Component({
  selector: 'app-testing-playground',
  imports: [Button, Badge, ReactiveFormsModule, InputDirective, CustomInput],
  templateUrl: './testing-playground.html',
  styleUrl: './testing-playground.scss'
})
export class TestingPlayground {
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
}
