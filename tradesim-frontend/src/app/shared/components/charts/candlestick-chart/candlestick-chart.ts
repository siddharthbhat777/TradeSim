import { ChangeDetectionStrategy, Component, computed, effect, ElementRef, HostListener, inject, input, OnDestroy, signal, viewChild } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, DOCUMENT } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SegmentedControl, SegmentOption } from '../../segmented-control/segmented-control';

export interface CandlestickData {
  time: number | string | Date;
  open: number;
  high: number;
  low: number;
  close: number;
}

export interface RenderedCandle {
  data: CandlestickData;
  x: number;
  yHigh: number;
  yLow: number;
  topWickY2: number;
  bottomWickY1: number;
  bodyY: number;
  bodyHeight: number;
  isBullish: boolean;
}

export interface AxisTick {
  value: number;
  y: number;
}

export interface XAxisTick {
  label: number | string | Date;
  x: number;
}

@Component({
  selector: 'app-candlestick-chart',
  imports: [DatePipe, CurrencyPipe, DecimalPipe, FormsModule, SegmentedControl],
  templateUrl: './candlestick-chart.html',
  styleUrl: './candlestick-chart.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CandlestickChart implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private readonly el = inject(ElementRef);
  private placeholder?: Comment;

  private readonly chartBodyRef = viewChild<ElementRef<HTMLDivElement>>('chartBody');

  readonly data = input.required<CandlestickData[]>();
  readonly width = input<number>(800);
  readonly height = input<number>(400);
  readonly candleWidth = input<number>(8);
  readonly currency = input<string>('INR');

  private readonly paddingTop = 20;
  private readonly paddingBottom = 30;
  private readonly paddingLeft = 15;
  private readonly paddingRight = 60;

  readonly isFullScreen = signal<boolean>(false);
  readonly currentSvgWidth = signal<number>(800);
  readonly currentSvgHeight = signal<number>(400);

  readonly hoveredCandle = signal<RenderedCandle | null>(null);
  readonly tooltipX = signal<number>(0);
  readonly tooltipY = signal<number>(0);

  readonly timeframeOptions: SegmentOption<number>[] = [
    { label: '1W', value: 7 },
    { label: '1M', value: 30 },
    { label: '3M', value: 90 },
    { label: '6M', value: 180 },
    { label: 'ALL', value: 365 }
  ];

  readonly selectedTimeframe = signal<number>(30);

  constructor() {
    effect((onCleanup) => {
      const bodyEl = this.chartBodyRef()?.nativeElement;
      if (!bodyEl) return;

      const ro = new ResizeObserver(entries => {
        const { width, height } = entries[0].contentRect;
        if (width > 0 && height > 0) {
          this.currentSvgWidth.set(width);
          this.currentSvgHeight.set(height);
        }
      });

      ro.observe(bodyEl);
      onCleanup(() => ro.disconnect());
    });
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.isFullScreen()) {
      this.toggleFullScreen();
    }
  }

  ngOnDestroy() {
    this.document.body.style.overflow = '';
    if (this.placeholder && this.placeholder.parentNode) {
      this.placeholder.parentNode.insertBefore(this.el.nativeElement, this.placeholder);
      this.placeholder.remove();
    }
  }

  readonly effectiveData = computed(() => {
    const allData = this.data();
    const tf = this.selectedTimeframe();
    return allData.slice(-tf);
  });

  readonly viewBox = computed(() => `0 0 ${this.currentSvgWidth()} ${this.currentSvgHeight()}`);

  readonly chartExtremes = computed(() => {
    const items = this.effectiveData();
    if (items.length === 0) return { min: 0, max: 0, range: 0 };

    let min = items[0].low;
    let max = items[0].high;

    for (const item of items) {
      if (item.low < min) min = item.low;
      if (item.high > max) max = item.high;
    }

    const paddingValue = (max - min) * 0.05;
    min -= paddingValue;
    max += paddingValue;

    return { min, max, range: max - min };
  });

  readonly yAxisTicks = computed<AxisTick[]>(() => {
    const { min, max, range } = this.chartExtremes();
    const ticks: AxisTick[] = [];
    const steps = 6;
    const h = this.currentSvgHeight();
    const drawHeight = h - this.paddingTop - this.paddingBottom;

    if (range > 0) {
      for (let i = 0; i < steps; i++) {
        const value = min + (range * i) / (steps - 1);
        const y = h - this.paddingBottom - ((value - min) / range) * drawHeight;
        ticks.push({ value, y });
      }
    }
    return ticks;
  });

  readonly xAxisTicks = computed<XAxisTick[]>(() => {
    const items = this.effectiveData();
    const ticks: XAxisTick[] = [];
    const w = this.currentSvgWidth();
    const drawWidth = w - this.paddingLeft - this.paddingRight;

    if (items.length > 0) {
      const numTicks = Math.min(items.length, this.isFullScreen() ? Math.floor(w / 100) : 6);
      for (let i = 0; i < numTicks; i++) {
        const dataIndex = Math.floor(i * (items.length - 1) / Math.max(1, numTicks - 1));
        const item = items[dataIndex];
        const xStep = drawWidth / items.length;
        const x = this.paddingLeft + (dataIndex * xStep) + (xStep / 2);
        ticks.push({ label: item.time, x });
      }
    }
    return ticks;
  });

  readonly renderedCandles = computed<RenderedCandle[]>(() => {
    const items = this.effectiveData();
    const { min, range } = this.chartExtremes();
    const w = this.currentSvgWidth();
    const h = this.currentSvgHeight();

    const drawWidth = w - this.paddingLeft - this.paddingRight;
    const drawHeight = h - this.paddingTop - this.paddingBottom;

    if (items.length === 0 || range === 0) return [];

    const xStep = drawWidth / items.length;
    const dynamicCandleWidth = Math.min(this.candleWidth(), (xStep * 0.6));

    return items.map((candle, index) => {
      const isBullish = candle.close >= candle.open;
      const scaleY = (value: number) => h - this.paddingBottom - ((value - min) / range) * drawHeight;

      const yHigh = scaleY(candle.high);
      const yLow = scaleY(candle.low);
      const yOpen = scaleY(candle.open);
      const yClose = scaleY(candle.close);

      const bodyY = Math.min(yOpen, yClose);
      let bodyHeight = Math.abs(yOpen - yClose);

      if (bodyHeight < 1) bodyHeight = 1;

      return {
        data: candle,
        x: this.paddingLeft + (index * xStep) + (xStep / 2),
        yHigh,
        topWickY2: bodyY,
        bottomWickY1: bodyY + bodyHeight,
        yLow,
        bodyY,
        bodyHeight,
        isBullish
      };
    });
  });

  toggleFullScreen() {
    this.isFullScreen.update(v => !v);
    this.hoveredCandle.set(null);

    if (this.isFullScreen()) {
      this.document.body.style.overflow = 'hidden';
      this.placeholder = this.document.createComment('chart-fs-placeholder');
      this.el.nativeElement.parentNode?.insertBefore(this.placeholder, this.el.nativeElement);
      this.document.body.appendChild(this.el.nativeElement);
    } else {
      this.document.body.style.overflow = '';
      if (this.placeholder && this.placeholder.parentNode) {
        this.placeholder.parentNode.insertBefore(this.el.nativeElement, this.placeholder);
        this.placeholder.remove();
        this.placeholder = undefined;
      }
    }
  }

  protected onMouseMove(event: MouseEvent, candle: RenderedCandle): void {
    this.hoveredCandle.set(candle);

    let tX = event.clientX + 15;
    let tY = event.clientY + 15;

    if (tX + 180 > window.innerWidth) {
      tX = event.clientX - 195;
    }

    this.tooltipX.set(tX);
    this.tooltipY.set(tY);
  }

  protected onMouseLeave(): void {
    this.hoveredCandle.set(null);
  }
}