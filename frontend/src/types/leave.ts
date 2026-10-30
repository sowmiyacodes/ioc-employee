export type LeaveType = 'ANNUAL' | 'SICK' | 'PERSONAL';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface LeaveBalance {
  leaveType: LeaveType;
  entitledDays: number;
  usedDays: number;
  pendingDays: number;
  availableDays: number;
}

export interface LeaveRequest {
  id: number;
  employeeId: string;
  employeeName: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  leaveDays: number;
  reason: string | null;
  status: LeaveStatus;
  submittedAt: string;
  decidedAt: string | null;
  decisionMaker: string | null;
  decisionRemarks: string | null;
}

export interface LeaveRequestInput {
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason: string;
}

export interface LeaveRequestsPage {
  content: LeaveRequest[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
