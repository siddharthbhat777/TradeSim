import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';

export interface PieChartData {
  id: string;
  label: string;
  value: number;
  color: string;
}

interface CalculatedSlice extends PieChartData {
  pathD: string;
  percentage: number;
}

@Component({
  selector: 'app-pie-chart',
  imports: [DecimalPipe],
  templateUrl: './pie-chart.html',
  styleUrl: './pie-chart.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PieChart {
  readonly data = input.required<PieChartData[]>();
  readonly shape = input<'full' | 'half'>('full');
  readonly chartStyle = input<'solid' | 'donut'>('donut');

  readonly size = input<number>(400);
  readonly donutThickness = input<number>(80);

  readonly activeSliceId = input<string | null>(null);
  readonly sliceHover = output<string | null>();

  readonly hoveredSlice = signal<CalculatedSlice | null>(null);
  readonly tooltipX = signal<number>(0);
  readonly tooltipY = signal<number>(0);

  readonly viewBox = computed(() => {
    const s = this.size();
    return this.shape() === 'half' ? `0 0 ${s} ${s / 2}` : `0 0 ${s} ${s}`;
  });

  readonly slices = computed<CalculatedSlice[]>(() => {
    const items = this.data();
    const total = items.reduce((sum, item) => sum + item.value, 0);

    if (total === 0) return [];

    const isHalf = this.shape() === 'half';
    const isDonut = this.chartStyle() === 'donut';

    const size = this.size();
    const cx = size / 2;
    const cy = size / 2;

    const padding = 10;
    const outerRadius = (size / 2) - padding;
    const innerRadius = isDonut ? outerRadius - this.donutThickness() : 0;

    let currentAngle = isHalf ? -90 : 0;
    const totalAngleMultiplier = isHalf ? 180 : 360;

    return items.map(item => {
      const percentage = (item.value / total);
      const sliceAngle = percentage * totalAngleMultiplier;

      const startAngle = currentAngle;
      const endAngle = currentAngle + sliceAngle;

      const pathD = isDonut
        ? this.describeAnnularSector(cx, cy, innerRadius, outerRadius, startAngle, endAngle)
        : this.describeSector(cx, cy, outerRadius, startAngle, endAngle);

      currentAngle += sliceAngle;

      return {
        ...item,
        pathD,
        percentage: percentage * 100
      };
    });
  });

  protected onMouseMove(event: MouseEvent, slice: CalculatedSlice): void {
    this.hoveredSlice.set(slice);
    this.tooltipX.set(event.clientX + 15);
    this.tooltipY.set(event.clientY + 15);
    this.sliceHover.emit(slice.id);
  }

  protected onMouseLeave(): void {
    this.hoveredSlice.set(null);
    this.sliceHover.emit(null);
  }

  private polarToCartesian(centerX: number, centerY: number, radius: number, angleInDegrees: number) {
    const angleInRadians = (angleInDegrees - 90) * Math.PI / 180.0;
    return {
      x: centerX + (radius * Math.cos(angleInRadians)),
      y: centerY + (radius * Math.sin(angleInRadians))
    };
  }

  private describeSector(cx: number, cy: number, radius: number, startAngle: number, endAngle: number): string {
    if (endAngle - startAngle >= 360) endAngle = startAngle + 359.999;

    const start = this.polarToCartesian(cx, cy, radius, endAngle);
    const end = this.polarToCartesian(cx, cy, radius, startAngle);
    const largeArcFlag = endAngle - startAngle <= 180 ? "0" : "1";

    return [
      "M", cx, cy,
      "L", start.x, start.y,
      "A", radius, radius, 0, largeArcFlag, 0, end.x, end.y,
      "Z"
    ].join(" ");
  }

  private describeAnnularSector(cx: number, cy: number, innerRadius: number, outerRadius: number, startAngle: number, endAngle: number): string {
    if (endAngle - startAngle >= 360) endAngle = startAngle + 359.999;

    const startOuter = this.polarToCartesian(cx, cy, outerRadius, endAngle);
    const endOuter = this.polarToCartesian(cx, cy, outerRadius, startAngle);
    const startInner = this.polarToCartesian(cx, cy, innerRadius, endAngle);
    const endInner = this.polarToCartesian(cx, cy, innerRadius, startAngle);
    const largeArcFlag = endAngle - startAngle <= 180 ? "0" : "1";

    return [
      "M", startOuter.x, startOuter.y,
      "A", outerRadius, outerRadius, 0, largeArcFlag, 0, endOuter.x, endOuter.y,
      "L", endInner.x, endInner.y,
      "A", innerRadius, innerRadius, 0, largeArcFlag, 1, startInner.x, startInner.y,
      "Z"
    ].join(" ");
  }
}