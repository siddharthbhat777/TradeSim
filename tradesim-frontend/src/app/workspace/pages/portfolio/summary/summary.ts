import { Component, input } from '@angular/core';
import { Card } from '../../../../shared/components/card/card';
import { PriceIndicator } from '../../../../shared/components/price-indicator/price-indicator';

@Component({
  selector: 'app-summary',
  imports: [Card, PriceIndicator],
  templateUrl: './summary.html',
  styleUrl: './summary.scss'
})
export class Summary {
  equity = input.required<number>();
  totalInvested = input.required<number>();
  unrealizedPnl = input.required<number>();
  buyingPower = input.required<number>();
  currency = input.required<string>();
}