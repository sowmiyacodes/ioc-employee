import React, { useEffect, useState } from 'react';
import { managerService, TeamMember } from '../services/managerService';
import { LeaveRequest, LeaveRequestsPage, LeaveStatus } from '../types/leave';

export const ManagerTeamPage: React.FC = () => {
  const [team, setTeam] = useState<TeamMember[]>([]);
  const [requests, setRequests] = useState<LeaveRequest[]>([]);
  const [status, setStatus] = useState<LeaveStatus | ''>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [teamPage, setTeamPage] = useState(0);
  const [teamTotalPages, setTeamTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([
      managerService.getTeam(teamPage, 20),
      managerService.getTeamLeaveRequests(page, 10, status || undefined),
    ]).then(([teamResult, requestPage]) => {
      if (!active) return;
      setTeam(teamResult.content);
      setTeamTotalPages(teamResult.totalPages);
      setRequests(requestPage.content);
      setTotalPages(requestPage.totalPages);
    }).catch((reason: unknown) => {
      if (active) setError(reason instanceof Error ? reason.message : 'Unable to load your reporting team.');
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, status, teamPage]);

  return (
    <>
      <div className="page-heading"><div><h1>My team</h1><p>Employees assigned to your reporting scope.</p></div></div>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      <section className="panel">
        {loading ? <div className="loading-inline"><div className="spinner" /> Loading team…</div> : team.length === 0 ? (
          <div className="empty-state">No active employees are currently assigned to you.</div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Employee</th><th>Employee ID</th><th>Department</th><th>Designation</th><th>Email</th></tr></thead>
              <tbody>{team.map((employee) => (
                <tr key={employee.profileId}>
                  <td><strong>{employee.name}</strong></td><td>{employee.employeeId}</td>
                  <td>{employee.department}</td><td>{employee.designation}</td><td>{employee.email}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        {teamTotalPages > 1 && <div className="pagination-row">
          <button className="button button-secondary" disabled={teamPage === 0} onClick={() => setTeamPage((value) => value - 1)}>Previous team page</button>
          <span>Page {teamPage + 1} of {teamTotalPages}</span>
          <button className="button button-secondary" disabled={teamPage + 1 >= teamTotalPages} onClick={() => setTeamPage((value) => value + 1)}>Next team page</button>
        </div>}
      </section>
      <section className="panel">
        <div className="panel-heading-row"><h2 className="panel-title">Team leave history</h2>
          <label className="filter-field">Status
            <select value={status} onChange={(event) => {
              setPage(0);
              setStatus(event.target.value as LeaveStatus | '');
            }}>
              <option value="">All statuses</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </label>
        </div>
        {loading ? <div className="loading-inline"><div className="spinner" /> Loading leave requests…</div> : requests.length === 0 ? (
          <div className="empty-state">No team leave requests match this status.</div>
        ) : (
          <>
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Employee</th><th>Leave type</th><th>Dates</th><th>Days</th><th>Status</th><th>Decision</th></tr></thead>
                <tbody>{requests.map((request) => (
                  <tr key={request.id}>
                    <td><strong>{request.employeeName}</strong><span>{request.employeeId}</span></td>
                    <td>{request.leaveType.replace('_', ' ')}</td>
                    <td>{request.startDate} – {request.endDate}</td>
                    <td>{request.leaveDays}</td>
                    <td><span className={`status-badge status-${request.status.toLowerCase()}`}>{request.status}</span></td>
                    <td>{request.decisionRemarks || '—'}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            {totalPages > 1 && <div className="pagination-row">
              <button className="button button-secondary" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button>
              <span>Page {page + 1} of {totalPages}</span>
              <button className="button button-secondary" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button>
            </div>}
          </>
        )}
      </section>
    </>
  );
};
