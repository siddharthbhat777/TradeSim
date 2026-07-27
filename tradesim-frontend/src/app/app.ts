import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Dialog } from './shared/components/dialog/dialog';
import { Toast } from './shared/components/toast/toast';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Dialog, Toast],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
}