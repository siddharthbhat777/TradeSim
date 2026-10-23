import { ChangeDetectionStrategy, Component, OnInit, signal, ViewChildren, QueryList, ElementRef, HostListener, AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-mechanics',
  templateUrl: './mechanics.html',
  styleUrl: './mechanics.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Mechanics implements OnInit, AfterViewInit {
  @ViewChildren('stepItem') stepElements!: QueryList<ElementRef>;

  formattedAsk1 = signal<string>('');
  formattedAsk2 = signal<string>('');
  formattedBid1 = signal<string>('');
  formattedBid2 = signal<string>('');
  formattedProceeds = signal<string>('');
  formattedLoan = signal<string>('');
  formattedFee = signal<string>('');

  activeStep = signal<number>(0);

  ngOnInit(): void {
    this.formattedAsk1.set(this.formatCurrency(142.50));
    this.formattedAsk2.set(this.formatCurrency(142.45));
    this.formattedBid1.set(this.formatCurrency(142.30));
    this.formattedBid2.set(this.formatCurrency(142.25));

    this.formattedProceeds.set(this.formatCurrency(4250.00, true));
    this.formattedLoan.set(this.formatCurrency(-1120.00));
    this.formattedFee.set(this.formatCurrency(-12.50));
  }

  ngAfterViewInit(): void {
    this.checkScroll();
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.checkScroll();
  }

  private checkScroll(): void {
    if (typeof window === 'undefined' || !this.stepElements) return;

    const elements = this.stepElements.toArray();
    let current = 0;
    const triggerPoint = window.innerHeight * 0.65;

    for (let i = 0; i < elements.length; i++) {
      const rect = elements[i].nativeElement.getBoundingClientRect();
      if (rect.top <= triggerPoint) {
        current = i;
      }
    }

    if (this.activeStep() !== current) {
      this.activeStep.set(current);
    }
  }

  private getBrowserCurrency(): string {
    if (typeof window === 'undefined') {
      return 'INR';
    }

    try {
      const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
      if (timeZone === 'Asia/Calcutta' || timeZone === 'Asia/Kolkata') {
        return 'INR';
      }
    } catch (e) { }

    const languages = navigator.languages || [navigator.language || 'en-IN'];

    const currencyMap: Record<string, string> = {
      'IN': 'INR', 'US': 'USD', 'GB': 'GBP', 'DE': 'EUR', 'FR': 'EUR',
      'IT': 'EUR', 'ES': 'EUR', 'NL': 'EUR', 'AU': 'AUD', 'CA': 'CAD',
      'JP': 'JPY', 'CN': 'CNY', 'CH': 'CHF', 'SG': 'SGD', 'NZ': 'NZD',
      'AE': 'AED', 'SA': 'SAR', 'ZA': 'ZAR', 'BR': 'BRL', 'MX': 'MXN',
      'RU': 'RUB', 'KR': 'KRW', 'SE': 'SEK', 'NO': 'NOK', 'DK': 'DKK',
      'HK': 'HKD', 'TR': 'TRY', 'ID': 'IDR', 'MY': 'MYR'
    };

    for (const locale of languages) {
      const parts = locale.split('-');
      if (parts.length > 1) {
        const countryCode = parts[parts.length - 1].toUpperCase();
        if (currencyMap[countryCode]) {
          return currencyMap[countryCode];
        }
      }
    }

    return 'INR';
  }

  private formatCurrency(value: number, showPlus: boolean = false): string {
    const locale = typeof window !== 'undefined' ? navigator.language : 'en-IN';
    const currency = this.getBrowserCurrency();

    const formatted = new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency
    }).format(Math.abs(value));

    if (value < 0) {
      return `-${formatted}`;
    }
    if (value > 0 && showPlus) {
      return `+${formatted}`;
    }
    return formatted;
  }
}