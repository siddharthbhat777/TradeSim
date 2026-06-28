import { computed, inject, Injectable, signal } from '@angular/core';
import { RegisterRequest } from '../../models/register-request';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';
import { AuthStatus } from '../../constants/auth';
import { AuthUser } from '../../models/auth-user';
import { LoginRequest } from '../../models/login-request';
import { LoginResponse } from '../../models/login-respons';
import { catchError, finalize, tap, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  showAuthDialog = signal({
    show: false,
    status: AuthStatus.Login
  });

  private readonly accessToken = signal<string | null>(null);
  private readonly user = signal<AuthUser | null>(null);

  readonly currentUser = this.user.asReadonly();
  readonly isLoggedIn = computed(() => this.accessToken() !== null);

  private http = inject(HttpClient);
  private readonly authUrl = `${environment.apiBaseURL}/auth`;

  registerUser(formData: RegisterRequest) {
    return this.http.post(`${this.authUrl}/register`, formData)
  }

  loginUser(formData: LoginRequest) {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, formData, {
      withCredentials: true
    }).pipe(
      tap((response) => this.setSession(response))
    );
  }

  refreshSession() {
    return this.http.post<LoginResponse>(`${this.authUrl}/refresh`, {}, {
      withCredentials: true
    }).pipe(
      tap((response) => this.setSession(response)),
      catchError((error) => {
        this.clearSession();
        return throwError(() => error);
      })
    );
  }

  logout() {
    return this.http.post<void>(`${this.authUrl}/logout`, {}, {
      withCredentials: true
    }).pipe(
      finalize(() => this.clearSession())
    );
  }

  reactivateAccount(formData: LoginRequest) {
    return this.http.post<LoginResponse>(`${this.authUrl}/reactivate`, formData, {
      withCredentials: true
    }).pipe(
      tap((response) => this.setSession(response))
    );
  }

  getAccessToken() {
    return this.accessToken();
  }

  clearSession() {
    this.accessToken.set(null);
    this.user.set(null);
  }

  private setSession(response: LoginResponse) {
    this.accessToken.set(response.accessToken);
    this.user.set({
      username: response.username,
      role: response.role
    });
  }
}