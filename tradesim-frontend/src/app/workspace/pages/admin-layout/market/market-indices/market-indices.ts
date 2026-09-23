import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Card } from '../../../../../shared/components/card/card';
import { Button } from '../../../../../shared/components/button/button';
import { Table, TableColumn, TableCellDirective, TableExpandedRowDirective } from '../../../../../shared/components/table/table';
import { CustomInput } from '../../../../../shared/components/input/input';
import { InputDirective } from '../../../../../shared/directives/input';
import { FormatCurrencyPipe } from '../../../../../shared/pipes/format-currency-pipe';
import { PriceIndicator } from '../../../../../shared/components/price-indicator/price-indicator';
import { Dropdown, DropdownOption } from '../../../../../shared/components/dropdown/dropdown';
import { InlineLoader } from '../../../../../shared/components/loaders/inline-loader/inline-loader';
import { Modal } from '../../../../../shared/components/modal/modal';
import { MarketIndexService } from '../../../../../services/market-index/market-index-service';
import { DialogService } from '../../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { MarketIndex, MarketIndexConstituent } from '../../../../../models/market-index';
import { Stock } from '../../../../../models/stock';
import { Exchange } from '../../../../../models/exchange';

@Component({
  selector: 'app-market-indices',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    Card,
    Button,
    Table,
    TableCellDirective,
    TableExpandedRowDirective,
    CustomInput,
    InputDirective,
    FormatCurrencyPipe,
    PriceIndicator,
    Dropdown,
    InlineLoader,
    Modal
  ],
  templateUrl: './market-indices.html',
  styleUrl: './market-indices.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MarketIndices {
  indices = input.required<MarketIndex[]>();
  stocks = input.required<Stock[]>();
  exchanges = input.required<Exchange[]>();
  selectedExchangeId = input.required<string | null>();
  fallbackCurrency = input.required<string>();
  constituents = input.required<Map<string, MarketIndexConstituent[]>>();

  indexCreated = output<MarketIndex>();
  indexInitialized = output<MarketIndex>();
  reloadConstituents = output<string>();

  private readonly fb = inject(FormBuilder);
  private readonly marketIndexService = inject(MarketIndexService);
  private readonly dialogService = inject(DialogService);
  private readonly toastService = inject(ToastService);

  readonly isProcessing = signal(false);
  readonly indexSearchQuery = signal<string>('');

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

  readonly filteredIndices = computed(() => {
    let data = this.indices();
    const query = this.indexSearchQuery().toLowerCase().trim();
    if (query) {
      data = data.filter(i =>
        i.name.toLowerCase().includes(query) ||
        i.symbol.toLowerCase().includes(query)
      );
    }
    return data;
  });

  readonly exchangeOptionsOnly = computed<DropdownOption<string>[]>(() => {
    return this.exchanges().map(e => ({ label: e.name, value: e.id }));
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

  readonly indexColumns = signal<TableColumn<MarketIndex>[]>([
    { key: 'symbol', header: 'Symbol', width: '120px' },
    { key: 'name', header: 'Index Name' },
    { key: 'baseValue', header: 'Base Value', align: 'right' },
    { key: 'currentValue', header: 'Current Value', align: 'right' },
    { key: 'actions', header: '', align: 'right', width: '150px' }
  ]);

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
        this.indexCreated.emit(newIndex);
        this.isCreatingIndex.set(false);
        this.showCreateIndexModal.set(false);
        this.toastService.success('Market index created successfully.');
      },
      error: (err: HttpErrorResponse) => {
        this.isCreatingIndex.set(false);
        this.toastService.danger(err.error?.message ?? 'Something went wrong.');
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
            this.indexInitialized.emit(updatedIndex);
            this.isProcessing.set(false);
            this.toastService.success('Index initialized successfully');
          },
          error: (err: HttpErrorResponse) => {
            this.isProcessing.set(false);
            this.toastService.danger(err.error?.message ?? 'Something went wrong.');
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
        this.reloadConstituents.emit(indexId);
        this.isAddingConstituent.set(false);
        this.showAddConstituentModal.set(false);
        this.toastService.success('Constituent added successfully.');
      },
      error: (err: HttpErrorResponse) => {
        this.isAddingConstituent.set(false);
        this.toastService.danger(err.error?.message ?? 'Something went wrong.');
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
            this.reloadConstituents.emit(indexId);
            this.isProcessing.set(false);
            this.toastService.success('Constituent removed successfully.');
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