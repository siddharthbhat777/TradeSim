import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-cta',
  imports: [RouterLink],
  templateUrl: './cta.html',
  styleUrl: './cta.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Cta {
  isLoggedIn = input.required<boolean>();
  openAuth = output<void>();
}