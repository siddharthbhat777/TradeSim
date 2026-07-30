import { Component, input, inject } from '@angular/core';
import { LoaderService } from './loader.service';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.html',
  styleUrl: './loader.scss'
})
export class Loader {
  size = input<'small' | 'medium' | 'large'>('medium');
  fullScreen = input<boolean>(false);

  loaderService = inject(LoaderService);
}