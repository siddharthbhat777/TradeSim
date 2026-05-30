import { inject, Injectable, signal } from '@angular/core';
import { Register } from '../../models/register';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { AuthStatus } from '../../constants/auth';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  showAuthDialog = signal({
    show: false,
    status: AuthStatus.Login
  });

  private http = inject(HttpClient);

  registerUser(formData: Register) {
    return this.http.post(environment.apiBaseURL + '/auth/register', formData)
  }
}