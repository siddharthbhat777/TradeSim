import { ChangeDetectionStrategy, Component, computed, effect, ElementRef, HostListener, inject, input, OnDestroy, signal, viewChild } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, DOCUMENT } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SegmentedControl, SegmentOption } from '../../segmented-control/segmented-control';

export interface AreaChartData {
  time: number | string | Date;
  value: number;
}

export interface RenderedPoint {
  data: AreaChartData;
  x: number;
  y: number;
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
  selector: 'app-area-chart',
  imports: [DatePipe, CurrencyPipe, DecimalPipe, FormsModule, SegmentedControl],
  templateUrl: './area-chart.html',
  styleUrl: './area-chart.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AreaChart implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private readonly el = inject(ElementRef);
  private placeholder?: Comment;

  private readonly chartBodyRef = viewChild<ElementRef<HTMLDivElement>>('chartBody');

  readonly data = input.required<AreaChartData[]>();
  readonly width = input<number>(800);
  readonly height = input<number>(400);
  readonly currency = input<string>('INR');

  private readonly paddingTop = 20;
  private readonly paddingBottom = 30;
  private readonly paddingLeft = 40;
  private readonly paddingRight = 85;

  readonly isFullScreen = signal<boolean>(false);
  readonly currentSvgWidth = signal<number>(800);
  readonly currentSvgHeight = signal<number>(400);

  readonly hoveredPoint = signal<RenderedPoint | null>(null);
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

    let min = items[0].value;
    let max = items[0].value;

    for (const item of items) {
      if (item.value < min) min = item.value;
      if (item.value > max) max = item.value;
    }

    const paddingValue = (max - min) * 0.1;
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

    if (items.length > 1) {
      const numTicks = Math.min(items.length, this.isFullScreen() ? Math.floor(w / 100) : 6);
      for (let i = 0; i < numTicks; i++) {
        const dataIndex = Math.floor(i * (items.length - 1) / Math.max(1, numTicks - 1));
        const item = items[dataIndex];
        const xStep = drawWidth / (items.length - 1);
        const x = this.paddingLeft + (dataIndex * xStep);
        ticks.push({ label: item.time, x });
      }
    }
    return ticks;
  });

  readonly renderedPoints = computed<RenderedPoint[]>(() => {
    const items = this.effectiveData();
    const { min, range } = this.chartExtremes();
    const w = this.currentSvgWidth();
    const h = this.currentSvgHeight();

    const drawWidth = w - this.paddingLeft - this.paddingRight;
    const drawHeight = h - this.paddingTop - this.paddingBottom;

    if (items.length === 0 || range === 0) return [];

    const xStep = items.length > 1 ? drawWidth / (items.length - 1) : 0;

    return items.map((point, index) => {
      const scaleY = (val: number) => h - this.paddingBottom - ((val - min) / range) * drawHeight;
      return {
        data: point,
        x: this.paddingLeft + (index * xStep),
        y: scaleY(point.value)
      };
    });
  });

  readonly pathStrings = computed(() => {
    const points = this.renderedPoints();
    if (points.length === 0) return { line: '', area: '' };

    const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x},${p.y}`).join(' ');

    const h = this.currentSvgHeight();
    const bottomY = h - this.paddingBottom;
    const areaPath = `${linePath} L ${points[points.length - 1].x},${bottomY} L ${points[0].x},${bottomY} Z`;

    return { line: linePath, area: areaPath };
  });

  readonly hoverHitboxWidth = computed(() => {
    const items = this.effectiveData();
    if (items.length <= 1) return 0;
    const drawWidth = this.currentSvgWidth() - this.paddingLeft - this.paddingRight;
    return drawWidth / (items.length - 1);
  });

  toggleFullScreen() {
    this.isFullScreen.update(v => !v);
    this.hoveredPoint.set(null);

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

  protected onMouseMove(event: MouseEvent, point: RenderedPoint): void {
    this.hoveredPoint.set(point);

    let tX = event.clientX + 15;
    let tY = event.clientY + 15;

    if (tX + 150 > window.innerWidth) {
      tX = event.clientX - 165;
    }

    this.tooltipX.set(tX);
    this.tooltipY.set(tY);
  }

  protected onMouseLeave(): void {
    this.hoveredPoint.set(null);
  }
}