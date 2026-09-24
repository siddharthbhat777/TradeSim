import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-team',
  imports: [],
  templateUrl: './team.html',
  styleUrl: './team.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Team {}
