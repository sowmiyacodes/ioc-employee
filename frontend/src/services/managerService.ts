import { ApiResponse } from '../types/auth';
import { LeaveRequest, LeaveRequestsPage, LeaveStatus } from '../types/leave';
import { authService } from './authService';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '');

export interface ManagerDashboardData {
  teamSize: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
}

export interface TeamMember {
  profileId: number;
  employeeId: string;
  name: string;
  department: string;
  designation: string;
  email: string;
}

export interface TeamPage {
  content: TeamMember[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

async function request<T>(path: string): Promise<T> {
  const token = authService.getToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  const result = await response.json() as ApiResponse<T>;
  if (!response.ok || !result.success) {
    if (response.status === 401) authService.clearSession();
    throw new Error(result.message || 'The manager service could not complete the request.');
  }
  return result.data;
}

export const managerService = {
  getDashboard: () => request<ManagerDashboardData>('/manager/dashboard'),
  getTeam: (page = 0, size = 20) =>
    request<TeamPage>(`/manager/team?page=${page}&size=${size}`),
  getTeamLeaveRequests: (page = 0, size = 20, status?: LeaveStatus) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) query.set('status', status);
    return request<LeaveRequestsPage>(`/manager/team/leave-requests?${query.toString()}`);
  },
};

export type { LeaveRequest };
