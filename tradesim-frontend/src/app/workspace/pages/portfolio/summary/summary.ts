import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Card } from '../../../../shared/components/card/card';
import { PriceIndicator } from '../../../../shared/components/price-indicator/price-indicator';
import { PortfolioResponse } from '../../../../models/portfolio';
import { RiskResponse } from '../../../../models/risk';

@Component({
  selector: 'app-summary',
  imports: [CommonModule, Card, PriceIndicator],
  templateUrl: './summary.html',
  styleUrl: './summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Summary {
  readonly portfolio = input.required<PortfolioResponse | null>();
  readonly risk = input.required<RiskResponse | null>();
  readonly baseCurrency = input.required<string>();

  readonly marginUsedPercent = computed(() => {
    const r = this.risk();
    if (!r || r.equity === 0) return 0;
    const pct = (r.marginUsed / r.equity) * 100;
    return pct > 100 ? 100 : pct;
  });
}