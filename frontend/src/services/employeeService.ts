import { ApiResponse } from '../types/auth';
import { Address, EmployeeProfile } from '../types/employee';
import { authService } from './authService';

const API_BASE_URL = 'http://localhost:8080/api';

class EmployeeService {
  private authorizationHeaders(): HeadersInit {
    const token = authService.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  async getMyProfile(): Promise<EmployeeProfile> {
    const response = await fetch(`${API_BASE_URL}/employees/me`, {
      headers: this.authorizationHeaders(),
    });
    const data: ApiResponse<EmployeeProfile> = await response.json();
    if (!response.ok || !data.success) {
      throw new Error(data.message || 'Unable to load your employee profile.');
    }
    return data.data;
  }

  async updateAddress(address: Partial<Address>): Promise<EmployeeProfile> {
    const response = await fetch(`${API_BASE_URL}/employees/me/address`, {
      method: 'PATCH',
      headers: { ...this.authorizationHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify(address),
    });
    const data: ApiResponse<EmployeeProfile> = await response.json();
    if (!response.ok || !data.success) {
      const errorData: unknown = data.data;
      if (errorData && typeof errorData === 'object') {
        const validation = Object.values(errorData)
          .filter((value): value is string => typeof value === 'string')
          .join(' ');
        throw new Error(validation || data.message);
      }
      throw new Error(data.message || 'Unable to update your address.');
    }
    return data.data;
  }
}

export const employeeService = new EmployeeService();
