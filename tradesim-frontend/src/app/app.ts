import { Component, afterNextRender } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Dialog } from './shared/components/dialog/dialog';
import { Toast } from './shared/components/toast/toast';
import { Loader } from './shared/components/loaders/loader/loader';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Dialog, Toast, Loader],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {

  constructor() {
    afterNextRender(() => {
      this.applySystemTheme();
    });
  }

  private applySystemTheme(): void {
    const matchMedia = window.matchMedia('(prefers-color-scheme: dark)');

    if (matchMedia.matches) {
      document.documentElement.setAttribute('data-theme', 'dark');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }

    matchMedia.addEventListener('change', event => {
      if (event.matches) {
        document.documentElement.setAttribute('data-theme', 'dark');
      } else {
        document.documentElement.removeAttribute('data-theme');
      }
    });
  }
}