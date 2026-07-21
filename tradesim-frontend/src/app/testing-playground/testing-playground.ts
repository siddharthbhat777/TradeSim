import { Component, inject, signal, viewChild } from '@angular/core';
import { Button } from '../shared/components/button/button';
import { Badge } from '../shared/components/badge/badge';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomInput, InputErrorMessages } from '../shared/components/input/input';
import { InputDirective } from '../shared/directives/input';
import { CardBorder, CardComponent, CardVariant } from '../shared/components/card/card';
import { Dialog } from '../shared/components/dialog/dialog';
import { PriceIndicator } from '../shared/components/price-indicator/price-indicator';
import { EmptyState } from '../shared/components/empty-state/empty-state';
import { Router } from '@angular/router';
import { Toggle } from '../shared/components/toggle/toggle';
import { Dropdown, DropdownOption } from '../shared/components/dropdown/dropdown';
import { Tooltip } from '../shared/components/tooltip/tooltip';
import { ToastService } from '../shared/components/toast/toast.service';
import { Toast } from '../shared/components/toast/toast';
import { Pagination } from '../shared/components/pagination/pagination';
import { Table, TableColumn } from '../shared/components/table/table';
import { DecimalPipe, JsonPipe, CommonModule } from '@angular/common';
import { SegmentedControl, SegmentOption } from '../shared/components/segmented-control/segmented-control';
import { Alert } from '../shared/components/alert/alert';
import { toSignal } from '@angular/core/rxjs-interop';
import { PieChartContainer } from '../shared/components/charts/pie-chart-container/pie-chart-container';
import { PieChart, PieChartData } from '../shared/components/charts/pie-chart-container/pie-chart/pie-chart';
import { Legend } from '../shared/components/charts/legend/legend';
import { NumberStepper } from '../shared/components/number-stepper/number-stepper';
import { Checkbox } from '../shared/components/checkbox/checkbox';
import { CheckboxGroup } from '../shared/components/checkbox/checkbox-group/checkbox-group';

interface PortfolioRow {
  id: number;
  stock: string;
  quantity: number;
  avgPrice: number;
  pnl: number;
}

@Component({
  selector: 'app-testing-playground',
  standalone: true,
  imports: [
    CommonModule, Button, Badge, FormsModule, ReactiveFormsModule, InputDirective, CustomInput,
    CardComponent, Dialog, PriceIndicator, EmptyState, Toggle, Dropdown, Tooltip,
    Toast, Pagination, Table, DecimalPipe, JsonPipe, SegmentedControl, Alert, PieChartContainer,
    PieChart, Legend, NumberStepper, Checkbox, CheckboxGroup
  ],
  templateUrl: './testing-playground.html',
  styleUrl: './testing-playground.scss'
})
export class TestingPlayground {
  readonly router = inject(Router);

