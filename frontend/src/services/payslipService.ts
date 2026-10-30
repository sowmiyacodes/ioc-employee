import { ApiResponse } from '../types/auth';
import { Payslip } from '../types/payslip';
import { authService } from './authService';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '');

export const payslipService = {
  async getPayslips(): Promise<Payslip[]> {
    const token = authService.getToken();
    const response = await fetch(`${API_BASE_URL}/payslips`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      cache: 'no-store',
    });
    const result = await response.json() as ApiResponse<Payslip[]>;
    if (!response.ok || !result.success) {
      if (response.status === 401) authService.clearSession();
      throw new Error(result.message || 'Unable to load your payslips.');
    }
    return result.data;
  },

  async download(id: number): Promise<Blob> {
    const token = authService.getToken();
    const response = await fetch(`${API_BASE_URL}/payslips/${id}/download`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      cache: 'no-store',
    });
    if (!response.ok) {
      if (response.status === 401) authService.clearSession();
      const result = await response.json() as ApiResponse<null>;
      throw new Error(result.message || 'Unable to download this payslip.');
    }
    return response.blob();
  },
};
