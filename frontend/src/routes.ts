import { UserRole } from './types/auth';

export const roleHome: Record<UserRole, string> = {
  EMPLOYEE: '/employee/dashboard',
  MANAGER: '/manager/dashboard',
  HR_ADMIN: '/hr/dashboard',
  SYSTEM_ADMIN: '/admin/dashboard',
};

const protectedPaths: Record<UserRole, string[]> = {
  EMPLOYEE: ['/employee/dashboard', '/profile', '/leaves'],
  MANAGER: ['/manager/dashboard', '/profile', '/approvals', '/team'],
  HR_ADMIN: ['/hr/dashboard', '/profile', '/approvals', '/balance-admin'],
  SYSTEM_ADMIN: ['/admin/dashboard', '/profile', '/user-management'],
};

export function canAccessPath(role: UserRole, path: string): boolean {
  return protectedPaths[role].includes(path);
}
