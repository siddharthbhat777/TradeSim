import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-admin-layout',
  imports: [],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminLayout {}
