import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnInit, OnDestroy, output, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WalletService } from '../../../services/wallet/wallet-service';
import { ForexService } from '../../../services/forex/forex-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { SegmentedControl, SegmentOption } from '../../../shared/components/segmented-control/segmented-control';
import { CustomInput } from '../../../shared/components/input/input';
import { InputDirective } from '../../../shared/directives/input';
import { Dropdown, DropdownOption } from '../../../shared/components/dropdown/dropdown';
import { Button } from '../../../shared/components/button/button';
import { Alert } from '../../../shared/components/alert/alert';
import { InlineLoader } from '../../../shared/components/loaders/inline-loader/inline-loader';

export type FundManagerMode = 'deposit' | 'convert';

@Component({
  selector: 'app-fund-manager',
  imports: [
    CommonModule,
    FormsModule,
    CurrencyPipe,
    DecimalPipe,
    SegmentedControl,
    CustomInput,
    InputDirective,
    Dropdown,
    Button,
    Alert,
    InlineLoader
  ],
  templateUrl: './fund-manager.html',
  styleUrl: './fund-manager.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FundManager implements OnInit, OnDestroy {
  private readonly walletService = inject(WalletService);
  private readonly forexService = inject(ForexService);
  private readonly tradingAccountService = inject(TradingAccountService);
  private readonly toastService = inject(ToastService);

  readonly initialMode = input<FundManagerMode>('deposit');
  readonly defaultTargetCurrency = input<string | null>(null);
  readonly showHeader = input<boolean>(true);

  readonly completed = output<void>();
  readonly modeChange = output<FundManagerMode>();

  readonly activeMode = signal<FundManagerMode>('deposit');
  readonly isSubmitting = signal<boolean>(false);
  readonly isLoadingRate = signal<boolean>(false);

  readonly depositAmount = signal<number | null>(null);

  readonly sourceCurrency = signal<string>('INR');
  readonly targetCurrency = signal<string>('USD');
  readonly convertAmount = signal<number | null>(null);
  readonly currentRate = signal<number | null>(null);
  readonly supportedCurrencies = signal<string[]>([]);

  private mediaQueryList: MediaQueryList | null = null;
  readonly isMobile = signal<boolean>(false);

  readonly modeOptions = computed<SegmentOption<FundManagerMode>[]>(() => [
    { label: this.isMobile() ? 'Deposit' : 'Deposit Cash', value: 'deposit' },
    { label: this.isMobile() ? 'Convert' : 'Convert Currency', value: 'convert' }
  ]);

  readonly quickDepositAmounts = [5000, 10000, 25000, 50000, 100000];

  readonly wallet = computed(() => this.walletService.wallet());
  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');

  readonly isMultiCurrencyApproved = computed(() => {
    return this.wallet()?.multiCurrencyStatus === 'APPROVED';
  });

  readonly baseBucket = computed(() => {
    const buckets = this.wallet()?.buckets ?? [];
    return buckets.find(b => b.currency === this.baseCurrency()) ?? null;
  });

  readonly activeSourceBucket = computed(() => {
    const buckets = this.wallet()?.buckets ?? [];
    return buckets.find(b => b.currency === this.sourceCurrency()) ?? null;
  });

  readonly sourceCurrencyOptions = computed<DropdownOption<string>[]>(() => {
    const supported = this.supportedCurrencies();
    const buckets = this.wallet()?.buckets ?? [];

    return supported.map(code => {
      const bucket = buckets.find(b => b.currency === code);
      return {
        label: code,
        value: code,
        availableBalance: bucket ? bucket.availableBalance : 0
      };
    });
  });

  readonly targetCurrencyOptions = computed<DropdownOption<string>[]>(() => {
    const supported = this.supportedCurrencies();
    const currentSource = this.sourceCurrency();
    const buckets = this.wallet()?.buckets ?? [];

    return supported
      .filter(code => code !== currentSource)
      .map(code => {
        const bucket = buckets.find(b => b.currency === code);
        return {
          label: code,
          value: code,
          availableBalance: bucket ? bucket.availableBalance : 0
        };
      });
  });

  readonly amountExceedsError = computed<string>(() => {
    if (this.activeMode() !== 'convert') return '';
    const amount = this.convertAmount();
    if (amount === null || amount <= 0) return '';
    const available = this.activeSourceBucket()?.availableBalance ?? 0;
    return amount > available ? 'Amount exceeds available balance.' : '';
  });

  readonly estimatedTargetAmount = computed<number>(() => {
    const amount = this.convertAmount();
    const rate = this.currentRate();
    if (!amount || amount <= 0 || !rate) return 0;
    return amount * rate;
  });

  readonly estimatedFxFee = computed<number>(() => {
    const amount = this.convertAmount();
    if (!amount || amount <= 0) return 0;
    return amount * 0.01;
  });

  constructor() {
    effect(() => {
      this.activeMode.set(this.initialMode());
    });

    effect(() => {
      const target = this.defaultTargetCurrency();
      if (target) {
        this.targetCurrency.set(target);
      }
    });

    effect(() => {
      const source = this.sourceCurrency();
      const target = this.targetCurrency();
      if (source && target && source !== target) {
        this.fetchRate(source, target);
      }
    });
  }

  ngOnInit(): void {
    if (!this.tradingAccountService.tradingAccount()) {
      this.tradingAccountService.loadTradingAccount();
    }
    if (!this.wallet()) {
      this.walletService.loadWallet();
    }
    this.fetchSupportedCurrencies();

    if (typeof window !== 'undefined') {
      this.mediaQueryList = window.matchMedia('(max-width: 640px)');
      this.isMobile.set(this.mediaQueryList.matches);
      this.mediaQueryList.addEventListener('change', this.handleMediaQueryChange);
    }
  }

  ngOnDestroy(): void {
    if (this.mediaQueryList) {
      this.mediaQueryList.removeEventListener('change', this.handleMediaQueryChange);
    }
  }

  private handleMediaQueryChange = (e: MediaQueryListEvent): void => {
    this.isMobile.set(e.matches);
  };

  setMode(mode: FundManagerMode): void {
    this.activeMode.set(mode);
    this.modeChange.emit(mode);
  }

  addDepositPreset(amount: number): void {
    const current = this.depositAmount() ?? 0;
    this.depositAmount.set(current + amount);
  }

  setFullSourceBalance(): void {
    const available = this.activeSourceBucket()?.availableBalance ?? 0;
    this.convertAmount.set(available > 0 ? available : null);
  }

  swapCurrencies(): void {
    const prevSource = this.sourceCurrency();
    const prevTarget = this.targetCurrency();
    this.sourceCurrency.set(prevTarget);
    this.targetCurrency.set(prevSource);
  }

  private fetchSupportedCurrencies(): void {
    this.forexService.getSupportedCurrencies().subscribe({
      next: (currencies) => {
        this.supportedCurrencies.set(currencies);
        if (!currencies.includes(this.targetCurrency()) && currencies.length > 0) {
          const fallback = currencies.find(c => c !== this.sourceCurrency()) || currencies[0];
          this.targetCurrency.set(fallback);
        }
      },
      error: () => {
        this.supportedCurrencies.set(['INR', 'USD', 'EUR', 'GBP', 'JPY']);
      }
    });
  }

  private fetchRate(source: string, target: string): void {
    this.isLoadingRate.set(true);
    this.forexService.getExchangeRate(source, target).subscribe({
      next: (rate) => {
        this.currentRate.set(rate);
        this.isLoadingRate.set(false);
      },
      error: () => {
        this.currentRate.set(null);
        this.isLoadingRate.set(false);
      }
    });
  }

  submitDeposit(): void {
    const amount = this.depositAmount();
    if (!amount || amount <= 0) return;

    this.isSubmitting.set(true);
    this.walletService.deposit({ amount }).subscribe({
      next: () => {
        this.toastService.success(`Successfully deposited ${amount.toFixed(2)} ${this.baseCurrency()}`);
        this.walletService.loadWallet();
        this.depositAmount.set(null);
        this.isSubmitting.set(false);
        this.completed.emit();
      },
      error: () => {
        this.isSubmitting.set(false);
      }
    });
  }

  submitConversion(): void {
    const amount = this.convertAmount();
    const source = this.sourceCurrency();
    const target = this.targetCurrency();

    if (!amount || amount <= 0 || source === target) return;

    this.isSubmitting.set(true);
    this.walletService.convert({
      sourceCurrencyCode: source,
      targetCurrencyCode: target,
      amountToConvert: amount
    }).subscribe({
      next: () => {
        this.toastService.success(`Converted ${amount.toFixed(2)} ${source} to ${target}`);
        this.walletService.loadWallet();
        this.convertAmount.set(null);
        this.isSubmitting.set(false);
        this.completed.emit();
      },
      error: () => {
        this.isSubmitting.set(false);
      }
    });
  }
}