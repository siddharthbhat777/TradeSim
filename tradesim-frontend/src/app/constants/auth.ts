export enum AuthStatus {
    Login = "LOGIN",
    Register = "REGISTER",
    Reactivate = "REACTIVATE"
}

export enum Role {
    user = "USER",
    admin = "ADMIN",
    companyRepresentative = "COMPANY_REPRESENTATIVE"
}

export enum OtpPurpose {
    REGISTRATION = "REGISTRATION",
    FORGOT_PASSWORD = "FORGOT_PASSWORD",
    CHANGE_EMAIL = "CHANGE_EMAIL"
}