import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Card } from '../../../../../shared/components/card/card';
import { Badge } from '../../../../../shared/components/badge/badge';
import { Button } from '../../../../../shared/components/button/button';
import { Table, TableColumn, TableCellDirective, TableExpandedRowDirective } from '../../../../../shared/components/table/table';
import { CustomInput } from '../../../../../shared/components/input/input';
import { InputDirective } from '../../../../../shared/directives/input';
import { FormatCurrencyPipe } from '../../../../../shared/pipes/format-currency-pipe';
import { Dropdown } from '../../../../../shared/components/dropdown/dropdown';
import { Drawer } from '../../../../../shared/components/drawer/drawer';
import { Slider } from '../../../../../shared/components/slider/slider';
import { CheckboxGroup } from '../../../../../shared/components/checkbox/checkbox-group/checkbox-group';
import { StockService } from '../../../../../services/stock/stock-service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { Stock } from '../../../../../models/stock';

@Component({
  selector: 'app-stock-controls',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    Card,
    Badge,
    Button,
    Table,
    TableCellDirective,
    TableExpandedRowDirective,
    CustomInput,
    InputDirective,
    FormatCurrencyPipe,
    Dropdown,
    Drawer,
    Slider,
    CheckboxGroup
  ],
  templateUrl: './stock-controls.html',
  styleUrl: './stock-controls.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockControls {
  stocks = input.required<Stock[]>();
  selectedExchangeId = input.required<string | null>();
  fallbackCurrency = input.required<string>();
  stockUpdated = output<Stock>();

  private readonly stockService = inject(StockService);
  private readonly dialogService = inject(DialogService);
  private readonly toastService = inject(ToastService);

  readonly isProcessing = signal(false);
  readonly searchQuery = signal<string>('');
  readonly sortBy = signal<string>('SYMBOL_ASC');
  readonly isFilterDrawerOpen = signal<boolean>(false);

  readonly appliedSectors = signal<string[]>([]);
  readonly appliedStatuses = signal<string[]>([]);
  readonly appliedMarketCapCategories = signal<string[]>([]);

  readonly draftSectors = signal<string[]>([]);
  readonly draftStatuses = signal<string[]>([]);
  readonly draftMarketCapCategories = signal<string[]>([]);

  readonly _appliedPriceRange = signal<[number, number] | null>(null);
  readonly _draftPriceRange = signal<[number, number] | null>(null);

  readonly priceRangeBounds = computed(() => {
    const data = this.stocks();
    if (data.length === 0) return { min: 0, max: 100 };
    const maxPrice = Math.max(...data.map(s => s.currentPrice));
    return {
      min: 0,
      max: (Math.ceil(maxPrice / 100) * 100) + 100
    };
  });

  readonly priceSliderStep = computed(() => Math.max(1, Math.floor(this.priceRangeBounds().max / 100)));

  readonly appliedPriceRange = computed(() => {
    const val = this._appliedPriceRange();
    return val ? val : [this.priceRangeBounds().min, this.priceRangeBounds().max] as [number, number];
  });

  readonly draftPriceRange = computed(() => {
    const val = this._draftPriceRange();
    return val ? val : [this.priceRangeBounds().min, this.priceRangeBounds().max] as [number, number];
  });

  readonly sortOptions = [
    { label: 'Symbol (A-Z)', value: 'SYMBOL_ASC' },
    { label: 'Company (A-Z)', value: 'NAME_ASC' },
    { label: 'Price (High to Low)', value: 'PRICE_DESC' },
    { label: 'Price (Low to High)', value: 'PRICE_ASC' }
  ];

  readonly marketCapOptions = [
    { label: 'Large Cap', value: 'LARGE' },
    { label: 'Mid Cap', value: 'MID' },
    { label: 'Small Cap', value: 'SMALL' }
  ];

  readonly statusOptions = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Halted', value: 'HALTED' },
    { label: 'Delisted', value: 'DELISTED' }
  ];

  readonly sectorOptions = computed(() => {
    const sectors = new Set(this.stocks().map(s => s.sector));
    return Array.from(sectors).sort().map(sector => ({
      label: sector.charAt(0) + sector.slice(1).toLowerCase().replace(/_/g, ' '),
      value: sector
    }));
  });

  readonly hasUnsavedFilters = computed(() => {
    const activeRange = this.appliedPriceRange();
    const draftRange = this.draftPriceRange();
    if (activeRange[0] !== draftRange[0] || activeRange[1] !== draftRange[1]) return true;

    const checkArraysDifference = (arr1: string[], arr2: string[]) => {
      if (arr1.length !== arr2.length) return true;
      const sorted1 = [...arr1].sort();
      const sorted2 = [...arr2].sort();
      return sorted1.some((val, i) => val !== sorted2[i]);
    };

    if (checkArraysDifference(this.appliedSectors(), this.draftSectors())) return true;
    if (checkArraysDifference(this.appliedStatuses(), this.draftStatuses())) return true;
    if (checkArraysDifference(this.appliedMarketCapCategories(), this.draftMarketCapCategories())) return true;

    return false;
  });

  readonly hasActiveFilters = computed(() => {
    const pRange = this.appliedPriceRange();
    const minPrice = Math.min(pRange[0], pRange[1]);
    const maxPrice = Math.max(pRange[0], pRange[1]);
    const bounds = this.priceRangeBounds();

    return this.appliedSectors().length > 0 ||
      this.appliedStatuses().length > 0 ||
      this.appliedMarketCapCategories().length > 0 ||
      minPrice > bounds.min ||
      maxPrice < bounds.max;
  });

  readonly filteredAndSortedStocks = computed(() => {
    let result = [...this.stocks()];

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      result = result.filter(s =>
        s.symbol.toLowerCase().includes(query) ||
        s.companyName.toLowerCase().includes(query)
      );
    }

    const pRange = this.appliedPriceRange();
    const minPrice = Math.min(pRange[0], pRange[1]);
    const maxPrice = Math.max(pRange[0], pRange[1]);
    const bounds = this.priceRangeBounds();

    if (minPrice > bounds.min || maxPrice < bounds.max) {
      result = result.filter(s => s.currentPrice >= minPrice && s.currentPrice <= maxPrice);
    }

    const sectors = this.appliedSectors();
    if (sectors.length > 0) result = result.filter(s => sectors.includes(s.sector));

    const statuses = this.appliedStatuses();
    if (statuses.length > 0) result = result.filter(s => statuses.includes(s.status));

    const marketCaps = this.appliedMarketCapCategories();
    if (marketCaps.length > 0) result = result.filter(s => marketCaps.includes(s.marketCapCategory));

    const sort = this.sortBy();
    result.sort((a, b) => {
      switch (sort) {
        case 'PRICE_DESC': return b.currentPrice - a.currentPrice;
        case 'PRICE_ASC': return a.currentPrice - b.currentPrice;
        case 'NAME_ASC': return a.companyName.localeCompare(b.companyName);
        case 'SYMBOL_ASC': return a.symbol.localeCompare(b.symbol);
        default: return 0;
      }
    });

    return result;
  });

  readonly stockColumns = signal<TableColumn<Stock>[]>([
    { key: 'symbol', header: 'Symbol', width: '100px' },
    { key: 'companyName', header: 'Company Name' },
    { key: 'sector', header: 'Sector' },
    { key: 'currentPrice', header: 'Price', align: 'right' },
    { key: 'status', header: 'Status' },
    { key: 'actions', header: '', align: 'right', width: '220px' }
  ]);

  constructor() {
    effect(() => {
      this.selectedExchangeId();
      this.resetDraftFilters();
      this._appliedPriceRange.set(null);
      this.appliedSectors.set([]);
      this.appliedStatuses.set([]);
      this.appliedMarketCapCategories.set([]);
    }, { allowSignalWrites: true });
  }

  openFilterDrawer(): void {
    this.isFilterDrawerOpen.set(true);
  }

  onPriceSliderChange(val: number | [number, number]): void {
    if (Array.isArray(val)) {
      this._draftPriceRange.set([val[0], val[1]]);
    }
  }

  resetDraftFilters(): void {
    this.draftSectors.set([]);
    this.draftStatuses.set([]);
    this.draftMarketCapCategories.set([]);
    this._draftPriceRange.set(null);
  }

  applyFilters(): void {
    this.appliedSectors.set([...this.draftSectors()]);
    this.appliedStatuses.set([...this.draftStatuses()]);
    this.appliedMarketCapCategories.set([...this.draftMarketCapCategories()]);

    const [min, max] = this.draftPriceRange();
    this._appliedPriceRange.set([Math.min(min, max), Math.max(min, max)]);

    this.isFilterDrawerOpen.set(false);
  }

  changeStockStatus(stock: Stock, newStatus: string): void {
    this.dialogService.open({
      title: `Confirm ${newStatus}`,
      message: `Are you sure you want to change the status of ${stock.symbol} to ${newStatus}?`,
      primaryLabel: 'Confirm',
      secondaryLabel: 'Cancel',
      primaryVariant: newStatus === 'DELISTED' ? 'danger' : (newStatus === 'HALTED' ? 'warning' : 'success'),
      onPrimary: () => {
        this.isProcessing.set(true);
        this.stockService.changeStockStatus(stock.id, newStatus).subscribe({
          next: (updatedStock) => {
            this.stockUpdated.emit(updatedStock);
            this.isProcessing.set(false);
            this.toastService.success(`Status updated successfully`);
          },
          error: (err: HttpErrorResponse) => {
            this.isProcessing.set(false);
            this.toastService.danger(err.error?.message ?? 'Something went wrong.');
          }
        });
      }
    });
  }
}