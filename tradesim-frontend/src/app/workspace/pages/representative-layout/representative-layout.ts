import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-representative-layout',
  imports: [RouterOutlet],
  templateUrl: './representative-layout.html',
  styleUrl: './representative-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RepresentativeLayout { }
