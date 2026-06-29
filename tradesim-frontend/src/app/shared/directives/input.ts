import { booleanAttribute, Directive, ElementRef, inject, input } from '@angular/core';
import { NgControl } from '@angular/forms';

export type InputSize = 'small' | 'medium' | 'large';
export type InputValidationMode = 'touched' | 'dirty' | 'touchedOrDirty' | 'always';

let nextInputId = 0;

@Directive({
  selector: 'input[appInput], textarea[appInput]',
  host: {
    class: 'app-input',
    '[attr.id]': 'inputId',
    '[class.app-input--small]': "size() === 'small'",
    '[class.app-input--large]': "size() === 'large'",
    '[class.app-input--full-width]': 'fullWidth()',
    '[class.app-input--invalid]': 'isInvalid()',
    '[class.app-input--textarea]': 'isTextarea',
    '[attr.aria-invalid]': "isInvalid() ? 'true' : null"
  }
})
export class InputDirective {
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly ngControl = inject(NgControl, { optional: true, self: true });
  private readonly generatedId = `app-input-${nextInputId++}`;

  protected readonly isTextarea = this.elementRef.nativeElement.tagName.toLowerCase() === 'textarea';

  size = input<InputSize>('medium', { alias: 'appInputSize' });
  validationMode = input<InputValidationMode>('touched', { alias: 'appInputValidationMode' });

  fullWidth = input(false, { alias: 'appInputFullWidth', transform: booleanAttribute });
  invalid = input(false, { alias: 'appInputInvalid', transform: booleanAttribute });

  get inputId(): string {
    return this.elementRef.nativeElement.id || this.generatedId;
  }

  get required(): boolean {
    return this.elementRef.nativeElement.hasAttribute('required');
  }

  get control() {
    return this.ngControl?.control ?? null;
  }

  isInvalid(): boolean {
    if (this.invalid()) {
      return true;
    }

    const control = this.control;

    if (!control || control.disabled || !control.invalid) {
      return false;
    }

    return this.shouldShowValidationState(control.touched, control.dirty);
  }

  private shouldShowValidationState(touched: boolean, dirty: boolean): boolean {
    switch (this.validationMode()) {
      case 'always':
        return true;
      case 'touched':
        return touched;
      case 'dirty':
        return dirty;
      case 'touchedOrDirty':
        return touched || dirty;
    }
  }
}