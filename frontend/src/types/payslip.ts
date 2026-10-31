export interface Payslip {
  id: number;
  payPeriodStart: string;
  payPeriodEnd: string;
  issuedAt: string;
}

export interface PayslipPage {
  content: Payslip[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
