import { ApiResponse } from '../types/auth';
import { LeaveBalance, LeaveType } from '../types/leave';
import { authService } from './authService';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '');

export type AdminRole = 'EMPLOYEE' | 'MANAGER' | 'HR_ADMIN' | 'SYSTEM_ADMIN';

export interface AdminUser {
  id: number;
  username: string;
  email: string;
  role: AdminRole;
  active: boolean;
  employeeId: string | null;
  legalName: string | null;
  department: string | null;
  designation: string | null;
  joiningDate: string | null;
  managerId: number | null;
  managerName: string | null;
}

export interface AdminUserPage {
  content: AdminUser[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminDashboardData {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  employees: number;
  managers: number;
  hrAdministrators: number;
  systemAdministrators: number;
}

export interface AdminUserInput {
  username?: string;
  email: string;
  password?: string;
  role: AdminRole;
  employeeId: string;
  legalName: string;
  department: string;
  designation: string;
  joiningDate: string;
  managerId: number | null;
}

export interface HrEmployee {
  profileId: number;
  employeeId: string;
  legalName: string;
  username: string;
}

export interface HrEmployeePage {
  content: HrEmployee[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EntitlementOptions {
  leaveTypes: LeaveType[];
  entitledDays: number[];
}

export interface HrDashboardData {
  totalEmployees: number;
  activeEmployees: number;
  employeesWithEntitlements: number;
  pendingLeaveRequests: number;
  approvedLeaveRequests: number;
  rejectedLeaveRequests: number;
}

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
    throw new Error(result.message || 'The administration service could not complete the request.');
  }
  return result.data;
}

export const adminService = {
  getDashboard: () => request<AdminDashboardData>('/admin/dashboard'),
  getUsers: (params: { page?: number; size?: number; search?: string; role?: AdminRole; active?: boolean; sortBy?: string; direction?: string } = {}) => {
    const query = new URLSearchParams();
    query.set('page', String(params.page ?? 0));
    query.set('size', String(params.size ?? 20));
    if (params.search) query.set('search', params.search);
    if (params.role) query.set('role', params.role);
    if (params.active !== undefined) query.set('active', String(params.active));
    if (params.sortBy) query.set('sortBy', params.sortBy);
    if (params.direction) query.set('direction', params.direction);
    return request<AdminUserPage>(`/admin/users?${query.toString()}`);
  },
  createUser: (input: AdminUserInput) =>
    request<AdminUser>('/admin/users', { method: 'POST', body: JSON.stringify(input) }),
  updateUser: (id: number, input: AdminUserInput) =>
    request<AdminUser>(`/admin/users/${id}`, { method: 'PATCH', body: JSON.stringify(input) }),
  setUserActive: (id: number, active: boolean) =>
    request<AdminUser>(`/admin/users/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
    }),
  getHrDashboard: () => request<HrDashboardData>('/hr/dashboard'),
  getEmployees: (page = 0, size = 100, search = '') => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (search) query.set('search', search);
    return request<HrEmployeePage>(`/hr/employees?${query.toString()}`);
  },
  getEntitlementOptions: () => request<EntitlementOptions>('/hr/entitlement-options'),
  getEmployeeBalances: (profileId: number) =>
    request<LeaveBalance[]>(`/hr/leave-balances?employeeProfileId=${profileId}`),
  setEmployeeBalance: (employeeProfileId: number, leaveType: LeaveType, entitledDays: number) =>
    request<LeaveBalance>('/leaves/balance', {
      method: 'PUT',
      body: JSON.stringify({ employeeProfileId, leaveType, entitledDays }),
    }),
};