  readonly inputDemoForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
    search: new FormControl('', {
      nonNullable: true
    }),
    notes: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(10)]
    }),
    username: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5)]
    })
  });

  protected readonly usernameErrorMessages: InputErrorMessages = {
    required: 'Pick a username first',
    minlength: (error) => {
      const err = error as { requiredLength: number; actualLength: number };
      return `Needs ${err.requiredLength - err.actualLength} more character(s)`;
    }
  };

  readonly alwaysValidateControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required]
  });

  protected submitInputDemo(): void {
    this.inputDemoForm.markAllAsTouched();
  }

  protected readonly variants: CardVariant[] = [
    'default', 'surface', 'outline', 'accent', 'success', 'danger', 'warning'
  ];

  protected readonly borders: CardBorder[] = [
    'primary', 'secondary', 'accent', 'success', 'danger', 'warning'
  ];

  protected readonly clickCount = signal(0);

  protected readonly handleCardClick = () => {
    this.clickCount.update((count) => count + 1);
  };

  protected readonly isBasicDialogOpen = signal(false);
  protected readonly isBodyOnlyDialogOpen = signal(false);
  protected readonly isHeaderOnlyDialogOpen = signal(false);
  protected readonly isForcedDialogOpen = signal(false);
  protected readonly isCustomContentDialogOpen = signal(false);

  readonly darkModeControl = new FormControl(false, { nonNullable: true });

  readonly settingsForm = new FormGroup({
    emailAlerts: new FormControl(true, { nonNullable: true }),
    smsAlerts: new FormControl(false, { nonNullable: true }),
    autoInvest: new FormControl(false, { nonNullable: true })
  });

  readonly lockedFeatureControl = new FormControl({ value: true, disabled: true });

  readonly riskModeControl = new FormControl(false, { nonNullable: true });
  riskModeLocked = false;

  toggleLock(): void {
    this.riskModeLocked = !this.riskModeLocked;
    if (this.riskModeLocked) {
      this.riskModeControl.disable();
    } else {
      this.riskModeControl.enable();
    }
  }

  get settingsSummary(): string {
    const v = this.settingsForm.value;
    return `emailAlerts: ${v.emailAlerts}, smsAlerts: ${v.smsAlerts}, autoInvest: ${v.autoInvest}`;
  }

  readonly orderTypeControl = new FormControl<string | null>(null);
  readonly orderTypeOptions: DropdownOption<string>[] = [
    { label: 'Market', value: 'market' },
    { label: 'Limit', value: 'limit' },
    { label: 'Stop', value: 'stop', disabled: true }
  ];

  readonly sortByControl = new FormControl<string | null>(null);
  readonly sortByOptions: DropdownOption<string>[] = [
    { label: 'Price', value: 'price' },
    { label: 'Name', value: 'name' },
    { label: 'Change %', value: 'change' }
  ];

  readonly tradePrefsForm = new FormGroup({
    exchange: new FormControl<string | null>(null, { validators: [Validators.required] }),
  });
  readonly exchangeOptions: DropdownOption<string>[] = [
    { label: 'NSE', value: 'nse' },
    { label: 'BSE', value: 'bse' },
    { label: 'NASDAQ', value: 'nasdaq' }
  ];

  protected submitTradePrefs(): void {
    this.tradePrefsForm.markAllAsTouched();
  }

  readonly currencyControl = new FormControl<string | null>(null);
  readonly currencyOptions: DropdownOption<string>[] = [
    { label: 'Indian Rupee', value: 'inr', icon: '🇮🇳' },
    { label: 'US Dollar', value: 'usd', icon: '🇺🇸' },
    { label: 'Euro', value: 'eur', icon: '🇪🇺' }
  ];

  readonly lockedExchangeControl = new FormControl<string | null>({ value: 'nse', disabled: true });

  readonly runtimeLockControl = new FormControl<string | null>(null);
  runtimeLockControlLocked = false;
  toggleDropdownLock(): void {
    this.runtimeLockControlLocked = !this.runtimeLockControlLocked;
    this.runtimeLockControlLocked ? this.runtimeLockControl.disable() : this.runtimeLockControl.enable();
  }

  readonly watchlistCategoryControl = new FormControl<string | null>(null, { validators: [Validators.required] });
  readonly watchlistCategoryOptions: DropdownOption<string>[] = [
    { label: 'Technology', value: 'tech' },
    { label: 'Banking', value: 'banking' },
    { label: 'Energy', value: 'energy' },
    { label: 'Pharma', value: 'pharma' }
  ];

  readonly pageSizeControl = new FormControl<string | null>(null);
  readonly pageSizeOptions: DropdownOption<string>[] = [
    { label: '10 / page', value: '10' },
    { label: '25 / page', value: '25' },
    { label: '50 / page', value: '50' },
    { label: '100 / page', value: '100' }
  ];

  readonly stockControl = new FormControl<string | null>(null);
  readonly stockOptions: DropdownOption<string>[] = [
    { label: 'Reliance Industries', value: 'reliance' },
    { label: 'Tata Consultancy Services', value: 'tcs' },
    { label: 'HDFC Bank', value: 'hdfcbank' },
    { label: 'Infosys', value: 'infy' },
    { label: 'ICICI Bank', value: 'icicibank' },
    { label: 'H हिंदुस्तान Unilever', value: 'hul' },
    { label: 'ITC', value: 'itc' },
    { label: 'State Bank of India', value: 'sbin' },
    { label: 'Bharti Airtel', value: 'bhartiartl' },
    { label: 'Bajaj Finance', value: 'bajfinance' },
    { label: 'Kotak Mahindra Bank', value: 'kotakbank' },
    { label: 'Larsen & Toubro', value: 'lt' },
    { label: 'Axis Bank', value: 'axisbank' },
    { label: 'Maruti Suzuki', value: 'maruti' },
    { label: 'Wipro', value: 'wipro' },
  ];

  readonly toastService = inject(ToastService);

  protected fireStackTest(): void {
    for (let i = 1; i <= 6; i++) {
      this.toastService.info(`Queued toast #${i}`);
    }
  }

  protected fireUndoDemo(): void {
    this.toastService.danger('Manager access revoked', {
      action: {
        label: 'Undo',
        onClick: () => {
          console.log('Re-adding manager to the managers table...');
          this.toastService.success('Manager access restored');
        }
      }
    });
  }

  protected fireReorderDemo(): void {
    this.toastService.info('Order cancelled', {
      action: {
        label: 'Reorder',
        onClick: () => {
          console.log('Opening prefilled order form for the cancelled order...');
          this.toastService.success('Reorder form opened');
        }
      }
    });
  }

  protected fireToastPosition(): void {
    this.toastService.info('Toast position bottom', {
      position: 'bottom-center'
    });
  }

  protected demoBasicTotalItems = signal(145);
  protected demoBasicCurrentPage = signal(1);
  protected demoBasicPageSize = signal(10);

  protected demoSinglePageTotalItems = signal(8);
  protected demoSinglePageCurrentPage = signal(1);
  protected demoSinglePageSize = signal(10);

  protected demoCustomTotalItems = signal(63);
  protected demoCustomCurrentPage = signal(1);
  protected demoCustomPageSize = signal(5);

  protected demoLargeTotalItems = signal(999);
  protected demoLargeCurrentPage = signal(1);
  protected demoLargePageSize = signal(10);

  protected demoRecoveryTotalItems = signal(97);
  protected demoRecoveryCurrentPage = signal(1);
  protected demoRecoveryPageSize = signal(10);

  protected goToHighRecoveryPage(): void {
    this.demoRecoveryCurrentPage.set(9);
  }

  protected shrinkRecoveryData(): void {
    this.demoRecoveryTotalItems.set(12);
  }

  protected resetRecoveryData(): void {
    this.demoRecoveryTotalItems.set(97);
  }

  protected readonly portfolioColumns: TableColumn<PortfolioRow>[] = [
    { key: 'stock', header: 'Stock' },
    { key: 'quantity', header: 'Quantity', align: 'right' },
    { key: 'avgPrice', header: 'Avg Price', align: 'right', accessor: (row) => `₹${row.avgPrice.toLocaleString()}` },
    { key: 'pnl', header: 'P&L', align: 'right' }
  ];

  protected readonly portfolioData: PortfolioRow[] = [
    { id: 1, stock: 'RELIANCE', quantity: 150, avgPrice: 2500, pnl: 12500 },
    { id: 2, stock: 'TCS', quantity: 75, avgPrice: 3850, pnl: -4200 },
    { id: 3, stock: 'INFY', quantity: 40, avgPrice: 1500, pnl: 800 },
    { id: 4, stock: 'HDFC', quantity: 20, avgPrice: 1650, pnl: -300 },
    { id: 5, stock: 'WIPRO', quantity: 100, avgPrice: 450, pnl: 2100 },
  ];

  protected readonly portfolioRowKey = (row: PortfolioRow): number => row.id;

  protected readonly planOptions: SegmentOption<string>[] = [
    { label: 'Monthly', value: 'monthly' },
    { label: 'Yearly', value: 'yearly' },
  ];

  protected readonly viewOptions: SegmentOption<string>[] = [
    { label: 'List', value: 'list' },
    { label: 'Board', value: 'board' },
    { label: 'Timeline', value: 'timeline', disabled: true },
  ];

  protected readonly planControl = new FormControl<string | null>(null);
  protected readonly plan = toSignal(this.planControl.valueChanges, {
    initialValue: this.planControl.value,
  });

  protected readonly viewControl = new FormControl<string>('list', { nonNullable: true });
  protected readonly view = toSignal(this.viewControl.valueChanges, {
    initialValue: this.viewControl.value,
  });

  private readonly planComponent = viewChild<SegmentedControl<string>>('planComponent');

  protected submitted = false;

  protected onSubmit(): void {
    this.submitted = true;
    this.planComponent()?.markAsTouched();
  }

  protected readonly pieChartSampleData: PieChartData[] = [
    { id: '1', label: 'Smartphones', value: 850, color: '#2563eb' },
    { id: '2', label: 'Laptops', value: 420, color: '#16a34a' },
    { id: '3', label: 'Tablets', value: 210, color: '#ea580c' },
    { id: '4', label: 'Wearables', value: 120, color: '#9333ea' }
  ];

  protected readonly pieChartPortfolioData: PieChartData[] = [
    { id: '1', label: 'Equities', value: 650000, color: '#059669' },
    { id: '2', label: 'Mutual Funds', value: 350000, color: '#0284c7' },
    { id: '3', label: 'Gold (SGB)', value: 150000, color: '#d97706' },
    { id: '4', label: 'Cash Balance', value: 100000, color: '#475569' }
  ];

  protected readonly stepperSmall = signal(0);
  protected readonly stepperMedium = signal(10);
  protected readonly stepperLarge = signal(100);

  protected readonly stepperPrimary = signal(1);
  protected readonly stepperSecondary = signal(2);
  protected readonly stepperSuccess = signal(3);
  protected readonly stepperDanger = signal(4);
  protected readonly stepperWarning = signal(5);
  protected readonly stepperOutline = signal(6);

  protected readonly stepperMinMax = signal(5);
  protected readonly stepperMixed = signal(0);

  protected standaloneBasic = signal(false);
  protected standaloneLeft = signal(false);

  protected standaloneRequiredChecked = signal(false);
  protected standaloneNoLabel = signal(false);
  private readonly reqCheckbox = viewChild<Checkbox>('reqCheckbox');

  protected submitCheckboxForm(): void {
    this.reqCheckbox()?.markAsTouched();
  }

  protected readonly simpleGroupOptions = [
    { value: 'aapl', label: 'Apple Inc.' },
    { value: 'msft', label: 'Microsoft Corp.' },
    { value: 'googl', label: 'Alphabet Inc.' }
  ];
  protected selectedSimpleGroup = signal(['aapl']);

  protected readonly sectorGroupOptions = [
    { id: 'tech', name: 'Technology', count: 145, color: '#2563eb', icon: '💻' },
    { id: 'fin', name: 'Finance', count: 89, color: '#16a34a', icon: '🏦' },
    { id: 'health', name: 'Healthcare', count: 112, color: '#ea580c', icon: '🏥' },
    { id: 'energy', name: 'Energy', count: 45, color: '#9333ea', icon: '⚡' }
  ];
  protected selectedSectorGroup = signal(['tech', 'fin']);
}