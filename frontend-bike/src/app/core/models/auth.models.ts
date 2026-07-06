export interface AppUser {
  id: string;
  name: string;
  email: string;
  role?: string;
  emailVerified?: boolean;
}

export interface AuthSession {
  token: string;
  user: AppUser;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
  phone: string;
  address: string;
}

export interface TokenResponse {
  token: string;
  user: AppUser;
  mfaRequired: boolean;
}
