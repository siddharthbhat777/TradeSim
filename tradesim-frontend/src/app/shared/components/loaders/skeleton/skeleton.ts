import { Component, input, computed, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-skeleton',
  templateUrl: './skeleton.html',
  styleUrls: ['./skeleton.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Skeleton {
  shape = input<'text' | 'rect' | 'circle'>('text');
  width = input<string>();
  height = input<string>();
  rounded = input<string>();

  computedWidth = computed(() => {
    const w = this.width();
    if (w) return w;
    return this.shape() === 'circle' ? '48px' : '100%';
  });

  computedHeight = computed(() => {
    const h = this.height();
    if (h) return h;
    const s = this.shape();
    if (s === 'circle') return '48px';
    if (s === 'rect') return '100%';
    return '16px';
  });

  computedRounded = computed(() => {
    const r = this.rounded();
    if (r) return r;
    const s = this.shape();
    if (s === 'circle') return '50%';
    if (s === 'rect') return '12px';
    return '9999px';
  });
}