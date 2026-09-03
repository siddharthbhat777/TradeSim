import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-wallet',
  imports: [],
  templateUrl: './wallet.html',
  styleUrl: './wallet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Wallet {}
