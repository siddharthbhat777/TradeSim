import { Role } from "../constants/auth";

export interface LoginResponse {
    accessToken: string;
    username: string;
    role: Role;
}