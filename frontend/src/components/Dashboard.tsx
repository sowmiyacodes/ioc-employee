import React, { useEffect, useState } from 'react';
import { authService } from '../services/authService';
import { employeeService } from '../services/employeeService';
import { leaveService } from '../services/leaveService';
import { UserProfile } from '../types/auth';
import { EmployeeProfile } from '../types/employee';
import { LeaveBalance, LeaveRequest, LeaveRequestsPage } from '../types/leave';
import { EmployeeProfilePage } from './EmployeeProfilePage';
import { LeaveBalanceAdminPage } from './LeaveBalanceAdminPage';
import { LeaveManagementPage } from './LeaveManagementPage';
import { AdminUserManagementPage } from './AdminUserManagementPage';
import { ManagerTeamPage } from './ManagerTeamPage';
import { adminService, AdminDashboardData, HrDashboardData } from '../services/adminService';
import { managerService, ManagerDashboardData } from '../services/managerService';

interface DashboardProps {
  user: UserProfile;
  path: string;
  onNavigate: (path: string) => void;
  onLogout: () => void;
}

const homePaths = {
  EMPLOYEE: '/employee/dashboard',
  MANAGER: '/manager/dashboard',
  HR_ADMIN: '/hr/dashboard',
  SYSTEM_ADMIN: '/admin/dashboard',
} as const;

const roleLabels = {
  EMPLOYEE: 'Employee',
  MANAGER: 'Manager',
  HR_ADMIN: 'HR administrator',
  SYSTEM_ADMIN: 'System administrator',
} as const;

const dateLabel = (value: string) => new Date(`${value}T00:00:00`).toLocaleDateString();

