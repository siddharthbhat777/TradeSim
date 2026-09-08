import { ChangeDetectionStrategy, Component, computed, contentChild, contentChildren, Directive, EventEmitter, inject, input, model, Output, signal, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Pagination } from '../pagination/pagination';
import { InlineLoader } from '../loaders/inline-loader/inline-loader';
import { EmptyState } from '../empty-state/empty-state';

@Directive({ selector: '[tableCell]' })
export class TableCellDirective {
  readonly name = input.required<string>({ alias: 'tableCell' });
  readonly template = inject(TemplateRef);
}

@Directive({ selector: '[tableExpandedRow]' })
export class TableExpandedRowDirective {
  readonly template = inject(TemplateRef);
}

export interface TableColumn<T = any> {
  key: string;
  header: string;
  align?: 'left' | 'center' | 'right';
  width?: string;
}

@Component({
  selector: 'app-table',
  imports: [CommonModule, Pagination, InlineLoader, EmptyState],
  templateUrl: './table.html',
  styleUrl: './table.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Table<T = any> {
  readonly columns = input.required<TableColumn<T>[]>();
  readonly data = input.required<T[]>();
  readonly variant = input<'default' | 'flush'>('default');
  readonly paginated = input<boolean>(true);
  readonly isLoading = input<boolean>(false);
  readonly paginationLabelSuffix = input<string>(' per page');
  readonly paginationMobileLabelSuffix = input<string>(' / pg');
  readonly pageSizeOptions = input<number[]>([10, 25, 50, 100]);
  readonly emptyText = input<string>('No data to display');
  readonly interactiveRows = input<boolean>(false);

  readonly currentPage = model<number>(1);
  readonly pageSize = model<number>(10);

  @Output() rowClick = new EventEmitter<T>();

  readonly cells = contentChildren(TableCellDirective);
  readonly expandedRowTemplate = contentChild(TableExpandedRowDirective);

  readonly expandedRows = signal<Set<T>>(new Set());

  handleRowClick(row: T): void {
    if (this.expandedRowTemplate()) {
      const current = new Set(this.expandedRows());
      if (current.has(row)) {
        current.delete(row);
      } else {
        current.add(row);
      }
      this.expandedRows.set(current);
    }

    this.rowClick.emit(row);
  }

  isExpanded(row: T): boolean {
    return this.expandedRows().has(row);
  }

  getCellTemplate(key: string): TemplateRef<any> | null {
    const cell = this.cells().find(c => c.name() === key);
    return cell ? cell.template : null;
  }

  readonly paginatedData = computed(() => {
    const allData = this.data();
    if (!this.paginated()) return allData;
    const size = this.pageSize();
    const page = this.currentPage();
    const start = (page - 1) * size;
    return allData.slice(start, start + size);
  });
}