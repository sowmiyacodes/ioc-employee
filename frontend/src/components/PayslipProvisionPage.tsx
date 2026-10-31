import React, { useEffect, useRef, useState } from 'react';
import { adminService, HrEmployee } from '../services/adminService';

const MAX_FILE_SIZE = 10 * 1024 * 1024;

export const PayslipProvisionPage: React.FC = () => {
  const [employees, setEmployees] = useState<HrEmployee[]>([]);
  const [employeePage, setEmployeePage] = useState(0);
  const [employeeTotalPages, setEmployeeTotalPages] = useState(0);
  const [employeeSearch, setEmployeeSearch] = useState('');
  const [employeeProfileId, setEmployeeProfileId] = useState('');
  const [payPeriodStart, setPayPeriodStart] = useState('');
  const [payPeriodEnd, setPayPeriodEnd] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    adminService.getEmployees(employeePage, 100, employeeSearch.trim())
      .then((result) => {
        if (!active) return;
        setEmployees(result.content);
        setEmployeeTotalPages(result.totalPages);
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : 'Unable to load active employees.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [employeePage, employeeSearch]);

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!employeeProfileId || !file) return;
    setError(null);
    setNotice(null);
    if (payPeriodEnd < payPeriodStart) {
      setError('Pay period end date must be on or after the start date.');
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      setError('The PDF must be 10 MB or smaller.');
      return;
    }
    setSaving(true);
    try {
      const result = await adminService.uploadPayslip(
        Number(employeeProfileId), payPeriodStart, payPeriodEnd, file,
      );
      setNotice(`Payslip ${result.id} was added. The employee can now find it under My payslips.`);
      setEmployeeProfileId('');
      setPayPeriodStart('');
      setPayPeriodEnd('');
      setFile(null);
      formRef.current?.reset();
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to upload the payslip.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="page-heading">
        <div><h1>Payslip management</h1><p>Provide a confidential PDF payslip to an active employee.</p></div>
      </div>
      <section className="panel">
        <h2 className="panel-title">Upload a payslip</h2>
        <p className="muted-copy">Payslips are stored outside the public web folder and can only be downloaded by the selected employee.</p>
        {error && <div className="notice notice-error" role="alert">{error}</div>}
        {notice && <div className="notice notice-success" role="status">{notice}</div>}
        {loading ? <div className="loading-inline"><div className="spinner" /> Loading active employees…</div> : (
          <form ref={formRef} onSubmit={(event) => void submit(event)}>
            <div className="form-grid">
              <label className="field">Find employee
                <input
                  value={employeeSearch}
                  onChange={(event) => {
                    setEmployeeSearch(event.target.value);
                    setEmployeePage(0);
                    setEmployeeProfileId('');
                  }}
                  placeholder="Name, employee ID, email or username"
                  disabled={saving}
                />
              </label>
              <label className="field">Employee
                <select
                  value={employeeProfileId}
                  onChange={(event) => setEmployeeProfileId(event.target.value)}
                  required
                  disabled={saving || employees.length === 0}
                >
                  <option value="">Select an active employee</option>
                  {employees.map((employee) => (
                    <option key={employee.profileId} value={employee.profileId}>
                      {employee.legalName} · {employee.employeeId}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">Pay period start
                <input type="date" value={payPeriodStart} onChange={(event) => setPayPeriodStart(event.target.value)} required disabled={saving} />
              </label>
              <label className="field">Pay period end
                <input type="date" value={payPeriodEnd} onChange={(event) => setPayPeriodEnd(event.target.value)} required disabled={saving} />
              </label>
              <label className="field">Payslip PDF
                <input
                  type="file"
                  accept="application/pdf,.pdf"
                  onChange={(event) => setFile(event.target.files?.[0] ?? null)}
                  required
                  disabled={saving}
                />
                <span className="field-hint">PDF only, maximum 10 MB.</span>
              </label>
            </div>
            {employeeTotalPages > 1 && <div className="pagination-row">
              <button type="button" className="button button-secondary" disabled={employeePage === 0 || saving} onClick={() => setEmployeePage((value) => value - 1)}>Previous employees</button>
              <span>Employee results page {employeePage + 1} of {employeeTotalPages}</span>
              <button type="button" className="button button-secondary" disabled={employeePage + 1 >= employeeTotalPages || saving} onClick={() => setEmployeePage((value) => value + 1)}>More employees</button>
            </div>}
            <div className="form-actions">
              <button className="button button-primary" type="submit" disabled={saving || !employeeProfileId || !file}>
                {saving ? 'Uploading…' : 'Upload payslip'}
              </button>
            </div>
          </form>
        )}
      </section>
    </>
  );
};
