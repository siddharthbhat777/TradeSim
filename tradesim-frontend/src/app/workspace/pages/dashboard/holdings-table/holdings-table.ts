import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Card } from '../../../../shared/components/card/card';
import { Table, TableCellDirective, TableColumn } from '../../../../shared/components/table/table';
import { PriceIndicator } from '../../../../shared/components/price-indicator/price-indicator';
import { PortfolioHoldingResponse } from '../../../../models/portfolio';

@Component({
  selector: 'app-holdings-table',
  imports: [Card, Table, TableCellDirective, PriceIndicator, CurrencyPipe, DecimalPipe],
  templateUrl: './holdings-table.html',
  styleUrl: './holdings-table.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HoldingsTable {
  readonly holdings = input.required<PortfolioHoldingResponse[]>();
  readonly currency = input.required<string>();

  readonly pageSize = signal(5);

  readonly columns: TableColumn<PortfolioHoldingResponse>[] = [
    { key: 'symbol', header: 'Symbol', align: 'left' },
    { key: 'quantity', header: 'Qty', align: 'right' },
    { key: 'averageBuyPrice', header: 'Avg Price', align: 'right' },
    { key: 'currentPrice', header: 'CMP', align: 'right' },
    { key: 'currentValue', header: 'Value', align: 'right' },
    { key: 'unrealizedPnl', header: 'PnL', align: 'right' }
  ];
}