export type UserRole = 'EMPLOYEE' | 'MANAGER' | 'HR_ADMIN' | 'SYSTEM_ADMIN';

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  active: boolean;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  id: number;
  username: string;
  email: string;
  role: UserRole;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}
