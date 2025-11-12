export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  tokenType: string;
}

export interface User {
  id: string;
  username: string;
  email: string;
}
