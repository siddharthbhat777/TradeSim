import { Role } from "../constants/auth";

export interface AuthUser {
    username: string;
    role: Role;
}