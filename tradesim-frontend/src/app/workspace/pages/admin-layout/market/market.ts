import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, FormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { Card } from '../../../../shared/components/card/card';
import { Badge } from '../../../../shared/components/badge/badge';
import { Button } from '../../../../shared/components/button/button';
import { Table, TableColumn, TableCellDirective, TableExpandedRowDirective } from '../../../../shared/components/table/table';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { FormatCurrencyPipe } from '../../../../shared/pipes/format-currency-pipe';
import { PriceIndicator } from '../../../../shared/components/price-indicator/price-indicator';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { StockService } from '../../../../services/stock/stock-service';
import { MarketIndexService } from '../../../../services/market-index/market-index-service';
import { ForexService } from '../../../../services/forex/forex-service';
import { Exchange, ExchangeMarketClock } from '../../../../models/exchange';
import { Stock } from '../../../../models/stock';
import { MarketIndex, MarketIndexConstituent } from '../../../../models/market-index';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { InlineLoader } from '../../../../shared/components/loaders/inline-loader/inline-loader';
import { Drawer } from '../../../../shared/components/drawer/drawer';
import { Slider } from '../../../../shared/components/slider/slider';
import { CheckboxGroup } from '../../../../shared/components/checkbox/checkbox-group/checkbox-group';
import { Modal } from '../../../../shared/components/modal/modal';

export type MarketTab = 'Stocks' | 'Indices' | 'Forex';

