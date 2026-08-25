import { Component, input } from '@angular/core';

@Component({
  selector: 'app-summary',
  templateUrl: './summary.html',
  styleUrl: './summary.scss'
})
export class Summary {
  equity = input.required<number>();
  totalInvested = input.required<number>();
  unrealizedPnl = input.required<number>();
  buyingPower = input.required<number>();
}