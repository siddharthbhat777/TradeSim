import { Component, input, model, computed, contentChild, TemplateRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Checkbox } from '../checkbox';

@Component({
  selector: 'app-checkbox-group',
  imports: [CommonModule, Checkbox],
  templateUrl: './checkbox-group.html',
  styleUrl: './checkbox-group.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CheckboxGroup {
  options = input<any[]>([]);
  selectedValues = model<any[]>([]);

  displayKey = input<string>('label');
  valueKey = input<string>('value');

  showSelectAll = input<boolean>(false);
  selectAllText = input<string>('Select All');
  layout = input<'column' | 'row'>('column');

  optionTemplate = contentChild<TemplateRef<any>>('optionTemplate');

  allSelected = computed(() => {
    const opts = this.options();
    const selected = this.selectedValues();
    return opts.length > 0 && opts.every(opt => selected.includes(opt[this.valueKey()]));
  });

  isIndeterminate = computed(() => {
    const opts = this.options();
    const selected = this.selectedValues();
    return selected.length > 0 && selected.length < opts.length;
  });

  isSelected(value: any): boolean {
    return this.selectedValues().includes(value);
  }

  toggleAll(checked: boolean) {
    if (checked) {
      const allValues = this.options().map(opt => opt[this.valueKey()]);
      this.selectedValues.set(allValues);
    } else {
      this.selectedValues.set([]);
    }
  }

  toggleItem(value: any, checked: boolean) {
    const currentSelected = [...this.selectedValues()];

    if (checked) {
      currentSelected.push(value);
    } else {
      const index = currentSelected.indexOf(value);
      if (index > -1) {
        currentSelected.splice(index, 1);
      }
    }

    this.selectedValues.set(currentSelected);
  }
}