import { Component, output } from '@angular/core';
import { Modal } from "../../../shared/components/modal/modal";

@Component({
  selector: 'app-auth',
  imports: [Modal],
  templateUrl: './auth.html',
  styleUrl: './auth.scss',
})
export class Auth {
  showAuth = output();

  closeAuth() {
    this.showAuth.emit();
  }
}