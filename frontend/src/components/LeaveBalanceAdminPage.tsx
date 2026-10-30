import React, { useEffect, useState } from 'react';
import { EntitlementOptions, HrEmployee, adminService } from '../services/adminService';
import { LeaveBalance, LeaveType } from '../types/leave';

export const LeaveBalanceAdminPage: React.FC = () => {
  const [employees, setEmployees] = useState<HrEmployee[]>([]);
  const [employeePage, setEmployeePage] = useState(0);
  const [employeeTotalPages, setEmployeeTotalPages] = useState(0);
  const [employeeSearch, setEmployeeSearch] = useState('');
  const [options, setOptions] = useState<EntitlementOptions | null>(null);
  const [employeeProfileId, setEmployeeProfileId] = useState('');
  const [leaveType, setLeaveType] = useState<LeaveType | ''>('');
  const [entitledDays, setEntitledDays] = useState('');
  const [balances, setBalances] = useState<LeaveBalance[]>([]);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    adminService.getEntitlementOptions()
      .then((entitlementOptions) => {
        if (!active) return;
        setOptions(entitlementOptions);
        setLeaveType(entitlementOptions.leaveTypes[0] ?? '');
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : 'Unable to load entitlement options.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    adminService.getEmployees(employeePage, 100, employeeSearch.trim())
      .then((result) => {
        if (!active) return;
        setEmployees(result.content);
        setEmployeeTotalPages(result.totalPages);
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : 'Unable to load employees.');
      });
    return () => { active = false; };
  }, [employeePage, employeeSearch]);

  useEffect(() => {
    let active = true;
    if (!employeeProfileId) {
      setBalances([]);
      return () => { active = false; };
    }
    adminService.getEmployeeBalances(Number(employeeProfileId))
      .then((result) => { if (active) setBalances(result); })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : 'Unable to load employee balances.');
      });
    return () => { active = false; };
  }, [employeeProfileId]);

  const chooseBalance = (balance: LeaveBalance) => {
    setLeaveType(balance.leaveType);
    setEntitledDays('');
    setNotice(null);
  };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!employeeProfileId || !leaveType || entitledDays === '') return;
    setError(null);
    setNotice(null);
    setSaving(true);
    try {
      const result = await adminService.setEmployeeBalance(
        Number(employeeProfileId), leaveType, Number(entitledDays),
      );
      setNotice(`${result.leaveType.replace('_', ' ')} entitlement saved: ${result.entitledDays} entitled, ${result.usedDays} used, ${result.pendingDays} pending, ${result.availableDays} available.`);
      setEntitledDays('');
      setBalances(await adminService.getEmployeeBalances(Number(employeeProfileId)));
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to save the leave entitlement.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="panel">
      <h2 className="panel-title">Configure leave entitlement</h2>
      <p className="muted-copy">Choose an active employee, supported leave type and approved day value. Existing used and pending leave is preserved.</p>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}
      {loading ? <div className="loading-inline"><div className="spinner" /> Loading employees and policy options…</div> : (
        <form onSubmit={submit}>
          <div className="form-grid">
            <label className="field">Find employee
              <input value={employeeSearch} onChange={(event) => {
                setEmployeeSearch(event.target.value);
                setEmployeePage(0);
                setEmployeeProfileId('');
              }} placeholder="Name, employee ID, email or username" disabled={saving} />
            </label>
            <label className="field">Employee
              <select value={employeeProfileId} onChange={(event) => {
                setEmployeeProfileId(event.target.value);
                setError(null);
                setNotice(null);
              }} required disabled={saving}>
                <option value="">Select an employee</option>
                {employees.map((employee) => (
                  <option value={employee.profileId} key={employee.profileId}>
                    {employee.legalName} · {employee.employeeId}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">Leave type
              <select value={leaveType} onChange={(event) => {
                setLeaveType(event.target.value as LeaveType);
                setEntitledDays('');
              }} required disabled={saving || !options}>
                <option value="" disabled>Select leave type</option>
                {options?.leaveTypes.map((type) => <option value={type} key={type}>{type.replace('_', ' ')}</option>)}
              </select>
            </label>
            <label className="field">Entitled days
              <select value={entitledDays} onChange={(event) => setEntitledDays(event.target.value)} required disabled={saving || !options}>
                <option value="" disabled>Select permitted days</option>
                {options?.entitledDays.map((days) => <option value={days} key={days}>{days} days</option>)}
              </select>
            </label>
          </div>
          {employeeTotalPages > 1 && <div className="pagination-row">
            <button type="button" className="button button-secondary" disabled={employeePage === 0} onClick={() => setEmployeePage((value) => value - 1)}>Previous employees</button>
            <span>Employee results page {employeePage + 1} of {employeeTotalPages}</span>
            <button type="button" className="button button-secondary" disabled={employeePage + 1 >= employeeTotalPages} onClick={() => setEmployeePage((value) => value + 1)}>More employees</button>
          </div>}
          <div className="form-actions"><button className="button button-primary" type="submit" disabled={saving || !employeeProfileId || !leaveType || entitledDays === ''}>
            {saving ? 'Saving…' : 'Save entitlement'}
          </button></div>
        </form>
      )}
      {employeeProfileId && (
        <div className="existing-balances">
          <h3 className="panel-subtitle">Current entitlements</h3>
          {balances.length === 0 ? <div className="empty-state">No leave entitlements are configured for this employee.</div> : (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Leave type</th><th>Entitled</th><th>Used</th><th>Pending</th><th>Available</th><th></th></tr></thead>
                <tbody>{balances.map((balance) => (
                  <tr key={balance.leaveType}>
                    <td>{balance.leaveType.replace('_', ' ')}</td><td>{balance.entitledDays}</td><td>{balance.usedDays}</td>
                    <td>{balance.pendingDays}</td><td>{balance.availableDays}</td>
                    <td><button className="text-button" type="button" onClick={() => chooseBalance(balance)}>Edit</button></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </section>
  );
};
