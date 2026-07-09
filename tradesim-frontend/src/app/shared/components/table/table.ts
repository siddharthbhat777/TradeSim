import {
  booleanAttribute,
  Component,
  TemplateRef,
  computed,
  contentChildren,
  input,
  model,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { Directive, inject } from '@angular/core';

import { Pagination, type PaginationSize } from '../pagination/pagination';
import { EmptyState } from '../empty-state/empty-state';

export interface TableColumn<T> {
  key: string;
  header: string;
  align?: 'left' | 'center' | 'right';
  width?: string;
  accessor?: (row: T) => unknown;
}

export interface TableCellContext<T> {
  $implicit: unknown;
  row: T;
}

@Directive({
  selector: 'ng-template[tableCell]'
})
export class TableCellDirective<T = unknown> {
  readonly tableCell = input.required<string>();
  readonly templateRef = inject(TemplateRef<TableCellContext<T>>);
}

@Component({
  selector: 'app-table',
  imports: [Pagination, EmptyState, NgTemplateOutlet],
  templateUrl: './table.html',
  styleUrl: './table.scss'
})
export class Table<T = unknown> {
  readonly columns = input.required<TableColumn<T>[]>();
  readonly data = input.required<T[]>();
  readonly rowKey = input<(row: T) => unknown>();

  readonly paginated = input(true, { transform: booleanAttribute });
  readonly pageSizeOptions = input<number[]>([10, 25, 50, 100]);
  readonly size = input<PaginationSize>('medium');

  readonly currentPage = model(1);
  readonly pageSize = model(10);

  readonly emptyText = input('No data to display');
  readonly emptySubtext = input('');

  private readonly cellTemplates = contentChildren(TableCellDirective);

  protected readonly templateMap = computed(() => {
    const map = new Map<string, TemplateRef<TableCellContext<T>>>();

    for (const directive of this.cellTemplates()) {
      map.set(directive.tableCell(), directive.templateRef as TemplateRef<TableCellContext<T>>);
    }

    return map;
  });

  protected readonly pagedData = computed(() => {
    if (!this.paginated()) {
      return this.data();
    }

    const start = (this.currentPage() - 1) * this.pageSize();
    return this.data().slice(start, start + this.pageSize());
  });

  protected getValue(row: T, column: TableColumn<T>): unknown {
    if (column.accessor) {
      return column.accessor(row);
    }

    return (row as Record<string, unknown>)[column.key];
  }

  protected trackRow = (index: number, row: T): unknown => {
    return this.rowKey()?.(row) ?? index;
  };
}