export const Dashboard: React.FC<DashboardProps> = ({ user, path, onNavigate, onLogout }) => {
  const [profile, setProfile] = useState<EmployeeProfile | null>(null);
  const [balances, setBalances] = useState<LeaveBalance[]>([]);
  const [requests, setRequests] = useState<LeaveRequest[]>([]);
  const [pendingCount, setPendingCount] = useState(0);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [adminStats, setAdminStats] = useState<AdminDashboardData | null>(null);
  const [hrStats, setHrStats] = useState<HrDashboardData | null>(null);
  const [managerStats, setManagerStats] = useState<ManagerDashboardData | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState<string | null>(null);

  const isEmployeeHome = path === homePaths.EMPLOYEE && user.role === 'EMPLOYEE';
  const isManagerHome = path === homePaths.MANAGER && user.role === 'MANAGER';

  useEffect(() => {
    let active = true;
    if (!isEmployeeHome && !isManagerHome) return () => { active = false; };
    setLoading(true);
    setLoadError(null);
    const fetchData = isEmployeeHome
      ? Promise.all([employeeService.getMyProfile(), leaveService.getBalances(), leaveService.getMyRequests(0, 4)])
      : leaveService.getPendingRequests(0, 5);
    fetchData
      .then((data) => {
        if (!active) return;
        if (isEmployeeHome && Array.isArray(data)) {
          setProfile(data[0] as EmployeeProfile);
          setBalances(data[1] as LeaveBalance[]);
          setRequests((data[2] as Awaited<ReturnType<typeof leaveService.getMyRequests>>).content);
        } else {
          const pendingPage = data as LeaveRequestsPage;
          setRequests(pendingPage.content);
          setPendingCount(pendingPage.totalElements);
        }
      })
      .catch((reason: unknown) => {
        if (active) setLoadError(reason instanceof Error ? reason.message : 'Unable to load dashboard information.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [isEmployeeHome, isManagerHome, refreshKey]);

  useEffect(() => {
    let active = true;
    const fetchStats = user.role === 'SYSTEM_ADMIN' && path === homePaths.SYSTEM_ADMIN
      ? adminService.getDashboard().then((data) => { if (active) setAdminStats(data); })
      : user.role === 'HR_ADMIN' && path === homePaths.HR_ADMIN
        ? adminService.getHrDashboard().then((data) => { if (active) setHrStats(data); })
        : user.role === 'MANAGER' && isManagerHome
          ? managerService.getDashboard().then((data) => { if (active) setManagerStats(data); })
          : null;
    if (!fetchStats) return () => { active = false; };
    setStatsLoading(true);
    setStatsError(null);
    fetchStats.catch((reason: unknown) => {
      if (active) setStatsError(reason instanceof Error ? reason.message : 'Unable to load dashboard statistics.');
    }).finally(() => { if (active) setStatsLoading(false); });
    return () => { active = false; };
  }, [user.role, path, isManagerHome, refreshKey]);

  const navItems = [
    { path: homePaths[user.role], label: 'Dashboard', icon: '⌂' },
    { path: '/profile', label: 'My profile', icon: '◉' },
    ...(user.role === 'EMPLOYEE' ? [{ path: '/leaves', label: 'Leave management', icon: '▤' }] : []),
    ...(['MANAGER', 'HR_ADMIN'].includes(user.role) ? [{ path: '/approvals', label: 'Leave approvals', icon: '✓' }] : []),
    ...(user.role === 'HR_ADMIN' ? [{ path: '/balance-admin', label: 'Leave balances', icon: '▦' }] : []),
    ...(user.role === 'MANAGER' ? [{ path: '/team', label: 'My team', icon: '♧' }] : []),
    ...(user.role === 'SYSTEM_ADMIN' ? [{ path: '/user-management', label: 'User management', icon: '♙' }] : []),
  ];

  const logout = () => {
    void onLogout();
  };

  const navigateFromMenu = (nextPath: string) => {
    setMobileNavOpen(false);
    onNavigate(nextPath);
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button className="brand brand-button" onClick={() => onNavigate(homePaths[user.role])}>
          <span className="brand-mark" aria-hidden="true">ESSP</span>
          <span className="brand-name">Employee Self-Service Portal</span>
        </button>
        <div className="nav-label">Workspace</div>
        <nav className="nav-links" aria-label="Main navigation">
          {navItems.map((item) => (
            <button
              className={`nav-link ${path === item.path ? 'active' : ''}`}
              key={item.path}
              onClick={() => onNavigate(item.path)}
              aria-current={path === item.path ? 'page' : undefined}
            >
              <span aria-hidden="true">{item.icon}</span>{item.label}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="user-chip">
            <span className="avatar" aria-hidden="true">{user.username.slice(0, 1).toUpperCase()}</span>
            <div className="user-meta"><strong>{user.username}</strong><span>{roleLabels[user.role]}</span></div>
          </div>
          <button className="nav-link" onClick={logout}><span aria-hidden="true">↪</span>Sign out</button>
        </div>
      </aside>
      <div className="main-column">
        <header className="topbar">
          <button
            className="button button-secondary mobile-menu"
            onClick={() => setMobileNavOpen((open) => !open)}
            aria-expanded={mobileNavOpen}
            aria-controls="mobile-navigation"
          >
            {mobileNavOpen ? 'Close menu' : 'Menu'}
          </button>
          <h1 className="topbar-title">Employee Self-Service Portal</h1>
          <button className="button button-secondary" onClick={logout}>Sign out</button>
        </header>
        {mobileNavOpen && (
          <nav className="mobile-nav" id="mobile-navigation" aria-label="Mobile navigation">
            {navItems.map((item) => (
              <button
                className={`nav-link ${path === item.path ? 'active' : ''}`}
                key={item.path}
                onClick={() => navigateFromMenu(item.path)}
                aria-current={path === item.path ? 'page' : undefined}
              >
                <span aria-hidden="true">{item.icon}</span>{item.label}
              </button>
            ))}
          </nav>
        )}
        <main className="page-content">
          {path === '/profile' ? (
            <EmployeeProfilePage onClose={() => onNavigate(homePaths[user.role])} />
          ) : path === '/leaves' && user.role === 'EMPLOYEE' ? (
            <LeaveManagementPage mode="employee" />
          ) : path === '/approvals' && ['MANAGER', 'HR_ADMIN'].includes(user.role) ? (
            <LeaveManagementPage mode="approvals" />
          ) : path === '/balance-admin' && user.role === 'HR_ADMIN' ? (
            <><div className="page-heading"><div><h1>Leave balances</h1><p>Manage an employee's leave entitlement.</p></div></div><LeaveBalanceAdminPage /></>
          ) : path === '/user-management' && user.role === 'SYSTEM_ADMIN' ? (
            <AdminUserManagementPage />
          ) : path === '/team' && user.role === 'MANAGER' ? (
            <ManagerTeamPage />
          ) : isEmployeeHome ? (
            <>
              <div className="page-heading">
                <div><h1>Good to see you, {profile?.legalName || user.username}</h1><p>Your employee services at a glance.</p></div>
                <button className="button button-primary" onClick={() => onNavigate('/leaves')}>Request leave</button>
              </div>
              {loadError && <div className="notice notice-error" role="alert">{loadError}</div>}
              {loading ? <div className="panel loading-screen"><div><div className="spinner" /><p>Loading your dashboard…</p></div></div> : loadError ? (
                <section className="panel"><div className="empty-state">Dashboard information could not be loaded.</div><div className="form-actions"><button className="button button-secondary" onClick={() => setRefreshKey((current) => current + 1)}>Try again</button></div></section>
              ) : (
                <>
                  <div className="stat-grid">
                    <article className="stat-card"><span className="stat-label">Available leave days</span><strong className="stat-value">{balances.reduce((total, item) => total + item.availableDays, 0)}</strong><span className="stat-caption">Across configured leave types</span></article>
                    <article className="stat-card"><span className="stat-label">Pending requests</span><strong className="stat-value">{requests.filter((request) => request.status === 'PENDING').length}</strong><span className="stat-caption">Awaiting a decision</span></article>
                    <article className="stat-card"><span className="stat-label">Department</span><strong className="stat-value stat-value-text">{profile?.department || 'Not available'}</strong><span className="stat-caption">{profile?.designation || 'Profile details unavailable'}</span></article>
                  </div>
                  <section className="panel">
                    <h2 className="panel-title">Leave balance</h2>
                    {balances.length === 0 ? <div className="empty-state">No leave balances are configured for your account yet.</div> : (
                      <div className="balance-grid">{balances.map((balance) => (
                        <article className="balance-card" key={balance.leaveType}>
                          <h3>{balance.leaveType.replace('_', ' ')}</h3>
                          <div className="balance-numbers">
                            <div>Available<strong>{balance.availableDays}</strong></div>
                            <div>Pending<strong>{balance.pendingDays}</strong></div>
                            <div>Used<strong>{balance.usedDays}</strong></div>
                          </div>
                        </article>
                      ))}</div>
                    )}
                  </section>
                  <section className="panel">
                    <h2 className="panel-title">Recent leave requests <button className="text-button" onClick={() => onNavigate('/leaves')}>View all</button></h2>
                    {requests.length === 0 ? <div className="empty-state">Your submitted leave requests will appear here.</div> : (
                      <div className="data-list">{requests.slice(0, 4).map((request) => (
                        <div className="data-row" key={request.id}>
                          <div><strong>{request.leaveType.replace('_', ' ')} · {request.leaveDays} day(s)</strong><br /><span>{dateLabel(request.startDate)} – {dateLabel(request.endDate)}</span></div>
                          <span className={`status-badge status-${request.status.toLowerCase()}`}>{request.status}</span>
                        </div>
                      ))}</div>
                    )}
                  </section>
                </>
              )}
            </>
          ) : isManagerHome ? (
            <>
              <div className="page-heading"><div><h1>Manager dashboard</h1><p>Review leave requests from employees in your reporting scope.</p></div><button className="button button-primary" onClick={() => onNavigate('/approvals')}>Review requests</button></div>
              {loadError && <div className="notice notice-error" role="alert">{loadError}</div>}
              {loading ? <div className="panel loading-screen"><div><div className="spinner" /><p>Loading manager dashboard…</p></div></div> : loadError ? (
                <section className="panel"><div className="empty-state">Manager dashboard information could not be loaded.</div><div className="form-actions"><button className="button button-secondary" onClick={() => setRefreshKey((current) => current + 1)}>Try again</button></div></section>
              ) : (
                <>
                  {statsError && <div className="notice notice-error" role="alert">{statsError}</div>}
                  <div className="stat-grid">
                    <article className="stat-card"><span className="stat-label">Team members</span><strong className="stat-value">{statsLoading ? '…' : managerStats?.teamSize ?? 0}</strong><span className="stat-caption">Active direct reports</span></article>
                    <article className="stat-card"><span className="stat-label">Requests awaiting your review</span><strong className="stat-value">{pendingCount}</strong><span className="stat-caption">Only assigned reporting scope</span></article>
                    <article className="stat-card"><span className="stat-label">Previously approved</span><strong className="stat-value">{statsLoading ? '…' : managerStats?.approvedRequests ?? 0}</strong><span className="stat-caption">Team requests</span></article>
                    <article className="stat-card"><span className="stat-label">Previously rejected</span><strong className="stat-value">{statsLoading ? '…' : managerStats?.rejectedRequests ?? 0}</strong><span className="stat-caption">Team requests</span></article>
                  </div>
                  <section className="panel"><h2 className="panel-title">Pending approvals</h2>
                    {requests.length === 0 ? <div className="empty-state">There are no pending requests in your reporting scope.</div> : requests.slice(0, 5).map((request) => (
                      <div className="data-row" key={request.id}><div><strong>{request.employeeName}</strong><br /><span>{request.leaveType.replace('_', ' ')} · {dateLabel(request.startDate)} – {dateLabel(request.endDate)}</span></div><span className="status-badge status-pending">PENDING</span></div>
                    ))}
                  </section>
                </>
              )}
            </>
          ) : path === homePaths.HR_ADMIN ? (
            <>
              <div className="page-heading"><div><h1>HR workspace</h1><p>Monitor leave activity and manage employee entitlements.</p></div></div>
              {statsError && <div className="notice notice-error" role="alert">{statsError}</div>}
              <div className="stat-grid">
                <article className="stat-card"><span className="stat-label">Active employees</span><strong className="stat-value">{statsLoading ? '…' : hrStats?.activeEmployees ?? 0}</strong><span className="stat-caption">Of {hrStats?.totalEmployees ?? 0} employee accounts</span></article>
                <article className="stat-card"><span className="stat-label">Employees with entitlements</span><strong className="stat-value">{statsLoading ? '…' : hrStats?.employeesWithEntitlements ?? 0}</strong><span className="stat-caption">Database-configured balances</span></article>
                <article className="stat-card"><span className="stat-label">Pending requests</span><strong className="stat-value">{statsLoading ? '…' : hrStats?.pendingLeaveRequests ?? 0}</strong><span className="stat-caption">Awaiting review</span></article>
                <article className="stat-card"><span className="stat-label">Approved requests</span><strong className="stat-value">{statsLoading ? '…' : hrStats?.approvedLeaveRequests ?? 0}</strong><span className="stat-caption">Across all employees</span></article>
                <article className="stat-card"><span className="stat-label">Rejected requests</span><strong className="stat-value">{statsLoading ? '…' : hrStats?.rejectedLeaveRequests ?? 0}</strong><span className="stat-caption">Across all employees</span></article>
              </div>
              <section className="panel"><h2 className="panel-title">Leave administration</h2><p className="muted-copy">Review leave requests and manage approved employee entitlements.</p><div className="form-actions"><button className="button button-primary" onClick={() => onNavigate('/approvals')}>Open leave approvals</button><button className="button button-secondary" onClick={() => onNavigate('/balance-admin')}>Manage entitlements</button></div></section>
            </>
          ) : (
            <>
              <div className="page-heading"><div><h1>System administration</h1><p>Manage accounts and review portal access using live database information.</p></div><button className="button button-primary" onClick={() => onNavigate('/user-management')}>Manage users</button></div>
              {statsError && <div className="notice notice-error" role="alert">{statsError}</div>}
              <div className="stat-grid">
                <article className="stat-card"><span className="stat-label">Total users</span><strong className="stat-value">{statsLoading ? '…' : adminStats?.totalUsers ?? 0}</strong><span className="stat-caption">Database accounts</span></article>
                <article className="stat-card"><span className="stat-label">Active accounts</span><strong className="stat-value">{statsLoading ? '…' : adminStats?.activeUsers ?? 0}</strong><span className="stat-caption">{adminStats?.inactiveUsers ?? 0} inactive</span></article>
                <article className="stat-card"><span className="stat-label">Employees</span><strong className="stat-value">{statsLoading ? '…' : adminStats?.employees ?? 0}</strong><span className="stat-caption">{adminStats?.managers ?? 0} managers · {adminStats?.hrAdministrators ?? 0} HR · {adminStats?.systemAdministrators ?? 0} system admins</span></article>
              </div>
              <section className="panel"><h2 className="panel-title">Account administration</h2><p className="muted-copy">Create and update accounts, activate or deactivate access, and assign eligible reporting managers.</p><button className="button button-primary" onClick={() => onNavigate('/user-management')}>Open user management</button></section>
            </>
          )}
        </main>
      </div>
    </div>
  );
};
