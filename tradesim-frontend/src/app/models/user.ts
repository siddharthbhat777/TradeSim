export interface UserProfile {
    id: string;
    fullName: string;
    username: string;
    email: string;
    linkedBankName: string;
    role: string;
    accountStatus: string;
    themePreference: string;
    countryCode: string;
    lastLogin: string | null;
}

export interface EditProfileRequest {
    fullName: string;
    linkedBankName: string;
}

export interface VerifyEmailChangeRequest {
    newEmail: string;
    otp: string;
}

export interface ChangePasswordRequest {
    currentPassword?: string;
    newPassword?: string;
}

export interface BankBalanceResponse {
    bankBalance: number;
}

export interface SendOtpRequest {
    email: string;
    purpose: string;
}

export interface ResetPasswordRequest {
    email: string;
    otp: string;
    newPassword: string;
}