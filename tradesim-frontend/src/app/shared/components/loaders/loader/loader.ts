import { Component, input, inject, ChangeDetectionStrategy } from '@angular/core';
import { LoaderService } from './loader.service';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.html',
  styleUrl: './loader.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Loader {
  size = input<'small' | 'medium' | 'large'>('medium');
  fullScreen = input<boolean>(false);

  loaderService = inject(LoaderService);
}