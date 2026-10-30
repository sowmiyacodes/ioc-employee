import { ApiResponse, LoginResponse, UserProfile } from '../types/auth';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '');

class AuthService {
  private readonly tokenKey = 'essp_auth_token';

  public async login(username: string, password: string): Promise<LoginResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const result = await response.json() as ApiResponse<LoginResponse>;

    if (!response.ok || !result.success) {
      throw new Error(result.message || 'Authentication failed. Please verify your credentials.');
    }

    this.saveSession(result.data.token);
    return result.data;
  }

  public async logout(): Promise<void> {
    const token = this.getToken();
    try {
      if (token) {
        const response = await fetch(`${API_BASE_URL}/auth/logout`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!response.ok) throw new Error('The server could not invalidate your session.');
      }
    } finally {
      this.clearSession();
    }
  }

  public async getCurrentUser(): Promise<UserProfile | null> {
    const token = this.getToken();
    if (!token) return null;

    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.status === 401 || response.status === 403) {
      this.clearSession();
      return null;
    }

    const result = await response.json() as ApiResponse<UserProfile>;
    if (!response.ok || !result.success) {
      throw new Error(result.message || 'Unable to verify your session.');
    }
    return result.data;
  }

  public saveSession(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  public clearSession(): void {
    localStorage.removeItem(this.tokenKey);
  }

  public getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }
}

export const authService = new AuthService();
