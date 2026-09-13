import { Component, afterNextRender, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Dialog } from './shared/components/dialog/dialog';
import { Toast } from './shared/components/toast/toast';
import { Loader } from './shared/components/loaders/loader/loader';
import { ThemeService } from './services/theme-service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Dialog, Toast, Loader],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly themeService = inject(ThemeService);
}