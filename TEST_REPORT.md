# Verification report

## Results

| Check | Result |
|---|---:|
| Backend `mvn test` | 41 tests passed; 0 failures, errors, or skips |
| Frontend `npm run build` | Passed |
| Frontend `npm test` | 2 tests passed |
| Frontend `npm run test:e2e` | 3 browser/security workflows passed |
| Frontend `npm audit` | 0 vulnerabilities |
| Paginated-list performance test | 25 concurrent synthetic users; 250 HTTP requests |

| Endpoint | Requests | p95 |
|---|---:|---:|
| `/api/payslips` | 60 | 323 ms |
| `/api/hr/employees` | 50 | 324 ms |
| `/api/manager/team/leave-requests` | 40 | 324 ms |
| `/api/admin/users` | 40 | 322 ms |
| `/api/leaves/my` | 60 | 156 ms |

The backend tests cover authentication and role authorization, administration,
leave management, and payslip workflows. Payslip coverage includes HR
provisioning, invalid uploads, ownership, pagination, and protected downloads.
The browser workflows verify that an employee address persists after signing
out and back in, and that an HR-provisioned payslip can be downloaded by its
employee.

## Requirements exercised

- Role-based backend access and employee-owned profile/address operations.
- Leave request, review, and entitlement workflows.
- HR-only payslip provisioning and employee-only listing/download.
- Payslip validation, duplicate handling, and download auditing.
- Per-HR payslip upload throttling and its audit event.
- Parameterized admin search / sort rejection of injection payloads.
- Browser verification that address markup is rejected rather than rendered.
- Paginated employee, manager, HR, and system-administrator lists with 25 concurrent users.
- Correlated API error responses and frontend build/test integration.
- Local setup, migrations, API endpoints, architecture, and four-role demo
  instructions are documented in the linked project documentation.

## Scope and limitations

- The 25-user benchmark uses an embedded HTTP server, an in-process test
  database, and synthetic records. Its p95 result is a local baseline, not a
  production-capacity guarantee.
- Salary calculation and automatic PDF generation are not implemented; HR
  provisions finalized PDF statements.

The Maven dependency inventory is generated at
`backend/target/dependency-inventory.txt`. `npm audit` reported zero frontend
vulnerabilities. The Maven dependency-check scan was attempted but stopped
during the initial NVD feed download (about 10% of 416,968 records); no backend
CVE result is claimed. Re-run it with an NVD API key to complete the scan.
