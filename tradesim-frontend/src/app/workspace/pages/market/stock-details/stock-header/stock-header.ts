import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Stock } from '../../../../../models/stock';
import { Button } from '../../../../../shared/components/button/button';
import { Badge } from '../../../../../shared/components/badge/badge';
import { PriceIndicator } from '../../../../../shared/components/price-indicator/price-indicator';

@Component({
  selector: 'app-stock-header',
  imports: [CommonModule, Button, Badge, PriceIndicator],
  templateUrl: './stock-header.html',
  styleUrl: './stock-header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockHeader {
  readonly stock = input.required<Stock>();
  readonly back = output<void>();

  onBack(): void {
    this.back.emit();
  }
}