import { describe, expect, it } from 'vitest';
import { canAccessPath, roleHome } from './routes';

describe('role-specific portal routes', () => {
  it('assigns a distinct dashboard to every supported role', () => {
    expect(roleHome.EMPLOYEE).toBe('/employee/dashboard');
    expect(roleHome.MANAGER).toBe('/manager/dashboard');
    expect(roleHome.HR_ADMIN).toBe('/hr/dashboard');
    expect(roleHome.SYSTEM_ADMIN).toBe('/admin/dashboard');
    expect(new Set(Object.values(roleHome)).size).toBe(4);
  });

  it('restricts frontend navigation according to the authenticated role', () => {
    expect(canAccessPath('EMPLOYEE', '/leaves')).toBe(true);
    expect(canAccessPath('EMPLOYEE', '/payslips')).toBe(true);
    expect(canAccessPath('EMPLOYEE', '/approvals')).toBe(false);
    expect(canAccessPath('MANAGER', '/approvals')).toBe(true);
    expect(canAccessPath('MANAGER', '/team')).toBe(true);
    expect(canAccessPath('HR_ADMIN', '/balance-admin')).toBe(true);
    expect(canAccessPath('MANAGER', '/balance-admin')).toBe(false);
    expect(canAccessPath('SYSTEM_ADMIN', '/user-management')).toBe(true);
    expect(canAccessPath('EMPLOYEE', '/user-management')).toBe(false);
    expect(canAccessPath('MANAGER', '/payslips')).toBe(false);
    expect(canAccessPath('SYSTEM_ADMIN', '/hr/dashboard')).toBe(false);
  });
});
