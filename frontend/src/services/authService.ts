import { ApiResponse, LoginResponse, UserProfile } from '../types/auth';

const API_BASE_URL = 'http://localhost:8080/api';

class AuthService {
  private tokenKey = 'essp_auth_token';
  private userKey = 'essp_user_info';

  public async login(username: string, password: string):Promise<LoginResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password }),
    });

    const data: ApiResponse<LoginResponse> = await response.json();

    if (!response.ok || !data.success) {
      throw new Error(data.message || 'Authentication failed. Please verify your credentials.');
    }

    this.saveSession(data.data.token, {
      id: data.data.id,
      username: data.data.username,
      email: data.data.email,
      role: data.data.role,
      active: true,
    });

    return data.data;
  }

  public async logout(): Promise<void> {
    const token = this.getToken();
    if (token) {
      try {
        await fetch(`${API_BASE_URL}/auth/logout`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
      } catch (err) {
        console.warn('Backend logout notification failed, clearing local state', err);
      }
    }
    this.clearSession();
  }

  public async getCurrentUser(): Promise<UserProfile | null> {
    const token = this.getToken();
    if (!token) return null;

    try {
      const response = await fetch(`${API_BASE_URL}/auth/me`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        this.clearSession();
        return null;
      }

      const data: ApiResponse<UserProfile> = await response.json();
      return data.data;
    } catch {
      return this.getCachedUser();
    }
  }

  public saveSession(token: string, user: UserProfile): void {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.userKey, JSON.stringify(user));
  }

  public clearSession(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }

  public getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  public getCachedUser(): UserProfile | null {
    const raw = localStorage.getItem(this.userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  public isAuthenticated(): boolean {
    return !!this.getToken();
  }
}

export const authService = new AuthService();
