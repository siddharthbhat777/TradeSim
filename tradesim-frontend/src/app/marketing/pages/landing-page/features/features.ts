import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-features',
  templateUrl: './features.html',
  styleUrl: './features.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Features { }
