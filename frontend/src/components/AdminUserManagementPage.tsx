import React, { useEffect, useState } from 'react';
import { AdminRole, AdminUser, AdminUserInput, adminService } from '../services/adminService';

const roles: AdminRole[] = ['EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN'];

const blankForm: AdminUserInput = {
  username: '',
  email: '',
  password: '',
  role: 'EMPLOYEE',
  employeeId: '',
  legalName: '',
  department: '',
  designation: '',
  joiningDate: '',
  managerId: null,
};

export const AdminUserManagementPage: React.FC = () => {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [managers, setManagers] = useState<AdminUser[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [role, setRole] = useState<AdminRole | ''>('');
  const [active, setActive] = useState('');
  const [sortBy, setSortBy] = useState('username');
  const [direction, setDirection] = useState('asc');
  const [form, setForm] = useState<AdminUserInput>(blankForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [refresh, setRefresh] = useState(0);

  useEffect(() => {
    let activeRequest = true;
    setLoading(true);
    setError(null);
    const activeFilter = active === '' ? undefined : active === 'true';
    Promise.all([
      adminService.getUsers({ page, size: 20, search, role: role || undefined, active: activeFilter, sortBy, direction }),
      adminService.getUsers({ page: 0, size: 100, role: 'MANAGER', active: true }),
    ]).then(([result, managerResult]) => {
      if (!activeRequest) return;
      setUsers(result.content);
      setTotalPages(result.totalPages);
      setManagers(managerResult.content);
    }).catch((reason: unknown) => {
      if (activeRequest) setError(reason instanceof Error ? reason.message : 'Unable to load users.');
    }).finally(() => { if (activeRequest) setLoading(false); });
    return () => { activeRequest = false; };
  }, [page, search, role, active, sortBy, direction, refresh]);

  const setField = <K extends keyof AdminUserInput>(key: K, value: AdminUserInput[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const startCreate = () => {
    setEditingId(null);
    setForm({ ...blankForm });
    setFormOpen(true);
    setNotice(null);
  };

  const startEdit = (user: AdminUser) => {
    setEditingId(user.id);
    setForm({
      email: user.email,
      role: user.role,
      employeeId: user.employeeId ?? '',
      legalName: user.legalName ?? '',
      department: user.department ?? '',
      designation: user.designation ?? '',
      joiningDate: user.joiningDate ?? '',
      managerId: user.managerId,
    });
    setFormOpen(true);
    setNotice(null);
  };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setNotice(null);
    setSaving(true);
    try {
      if (editingId === null) {
        await adminService.createUser(form);
        setNotice('User account created.');
      } else {
        await adminService.updateUser(editingId, form);
        setNotice('User account updated.');
      }
      setFormOpen(false);
      setEditingId(null);
      setForm({ ...blankForm });
      setRefresh((value) => value + 1);
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to save this user.');
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (user: AdminUser) => {
    const next = !user.active;
    const action = next ? 'activate' : 'deactivate';
    if (!window.confirm(`Are you sure you want to ${action} ${user.username}?`)) return;
    setError(null);
    setNotice(null);
    try {
      await adminService.setUserActive(user.id, next);
      setNotice(`Account ${next ? 'activated' : 'deactivated'}.`);
      setRefresh((value) => value + 1);
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : `Unable to ${action} this account.`);
    }
  };

  const applySearch = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  return (
    <>
      <div className="page-heading">
        <div><h1>User management</h1><p>Create, update and manage portal accounts and reporting assignments.</p></div>
        <button className="button button-primary" type="button" onClick={startCreate}>Add user</button>
      </div>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}

      {formOpen && (
        <section className="panel admin-form-panel">
          <div className="panel-heading-row">
            <h2 className="panel-title">{editingId === null ? 'Create user account' : 'Edit user account'}</h2>
            <button type="button" className="text-button" onClick={() => setFormOpen(false)}>Cancel</button>
          </div>
          <form onSubmit={submit}>
            <div className="form-grid">
              {editingId === null && (
                <>
                  <label className="field">Username
                    <input value={form.username ?? ''} maxLength={50} autoComplete="off" required
                      onChange={(event) => setField('username', event.target.value)} disabled={saving} />
                  </label>
                  <label className="field">Initial password
                    <input type="password" value={form.password ?? ''} minLength={12} maxLength={72} autoComplete="new-password" required
                      onChange={(event) => setField('password', event.target.value)} disabled={saving} />
                  </label>
                </>
              )}
              <label className="field">Email
                <input type="email" value={form.email} maxLength={100} required
                  onChange={(event) => setField('email', event.target.value)} disabled={saving} />
              </label>
              <label className="field">Role
                <select value={form.role} required
                  onChange={(event) => setField('role', event.target.value as AdminRole)} disabled={saving}>
                  {roles.map((item) => <option value={item} key={item}>{item.replace('_', ' ')}</option>)}
                </select>
              </label>
              <label className="field">Employee ID
                <input value={form.employeeId} maxLength={30} required
                  onChange={(event) => setField('employeeId', event.target.value)} disabled={saving} />
              </label>
              <label className="field">Full name
                <input value={form.legalName} maxLength={150} required
                  onChange={(event) => setField('legalName', event.target.value)} disabled={saving} />
              </label>
              <label className="field">Department
                <input value={form.department} maxLength={100} required
                  onChange={(event) => setField('department', event.target.value)} disabled={saving} />
              </label>
              <label className="field">Designation
                <input value={form.designation} maxLength={100} required
                  onChange={(event) => setField('designation', event.target.value)} disabled={saving} />
              </label>
              <label className="field">Joining date
                <input type="date" value={form.joiningDate} required
                  onChange={(event) => setField('joiningDate', event.target.value)} disabled={saving} />
              </label>
              <label className="field">Reporting manager
                <select value={form.managerId ?? ''} onChange={(event) => setField(
                  'managerId', event.target.value ? Number(event.target.value) : null,
                )} disabled={saving}>
                  <option value="">No manager assigned</option>
                  {managers.map((manager) => (
                    <option value={manager.id} key={manager.id}>
                      {manager.legalName} · {manager.employeeId}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            {editingId === null && <p className="muted-copy">New passwords must be at least 12 characters. Passwords are hashed before they are stored.</p>}
            <div className="form-actions">
              <button className="button button-primary" type="submit" disabled={saving}>
                {saving ? 'Saving…' : editingId === null ? 'Create account' : 'Save changes'}
              </button>
              <button className="button button-secondary" type="button" onClick={() => setFormOpen(false)} disabled={saving}>Cancel</button>
            </div>
          </form>
        </section>
      )}

      <section className="panel">
        <form className="admin-filter-row" onSubmit={applySearch}>
          <label className="field">Search accounts
            <input value={searchInput} onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Employee ID, name, email or username" />
          </label>
          <label className="field">Role
            <select value={role} onChange={(event) => { setRole(event.target.value as AdminRole | ''); setPage(0); }}>
              <option value="">All roles</option>
              {roles.map((item) => <option value={item} key={item}>{item.replace('_', ' ')}</option>)}
            </select>
          </label>
          <label className="field">Status
            <select value={active} onChange={(event) => { setActive(event.target.value); setPage(0); }}>
              <option value="">All statuses</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </label>
          <label className="field">Sort by
            <select value={sortBy} onChange={(event) => { setSortBy(event.target.value); setPage(0); }}>
              <option value="username">Username</option>
              <option value="email">Email</option>
              <option value="role">Role</option>
              <option value="active">Status</option>
              <option value="employeeId">Employee ID</option>
              <option value="name">Name</option>
            </select>
          </label>
          <label className="field">Order
            <select value={direction} onChange={(event) => { setDirection(event.target.value); setPage(0); }}>
              <option value="asc">Ascending</option>
              <option value="desc">Descending</option>
            </select>
          </label>
          <button type="submit" className="button button-secondary">Search</button>
        </form>
        {loading ? <div className="loading-inline"><div className="spinner" /> Loading users…</div> : users.length === 0 ? (
          <div className="empty-state">No matching user accounts were found.</div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Employee</th><th>Username / email</th><th>Role</th><th>Manager</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>{users.map((user) => (
                <tr key={user.id}>
                  <td><strong>{user.legalName || 'Profile unavailable'}</strong><span>{user.employeeId || 'No employee ID'}</span></td>
                  <td>{user.username}<span>{user.email}</span></td>
                  <td>{user.role.replace('_', ' ')}</td>
                  <td>{user.managerName || '—'}</td>
                  <td><span className={`status-badge ${user.active ? 'status-approved' : 'status-rejected'}`}>{user.active ? 'Active' : 'Inactive'}</span></td>
                  <td><div className="table-actions">
                    <button className="text-button" type="button" onClick={() => startEdit(user)}>Edit</button>
                    <button className="text-button" type="button" onClick={() => void toggleActive(user)}>{user.active ? 'Deactivate' : 'Activate'}</button>
                  </div></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        {totalPages > 1 && <div className="pagination-row">
          <button className="button button-secondary" type="button" disabled={page === 0 || loading} onClick={() => setPage((value) => value - 1)}>Previous</button>
          <span>Page {page + 1} of {totalPages}</span>
          <button className="button button-secondary" type="button" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((value) => value + 1)}>Next</button>
        </div>}
      </section>
    </>
  );
};
