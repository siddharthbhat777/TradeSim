import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Card } from '../../../../../shared/components/card/card';
import { Dropdown, DropdownOption } from '../../../../../shared/components/dropdown/dropdown';
import { Exchange, ExchangeMarketClock } from '../../../../../models/exchange';

@Component({
  selector: 'app-exchange-slate',
  standalone: true,
  imports: [CommonModule, FormsModule, Card, Dropdown],
  templateUrl: './exchange-slate.html',
  styleUrl: './exchange-slate.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExchangeSlate {
  exchanges = input.required<Exchange[]>();
  selectedExchangeId = input.required<string | null>();
  clocks = input.required<Map<string, ExchangeMarketClock>>();
  stocksCount = input.required<number>();
  indicesCount = input.required<number>();

  exchangeChange = output<string | null>();

  exchangeOptions = computed<DropdownOption<string>[]>(() => {
    return this.exchanges().map(e => ({ label: `${e.name} (${e.code})`, value: e.id }));
  });

  currentExchange = computed(() => {
    const id = this.selectedExchangeId();
    return this.exchanges().find(e => e.id === id) ?? null;
  });

  currentExchangeClock = computed(() => {
    const id = this.selectedExchangeId();
    if (!id) return null;
    return this.clocks().get(id) ?? null;
  });

  onExchangeSelect(val: string | null): void {
    this.exchangeChange.emit(val);
  }
}