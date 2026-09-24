import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-listing',
  imports: [],
  templateUrl: './listing.html',
  styleUrl: './listing.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Listing {}
