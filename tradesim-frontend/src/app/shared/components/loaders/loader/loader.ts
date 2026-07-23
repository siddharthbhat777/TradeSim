import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.html',
  styleUrl: './loader.scss'
})
export class Loader {
  size = input<'small' | 'medium' | 'large'>('medium');
  fullScreen = input<boolean>(false);
}