@Component({
  selector: 'app-market',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    Card,
    Badge,
    Button,
    Table,
    TableCellDirective,
    TableExpandedRowDirective,
    SegmentedControl,
    EmptyState,
    CustomInput,
    InputDirective,
    FormatCurrencyPipe,
    PriceIndicator,
    Dropdown,
    InlineLoader,
    Drawer,
    Slider,
    CheckboxGroup,
    Modal
  ],
  templateUrl: './market.html',
  styleUrl: './market.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Market implements OnInit, OnDestroy {
  private readonly exchangeService = inject(ExchangeService);
  private readonly stockService = inject(StockService);
  private readonly marketIndexService = inject(MarketIndexService);
  private readonly forexService = inject(ForexService);
  private readonly dialogService = inject(DialogService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly isLoading = signal(true);
  readonly isProcessing = signal(false);

  readonly exchanges = signal<Exchange[]>([]);
  readonly selectedExchangeId = signal<string | null>(null);
  readonly clocks = signal<Map<string, ExchangeMarketClock>>(new Map());
  readonly stocks = signal<Stock[]>([]);
  readonly indices = signal<MarketIndex[]>([]);
  readonly constituents = signal<Map<string, MarketIndexConstituent[]>>(new Map());
  readonly currencies = signal<string[]>([]);

  readonly currentExchange = computed(() => {
    const id = this.selectedExchangeId();
    return this.exchanges().find(e => e.id === id) ?? null;
  });

  readonly currentExchangeClock = computed(() => {
    const id = this.selectedExchangeId();
    if (!id) return null;
    return this.clocks().get(id) ?? null;
  });

  readonly exchangeStocks = computed(() => {
    const id = this.selectedExchangeId();
    if (!id) return [];
    return this.stocks().filter(s => s.exchangeId === id);
  });

  readonly exchangeIndices = computed(() => {
    const id = this.selectedExchangeId();
    if (!id) return [];
    return this.indices().filter(i => i.exchangeId === id);
  });

  readonly tabControl = new FormControl<MarketTab>('Stocks', { nonNullable: true });

  readonly tabOptions = signal<SegmentOption<MarketTab>[]>([
    { label: 'Stock Controls', value: 'Stocks' },
    { label: 'Market Indices', value: 'Indices' },
    { label: 'Forex & Fees', value: 'Forex' }
  ]);

  readonly searchQuery = signal<string>('');
  readonly indexSearchQuery = signal<string>('');
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

  readonly exchangeOptions = computed<DropdownOption<string>[]>(() => {
    return this.exchanges().map(e => ({ label: `${e.name} (${e.code})`, value: e.id }));
  });

  readonly priceRangeBounds = computed(() => {
    const data = this.exchangeStocks();
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
    const sectors = new Set(this.exchangeStocks().map(s => s.sector));
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
    let result = [...this.exchangeStocks()];

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

  readonly indexExchangeFilter = signal<string>('ALL');

  readonly indexExchangeOptions = computed<DropdownOption<string>[]>(() => {
    const options: DropdownOption<string>[] = [{ label: 'All Exchanges', value: 'ALL' }];
    this.exchanges().forEach(e => options.push({ label: e.name, value: e.id }));
    return options;
  });

  readonly filteredIndices = computed(() => {
    let data = this.exchangeIndices();
    const query = this.indexSearchQuery().toLowerCase().trim();
    if (query) {
      data = data.filter(i =>
        i.name.toLowerCase().includes(query) ||
        i.symbol.toLowerCase().includes(query)
      );
    }
    return data;
  });

  readonly indexColumns = signal<TableColumn<MarketIndex>[]>([
    { key: 'symbol', header: 'Symbol', width: '120px' },
    { key: 'name', header: 'Index Name' },
    { key: 'baseValue', header: 'Base Value', align: 'right' },
    { key: 'currentValue', header: 'Current Value', align: 'right' },
    { key: 'actions', header: '', align: 'right', width: '150px' }
  ]);

  readonly currencyOptions = computed<DropdownOption<string>[]>(() => {
    return this.currencies().map(c => ({ label: c, value: c }));
  });

  readonly sourceCurrency = new FormControl<string>('USD', { nonNullable: true });
  readonly targetCurrency = new FormControl<string>('INR', { nonNullable: true });
  readonly amountToConvert = new FormControl<number | null>(null);
  readonly convertedRate = signal<number | null>(null);
  readonly isCalculating = signal(false);

  readonly showCreateIndexModal = signal<boolean>(false);
  readonly isCreatingIndex = signal<boolean>(false);

  readonly showAddConstituentModal = signal<boolean>(false);
  readonly isAddingConstituent = signal<boolean>(false);
  readonly activeTargetIndexId = signal<string | null>(null);

  readonly createIndexForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    symbol: ['', [Validators.required, Validators.maxLength(20)]],
    exchangeId: ['', Validators.required],
    baseValue: [1000, [Validators.required, Validators.min(1)]]
  });

  readonly addConstituentForm = this.fb.nonNullable.group({
    stockId: ['', Validators.required]
  });

  readonly availableStocksOptions = computed<DropdownOption<string>[]>(() => {
    const indexId = this.activeTargetIndexId();
    if (!indexId) return [];
    const index = this.indices().find(i => i.id === indexId);
    if (!index) return [];

    const existingConstituents = this.constituents().get(indexId) || [];
    const existingStockIds = new Set(existingConstituents.map(c => c.stockId));

    return this.stocks()
      .filter(s => s.exchangeId === index.exchangeId && !existingStockIds.has(s.id))
      .map(s => ({ label: `${s.symbol} - ${s.companyName}`, value: s.id }));
  });

  readonly exchangeOptionsOnly = computed<DropdownOption<string>[]>(() => {
    return this.exchanges().map(e => ({ label: e.name, value: e.id }));
  });

  private clockTimer: any;

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

  ngOnInit(): void {
    this.loadData();
    this.clockTimer = setInterval(() => this.tickClocks(), 1000);
  }

  ngOnDestroy(): void {
    if (this.clockTimer) {
      clearInterval(this.clockTimer);
    }
  }

  onExchangeSelect(val: string | null): void {
    if (val) {
      this.selectedExchangeId.set(val);
    } else {
      const current = this.selectedExchangeId();
      if (current) {
        this.selectedExchangeId.set(null);
        setTimeout(() => this.selectedExchangeId.set(current));
      }
    }
  }

  private loadData(): void {
    this.isLoading.set(true);
    forkJoin({
      exchanges: this.exchangeService.getExchanges(),
      stocks: this.stockService.getStocks(),
      indices: this.marketIndexService.getAllIndices(),
      currencies: this.forexService.getSupportedCurrencies()
    }).subscribe({
      next: (res) => {
        this.exchanges.set(res.exchanges);
        if (res.exchanges.length > 0 && !this.selectedExchangeId()) {
          this.selectedExchangeId.set(res.exchanges[0].id);
        }

        this.stocks.set(res.stocks);
        this.indices.set(res.indices);
        this.currencies.set(res.currencies);

        res.exchanges.forEach(ex => this.loadMarketClock(ex.id));
        res.indices.forEach(idx => this.loadConstituents(idx.id));

        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  private loadMarketClock(exchangeId: string): void {
    this.exchangeService.getMarketClock(exchangeId).subscribe({
      next: (clock) => {
        const currentMap = new Map(this.clocks());
        currentMap.set(exchangeId, clock);
        this.clocks.set(currentMap);
      }
    });
  }

  private loadConstituents(indexId: string): void {
    this.marketIndexService.getConstituents(indexId).subscribe({
      next: (data) => {
        const currentMap = new Map(this.constituents());
        currentMap.set(indexId, data);
        this.constituents.set(currentMap);
      }
    });
  }

  private tickClocks(): void {
    const currentMap = this.clocks();
    if (currentMap.size === 0) return;

    const newMap = new Map<string, ExchangeMarketClock>();

    currentMap.forEach((clock, id) => {
      if (!clock || !clock.localTime) {
        newMap.set(id, clock);
        return;
      }

      const parts = clock.localTime.split(':');
      if (parts.length === 3) {
        let h = parseInt(parts[0], 10);
        let m = parseInt(parts[1], 10);
        let s = parseInt(parts[2], 10);

        s++;
        if (s >= 60) {
          s = 0;
          m++;
        }
        if (m >= 60) {
          m = 0;
          h++;
        }
        if (h >= 24) {
          h = 0;
        }

        newMap.set(id, {
          ...clock,
          localTime: `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
        });
      } else {
        newMap.set(id, clock);
      }
    });

    this.clocks.set(newMap);
  }

  private errorMessage(error: unknown): string {
    const httpError = error as HttpErrorResponse;
    return httpError.error?.message ?? 'Something went wrong. Please try again.';
  }

  getFallbackCurrency(): string {
    return this.currentExchange()?.currency || (this.exchanges().length > 0 ? this.exchanges()[0].currency : 'USD');
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
            this.stocks.update(arr => arr.map(s => s.id === updatedStock.id ? updatedStock : s));
            this.isProcessing.set(false);
            this.toastService.success(`Status updated successfully`);
          },
          error: (err: HttpErrorResponse) => {
            this.isProcessing.set(false);
            this.toastService.danger(this.errorMessage(err));
          }
        });
      }
    });
  }

  inputError(control: AbstractControl | null): string {
    if (!control || !control.invalid || !(control.touched || control.dirty)) {
      return '';
    }
    if (control.errors?.['required']) return 'This field is required.';
    if (control.errors?.['maxlength']) return 'Exceeds maximum length.';
    if (control.errors?.['min']) return 'Value is too small.';
    return 'Invalid value.';
  }

  openCreateIndexModal(): void {
    this.createIndexForm.reset({
      name: '',
      symbol: '',
      exchangeId: this.selectedExchangeId() || (this.exchanges().length > 0 ? this.exchanges()[0].id : ''),
      baseValue: 1000
    });
    this.showCreateIndexModal.set(true);
  }

  closeCreateIndexModal(): void {
    this.showCreateIndexModal.set(false);
  }

  submitCreateIndex(): void {
    if (this.createIndexForm.invalid) {
      this.createIndexForm.markAllAsTouched();
      return;
    }
    this.isCreatingIndex.set(true);
    this.marketIndexService.createIndex(this.createIndexForm.getRawValue()).subscribe({
      next: (newIndex) => {
        this.indices.update(arr => [...arr, newIndex]);
        this.isCreatingIndex.set(false);
        this.showCreateIndexModal.set(false);
        this.toastService.success('Market index created successfully.');
      },
      error: (err: HttpErrorResponse) => {
        this.isCreatingIndex.set(false);
        this.toastService.danger(this.errorMessage(err));
      }
    });
  }

  initializeIndex(index: MarketIndex): void {
    this.dialogService.open({
      title: 'Initialize Index',
      message: `Are you sure you want to initialize the base market cap for ${index.symbol}? This will reset its base tracking metrics to today.`,
      primaryLabel: 'Initialize',
      secondaryLabel: 'Cancel',
      primaryVariant: 'primary',
      onPrimary: () => {
        this.isProcessing.set(true);
        this.marketIndexService.initializeIndex(index.id).subscribe({
          next: (updatedIndex) => {
            this.indices.update(arr => arr.map(i => i.id === updatedIndex.id ? updatedIndex : i));
            this.isProcessing.set(false);
            this.toastService.success('Index initialized successfully');
          },
          error: (err: HttpErrorResponse) => {
            this.isProcessing.set(false);
            this.toastService.danger(this.errorMessage(err));
          }
        });
      }
    });
  }

  openAddConstituentModal(index: MarketIndex): void {
    this.activeTargetIndexId.set(index.id);
    this.addConstituentForm.reset({ stockId: '' });
    this.showAddConstituentModal.set(true);
  }

  closeAddConstituentModal(): void {
    this.showAddConstituentModal.set(false);
    this.activeTargetIndexId.set(null);
  }

  submitAddConstituent(): void {
    if (this.addConstituentForm.invalid) {
      this.addConstituentForm.markAllAsTouched();
      return;
    }
    const indexId = this.activeTargetIndexId();
    if (!indexId) return;

    this.isAddingConstituent.set(true);
    const stockId = this.addConstituentForm.getRawValue().stockId;

    this.marketIndexService.addConstituent(indexId, stockId).subscribe({
      next: () => {
        this.loadConstituents(indexId);
        this.isAddingConstituent.set(false);
        this.showAddConstituentModal.set(false);
        this.toastService.success('Constituent added successfully.');
      },
      error: (err: HttpErrorResponse) => {
        this.isAddingConstituent.set(false);
        this.toastService.danger(this.errorMessage(err));
      }
    });
  }

  removeConstituent(indexId: string, stockId: string): void {
    this.dialogService.open({
      title: 'Remove Constituent',
      message: 'Are you sure you want to remove this stock from the index?',
      primaryLabel: 'Remove',
      secondaryLabel: 'Cancel',
      primaryVariant: 'danger',
      onPrimary: () => {
        this.isProcessing.set(true);
        this.marketIndexService.removeConstituent(indexId, stockId).subscribe({
          next: () => {
            this.loadConstituents(indexId);
            this.isProcessing.set(false);
            this.toastService.success('Constituent removed successfully.');
          },
          error: (err: HttpErrorResponse) => {
            this.isProcessing.set(false);
            this.toastService.danger(this.errorMessage(err));
          }
        });
      }
    });
  }

  calculateRate(): void {
    if (this.sourceCurrency.invalid || this.targetCurrency.invalid || this.amountToConvert.invalid || !this.amountToConvert.value) {
      return;
    }

    this.isCalculating.set(true);
    this.forexService.getExchangeRate(this.sourceCurrency.value, this.targetCurrency.value).subscribe({
      next: (rate) => {
        this.convertedRate.set(rate * this.amountToConvert.value!);
        this.isCalculating.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.convertedRate.set(null);
        this.isCalculating.set(false);
        this.toastService.danger(this.errorMessage(err));
      }
    });
  }

  swapCurrencies(): void {
    const temp = this.sourceCurrency.value;
    this.sourceCurrency.setValue(this.targetCurrency.value);
    this.targetCurrency.setValue(temp);
    this.convertedRate.set(null);
  }
}