import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment/environment';
import { TradingAccountResponse } from '../../models/trading-account';
import { skipInterceptors } from '../../shared/utils/http-context';
import {
  BankBalanceResponse, ChangePasswordRequest,
  EditProfileRequest, UserProfile,
  VerifyEmailChangeRequest
} from '../../models/user';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly usersUrl = `${environment.apiBaseURL}/users`;

  getProfile() {
    return this.http.get<UserProfile>(`${this.usersUrl}/profile`, {
      context: skipInterceptors({ loader: true })
    });
  }

  getTradingAccount() {
    return this.http.get<TradingAccountResponse>(`${environment.apiBaseURL}/trading-account`, {
      context: skipInterceptors({ loader: true })
    });
  }

  updateProfile(request: EditProfileRequest) {
    return this.http.put<UserProfile>(`${this.usersUrl}/profile/edit`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  initiateEmailChange(newEmail: string) {
    return this.http.post<void>(`${this.usersUrl}/email/change/initiate`, { newEmail }, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  verifyEmailChange(request: VerifyEmailChangeRequest) {
    return this.http.put<UserProfile>(`${this.usersUrl}/email/change/verify`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  revealBankBalance(password: string) {
    return this.http.post<BankBalanceResponse>(`${this.usersUrl}/profile/bank-balance`, { password }, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }

  changePassword(request: ChangePasswordRequest) {
    return this.http.put<void>(`${this.usersUrl}/password/change`, request, {
      context: skipInterceptors({ loader: true, toast: true })
    });
  }
}