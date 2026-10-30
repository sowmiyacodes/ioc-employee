import { ApiResponse } from '../types/auth';
import { Address, EmployeeProfile } from '../types/employee';
import { authService } from './authService';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '');

class EmployeeService {
  private authorizationHeaders(): HeadersInit {
    const token = authService.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  async getMyProfile(): Promise<EmployeeProfile> {
    const response = await fetch(`${API_BASE_URL}/employees/me`, {
      headers: this.authorizationHeaders(),
    });
    const result = await response.json() as ApiResponse<EmployeeProfile>;
    if (!response.ok || !result.success) {
      if (response.status === 401) authService.clearSession();
      throw new Error(result.message || 'Unable to load your employee profile.');
    }
    return result.data;
  }

  async updateAddress(address: Address): Promise<EmployeeProfile> {
    const response = await fetch(`${API_BASE_URL}/employees/me/address`, {
      method: 'PATCH',
      headers: { ...this.authorizationHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify(address),
    });
    const result = await response.json() as ApiResponse<EmployeeProfile>;
    if (!response.ok || !result.success) {
      if (response.status === 401) authService.clearSession();
      const errorData: unknown = result.data;
      if (errorData && typeof errorData === 'object') {
        const validation = Object.values(errorData)
          .filter((value): value is string => typeof value === 'string')
          .join(' ');
        if (validation) throw new Error(validation);
      }
      throw new Error(result.message || 'Unable to update your address.');
    }
    return result.data;
  }
}

export const employeeService = new EmployeeService();
