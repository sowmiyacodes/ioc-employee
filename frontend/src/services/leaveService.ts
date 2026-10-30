import { ApiResponse } from '../types/auth';
import { LeaveBalance, LeaveRequest, LeaveRequestInput, LeaveRequestsPage, LeaveStatus } from '../types/leave';
import { authService } from './authService';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '');

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = authService.getToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...init.headers,
    },
  });

  const result = await response.json() as ApiResponse<T>;
  if (!response.ok || !result.success) {
    if (response.status === 401) authService.clearSession();
    throw new Error(result.message || 'The leave service could not complete the request.');
  }
  return result.data;
}

export const leaveService = {
  getBalances: () => request<LeaveBalance[]>('/leaves/balance'),
  getMyRequests: (page = 0, size = 20, status?: LeaveStatus) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) query.set('status', status);
    return request<LeaveRequestsPage>(`/leaves/my?${query.toString()}`);
  },
  getPendingRequests: (page = 0, size = 20) =>
    request<LeaveRequestsPage>(`/leaves/pending?page=${page}&size=${size}`),
  submitRequest: (input: LeaveRequestInput) =>
    request<LeaveRequest>('/leaves', { method: 'POST', body: JSON.stringify(input) }),
  decide: (id: number, action: 'APPROVE' | 'REJECT', remarks = '') =>
    request<LeaveRequest>(`/leaves/${id}/decision`, {
      method: 'PATCH',
      body: JSON.stringify({ action, remarks }),
    }),
  setBalance: (employeeProfileId: number, leaveType: LeaveBalance['leaveType'], entitledDays: number) =>
    request<LeaveBalance>('/leaves/balance', {
      method: 'PUT',
      body: JSON.stringify({ employeeProfileId, leaveType, entitledDays }),
    }),
};
