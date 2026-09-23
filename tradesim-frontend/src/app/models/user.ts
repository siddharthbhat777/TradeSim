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

export interface UserListResponse {
    id: string;
    fullName: string;
    username: string;
    email: string;
    role: string;
    accountStatus: string;
    countryCode: string;
    lastLogin: string | null;
    createdAt: string;
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

export interface UpdateThemeRequest {
    theme: string;
}

export interface InitiateEmailChangeRequest {
    newEmail: string;
}

export interface BankBalanceRequest {
    password: string;
}

export interface ChangeUserStatusRequest {
    status: string;
}

export interface ChangeUserRoleRequest {
    role: string;
}

export interface ChangeUserStatusResponse {
    id: string;
    username: string;
    email: string;
    role: string;
    accountStatus: string;
}

export interface ChangeUserRoleResponse {
    id: string;
    username: string;
    email: string;
    role: string;
    accountStatus: string;
}