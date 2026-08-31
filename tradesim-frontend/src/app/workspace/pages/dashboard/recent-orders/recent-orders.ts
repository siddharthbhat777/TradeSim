import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Table, TableColumn, TableCellDirective } from '../../../../shared/components/table/table';
import { Card } from '../../../../shared/components/card/card';
import { Button } from '../../../../shared/components/button/button';
import { OrderHistoryResponse } from '../../../../models/order';

@Component({
  selector: 'app-recent-orders',
  imports: [CommonModule, RouterModule, Table, Card, Button, TableCellDirective, DatePipe],
  templateUrl: './recent-orders.html',
  styleUrl: './recent-orders.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecentOrders {
  readonly orders = input.required<OrderHistoryResponse[]>();
  readonly currency = input.required<string>();

  protected pageSize = signal(5);

  protected columns: TableColumn<OrderHistoryResponse>[] = [
    { key: 'createdAt', header: 'Date & Time', width: '20%' },
    { key: 'symbol', header: 'Symbol', width: '15%' },
    { key: 'side', header: 'Side', align: 'center', width: '10%' },
    { key: 'orderType', header: 'Type', align: 'center', width: '15%' },
    { key: 'quantity', header: 'Filled / Total', align: 'right', width: '15%' },
    { key: 'limitPrice', header: 'Price', align: 'right', width: '15%' },
    { key: 'status', header: 'Status', align: 'center', width: '10%' }
  ];

  protected formatStatus(status: string): string {
    if (!status) return '';
    const formatted = status.replace('_', ' ').toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }
}