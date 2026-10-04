import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-cta',
  templateUrl: './cta.html',
  styleUrl: './cta.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Cta { }
