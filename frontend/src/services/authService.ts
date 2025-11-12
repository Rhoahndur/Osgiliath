import apiClient from './apiClient';
import { LoginRequest, LoginResponse, RegisterRequest, User } from '@/models/Auth';

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>('/auth/login', credentials);
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
    }
    return response.data;
  },

  async register(userData: RegisterRequest): Promise<void> {
    // Register endpoint returns UserResponse (no token), so we don't store anything
    await apiClient.post('/auth/register', userData);
  },

  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<User>('/auth/me');
    return response.data;
  },

  logout(): void {
    localStorage.removeItem('token');
    window.location.href = '/login';
  },

  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }
};
