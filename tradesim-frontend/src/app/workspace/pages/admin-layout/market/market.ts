import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-market',
  imports: [],
  templateUrl: './market.html',
  styleUrl: './market.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Market {}
