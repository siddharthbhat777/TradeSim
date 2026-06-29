import { booleanAttribute, Component, computed, contentChild, input, ViewEncapsulation } from '@angular/core';
import type { ValidationErrors } from '@angular/forms';
import { InputDirective } from '../../directives/input';

export type InputErrorMessages = Record<
  string,
  string | ((error: unknown, errors: ValidationErrors) => string)
>;

const booleanAttributeOrNull = (value: unknown): boolean | null => {
  if (value === null || value === undefined) {
    return null;
  }

  return booleanAttribute(value);
};

@Component({
  selector: 'app-input',
  imports: [],
  templateUrl: './input.html',
  styleUrl: './input.scss',
  encapsulation: ViewEncapsulation.None
})
export class CustomInput {
  private readonly inputDirective = contentChild(InputDirective);

  label = input('');
  helperText = input('');
  errorText = input('');
  errorMessages = input<InputErrorMessages>({});

  required = input(false, { transform: booleanAttribute });
  fullWidth = input(false, { transform: booleanAttribute });
  showErrors = input(true, { transform: booleanAttribute });
  reserveMessageSpace = input<boolean | null>(null, {
    transform: booleanAttributeOrNull
  });

  shouldReserveMessageSpace = computed(() => {
    if (this.reserveMessageSpace() !== null) {
      return this.reserveMessageSpace();
    }

    const control = this.inputDirective()?.control;

    return !!(
      this.errorText() ||
      this.helperText() ||
      Object.keys(this.errorMessages()).length ||
      this.inputDirective()?.required ||
      control?.validator ||
      control?.asyncValidator
    );
  });

  inputId(): string | null {
    return this.inputDirective()?.inputId ?? null;
  }

  isRequired(): boolean {
    return this.required() || !!this.inputDirective()?.required;
  }

  shouldShowError(): boolean {
    return this.showErrors() && !!this.inputDirective()?.isInvalid();
  }

  getErrorMessage(): string {
    if (this.errorText()) {
      return this.errorText();
    }

    const errors = this.inputDirective()?.control?.errors;

    if (!errors) {
      return 'Invalid value';
    }

    const firstErrorKey = Object.keys(errors)[0];
    const customMessage = this.errorMessages()[firstErrorKey];

    if (typeof customMessage === 'function') {
      return customMessage(errors[firstErrorKey], errors);
    }

    return customMessage ?? this.getDefaultErrorMessage(firstErrorKey, errors[firstErrorKey]);
  }

  private getDefaultErrorMessage(errorKey: string, error: unknown): string {
    const errorValue = error as { requiredLength?: number; min?: number; max?: number };

    switch (errorKey) {
      case 'required':
        return 'This field is required';
      case 'email':
        return 'Enter a valid email address';
      case 'min':
        return `Minimum value is ${errorValue.min}`;
      case 'max':
        return `Maximum value is ${errorValue.max}`;
      case 'minlength':
        return `Minimum length is ${errorValue.requiredLength}`;
      case 'maxlength':
        return `Maximum length is ${errorValue.requiredLength}`;
      case 'pattern':
        return 'Invalid format';
      default:
        return 'Invalid value';
    }
  }
}