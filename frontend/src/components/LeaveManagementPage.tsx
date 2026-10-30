import React, { useEffect, useState } from 'react';
import { leaveService } from '../services/leaveService';
import { LeaveBalance, LeaveRequest, LeaveStatus, LeaveType } from '../types/leave';

interface Props {
  mode: 'employee' | 'approvals';
}

const leaveTypes: Array<{ value: LeaveType; label: string }> = [
  { value: 'ANNUAL', label: 'Annual leave' },
  { value: 'SICK', label: 'Sick leave' },
  { value: 'PERSONAL', label: 'Personal leave' },
];

const dateLabel = (value: string) => new Date(`${value}T00:00:00`).toLocaleDateString();
const dateTimeLabel = (value: string) => new Date(value).toLocaleDateString();

export const LeaveManagementPage: React.FC<Props> = ({ mode }) => {
  const isApprovals = mode === 'approvals';
  const [balances, setBalances] = useState<LeaveBalance[]>([]);
  const [requests, setRequests] = useState<LeaveRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [filter, setFilter] = useState<'ALL' | LeaveStatus>('ALL');
  const [page, setPage] = useState(0);
  const [pageCount, setPageCount] = useState(1);
  const [totalRequests, setTotalRequests] = useState(0);
  const [leaveType, setLeaveType] = useState<LeaveType>('ANNUAL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');

  const loadData = async (targetPage = page, targetFilter = filter) => {
    setError(null);
    setLoading(true);
    try {
      if (isApprovals) {
        const requestRows = await leaveService.getPendingRequests(targetPage, 8);
        setRequests(requestRows.content);
        setPageCount(Math.max(1, requestRows.totalPages));
        setTotalRequests(requestRows.totalElements);
      } else {
        const [balanceRows, requestRows] = await Promise.all([
          leaveService.getBalances(),
          leaveService.getMyRequests(targetPage, 8, targetFilter === 'ALL' ? undefined : targetFilter),
        ]);
        setBalances(balanceRows);
        setRequests(requestRows.content);
        setPageCount(Math.max(1, requestRows.totalPages));
        setTotalRequests(requestRows.totalElements);
      }
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to load leave information.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, [isApprovals, page, filter]);

  const pageSize = 8;
  const visibleRequests = requests;

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setNotice(null);
    if (new Date(endDate) < new Date(startDate)) {
      setError('The end date must be on or after the start date.');
      return;
    }
    setSubmitting(true);
    try {
      await leaveService.submitRequest({ leaveType, startDate, endDate, reason });
      setNotice('Your leave request was submitted.');
      setStartDate('');
      setEndDate('');
      setReason('');
      setPage(0);
      const [balanceRows, requestRows] = await Promise.all([
        leaveService.getBalances(),
        leaveService.getMyRequests(0, pageSize, filter === 'ALL' ? undefined : filter),
      ]);
      setBalances(balanceRows);
      setRequests(requestRows.content);
      setPageCount(Math.max(1, requestRows.totalPages));
      setTotalRequests(requestRows.totalElements);
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to submit your leave request.');
    } finally {
      setSubmitting(false);
    }
  };

  const decide = async (request: LeaveRequest, action: 'APPROVE' | 'REJECT') => {
    const verb = action === 'APPROVE' ? 'approve' : 'reject';
    if (!window.confirm(`Are you sure you want to ${verb} this leave request?`)) return;
    setError(null);
    setNotice(null);
    try {
      await leaveService.decide(request.id, action);
      setNotice(`Leave request ${action === 'APPROVE' ? 'approved' : 'rejected'}.`);
      await loadData();
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to process this leave decision.');
    }
  };

  const balanceForType = balances.find((balance) => balance.leaveType === leaveType);

  if (loading) {
    return <section className="panel loading-screen"><div><div className="spinner" /><p>Loading leave information…</p></div></section>;
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>{isApprovals ? 'Leave approvals' : 'Leave management'}</h1>
          <p>{isApprovals ? 'Review requests within your authorized scope.' : 'Review your balance, apply for leave, and follow your requests.'}</p>
        </div>
        <button className="button button-secondary" onClick={() => void loadData()}>Refresh</button>
      </div>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}

      {isApprovals ? (
        <section className="panel">
          <h2 className="panel-title">Requests awaiting a decision <small>{totalRequests} pending</small></h2>
          {requests.length === 0 ? (
            <div className="empty-state">There are no pending leave requests in your review scope.</div>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead><tr><th>Employee</th><th>Leave type</th><th>Dates</th><th>Days</th><th>Reason</th><th>Actions</th></tr></thead>
                <tbody>
                  {requests.map((request) => (
                    <tr key={request.id}>
                      <td><strong>{request.employeeName}</strong><br /><span>{request.employeeId}</span></td>
                      <td>{leaveTypes.find((type) => type.value === request.leaveType)?.label ?? request.leaveType}</td>
                      <td>{dateLabel(request.startDate)} – {dateLabel(request.endDate)}</td>
                      <td>{request.leaveDays}</td>
                      <td>{request.reason || '—'}</td>
                      <td><div className="table-actions">
                        <button className="button button-primary button-small" onClick={() => void decide(request, 'APPROVE')}>Approve</button>
                        <button className="button button-danger button-small" onClick={() => void decide(request, 'REJECT')}>Reject</button>
                      </div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {pageCount > 1 && <div className="pagination">
            <span>Page {page + 1} of {pageCount}</span>
            <div className="form-actions">
              <button className="button button-secondary" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Previous</button>
              <button className="button button-secondary" disabled={page + 1 >= pageCount} onClick={() => setPage((current) => current + 1)}>Next</button>
            </div>
          </div>}
        </section>
      ) : (
        <>
          <section aria-labelledby="balance-heading">
            <h2 className="panel-title" id="balance-heading">Leave balance</h2>
            {balances.length === 0 ? (
              <div className="empty-state">No leave balances are configured for your account yet. Please contact HR.</div>
            ) : (
              <div className="balance-grid">
                {balances.map((balance) => (
                  <article className="balance-card" key={balance.leaveType}>
                    <h3>{leaveTypes.find((type) => type.value === balance.leaveType)?.label ?? balance.leaveType}</h3>
                    <div className="balance-numbers">
                      <div>Available<strong>{balance.availableDays}</strong></div>
                      <div>Pending<strong>{balance.pendingDays}</strong></div>
                      <div>Used<strong>{balance.usedDays}</strong></div>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="panel">
            <h2 className="panel-title">Apply for leave <small>Calendar days, inclusive of start and end dates</small></h2>
            <form onSubmit={submit}>
              <div className="form-grid">
                <label className="field" htmlFor="leave-type">
                  Leave type
                  <select id="leave-type" value={leaveType} onChange={(event) => setLeaveType(event.target.value as LeaveType)} disabled={submitting}>
                    {leaveTypes.map((type) => <option value={type.value} key={type.value}>{type.label}</option>)}
                  </select>
                </label>
                <div className="field">
                  Available days
                  <span className="availability">{balanceForType ? `${balanceForType.availableDays} day(s)` : 'No balance configured'}</span>
                </div>
                <label className="field" htmlFor="leave-start">Start date
                  <input id="leave-start" type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} required disabled={submitting} />
                </label>
                <label className="field" htmlFor="leave-end">End date
                  <input id="leave-end" type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} required disabled={submitting} />
                </label>
                <label className="field form-span" htmlFor="leave-reason">Reason <span className="field-optional">Optional</span>
                  <textarea id="leave-reason" value={reason} onChange={(event) => setReason(event.target.value)} maxLength={1000} disabled={submitting} />
                </label>
              </div>
              <div className="form-actions"><button className="button button-primary" type="submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit request'}</button></div>
            </form>
          </section>

          <section className="panel">
            <div className="panel-header">
              <h2 className="panel-title">My requests</h2>
              <label className="filter-field" htmlFor="status-filter">Status
                <select id="status-filter" value={filter} onChange={(event) => { setFilter(event.target.value as typeof filter); setPage(0); }}>
                  <option value="ALL">All requests</option>
                  <option value="PENDING">Pending</option>
                  <option value="APPROVED">Approved</option>
                  <option value="REJECTED">Rejected</option>
                </select>
              </label>
            </div>
            {visibleRequests.length === 0 ? (
              <div className="empty-state">{filter === 'ALL' ? 'You have not submitted any leave requests.' : `No ${filter.toLowerCase()} requests found.`}</div>
            ) : (
              <>
                <div className="table-wrap">
                  <table className="data-table">
                    <thead><tr><th>Leave type</th><th>Dates</th><th>Days</th><th>Submitted</th><th>Status</th><th>Decision details</th></tr></thead>
                    <tbody>{visibleRequests.map((request) => (
                      <tr key={request.id}>
                        <td>{leaveTypes.find((type) => type.value === request.leaveType)?.label ?? request.leaveType}</td>
                        <td>{dateLabel(request.startDate)} – {dateLabel(request.endDate)}</td>
                        <td>{request.leaveDays}</td>
                        <td>{dateTimeLabel(request.submittedAt)}</td>
                        <td><span className={`status-badge status-${request.status.toLowerCase()}`}>{request.status}</span></td>
                        <td>{request.decisionRemarks || (request.decisionMaker ? `By ${request.decisionMaker}` : '—')}</td>
                      </tr>
                    ))}</tbody>
                  </table>
                </div>
                {pageCount > 1 && <div className="pagination">
                  <span>Page {page + 1} of {pageCount}</span>
                  <div className="form-actions">
                    <button className="button button-secondary" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Previous</button>
                    <button className="button button-secondary" disabled={page + 1 >= pageCount} onClick={() => setPage((current) => current + 1)}>Next</button>
                  </div>
                </div>}
              </>
            )}
          </section>
        </>
      )}
    </>
  );
};
