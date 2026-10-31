# Employee Self-Service Portal (ESSP)
The portal combines Spring Boot authentication and authorization, employee profile
and address management, leave management, protected payslip downloads, and a React self-service interface.

---

### 1. Architectural Highlights

- **Backend Framework:** Java 17+, Spring Boot 3.2.4
- **Security & RBAC:** Spring Security with custom `JwtAuthenticationFilter`
- **Password Security:** BCrypt password hashing (`BCryptPasswordEncoder`) with enforced minimum **12-character** policy
- **Roles Implemented:**
  - `EMPLOYEE`
  - `MANAGER`
  - `HR_ADMIN`
  - `SYSTEM_ADMIN`
- **Brute Force Protection:** Bucket4j token bucket rate limiter per client IP / username
- **Audit Logging:** Security and business events are persisted in the `audit_events` table
- **Request correlation:** Requests receive an `X-Correlation-ID`, which is returned in the response and stored with audit events
- **Database:** File-backed H2 for local use, Flyway migrations, optional PostgreSQL configuration
- **Frontend Stack:** React 18, TypeScript, Vite

---

### 2. Available Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Authenticates credentials and returns JWT token |
| `POST` | `/api/auth/logout` | Authenticated | Invalidates JWT token and clears session |
| `GET` | `/api/auth/me` | Authenticated | Retrieves current authenticated profile & role |
| `GET` | `/api/employees/me` | Authenticated | Retrieves the current user's employee profile |
| `PATCH` | `/api/employees/me/address` | Authenticated | Updates only the current user's address |
| `GET` | `/api/leaves/balance` | Authenticated | Retrieves configured leave balances |
| `POST` | `/api/leaves` | EMPLOYEE | Submits a leave request |
| `GET` | `/api/leaves/my` | Authenticated | Retrieves paginated personal leave history |
| `GET` | `/api/leaves/pending` | MANAGER, HR_ADMIN, SYSTEM_ADMIN | Retrieves pending requests in authorized scope |
| `PATCH` | `/api/leaves/{id}/decision` | MANAGER, HR_ADMIN, SYSTEM_ADMIN | Decides a pending request |
| `PUT` | `/api/leaves/balance` | HR_ADMIN, SYSTEM_ADMIN | Configures an employee leave entitlement |
| `GET` | `/api/payslips` | EMPLOYEE | Lists the authenticated employee's payslips with pagination |
| `GET` | `/api/payslips/{id}/download` | EMPLOYEE | Downloads an authorized PDF payslip |
| `POST` | `/api/hr/payslips` | HR_ADMIN | Uploads a PDF payslip for an active employee |
| `GET` | `/api/manager/dashboard` | MANAGER | Team and leave metrics for direct reports |
| `GET` | `/api/manager/team` | MANAGER | Active employees reporting to the manager |
| `GET` | `/api/manager/team/leave-requests` | MANAGER | Paginated leave history for direct reports |
| `GET` | `/api/hr/dashboard` | HR_ADMIN, SYSTEM_ADMIN | Database-derived HR metrics |
| `GET` | `/api/hr/employees` | HR_ADMIN, SYSTEM_ADMIN | Paginated active employee choices |
| `GET` | `/api/hr/entitlement-options` | HR_ADMIN, SYSTEM_ADMIN | Supported leave types and permitted day values |
| `GET` | `/api/hr/leave-balances` | HR_ADMIN, SYSTEM_ADMIN | Balances for an active employee profile |
| `GET` | `/api/admin/dashboard` | SYSTEM_ADMIN | Database-derived user counts |
| `GET` | `/api/admin/users` | SYSTEM_ADMIN | Searchable, filterable, sortable paginated user list |
| `GET` | `/api/admin/users/{id}` | SYSTEM_ADMIN | User and employee profile details |
| `POST` | `/api/admin/users` | SYSTEM_ADMIN | Create an account and employee profile |
| `PATCH` | `/api/admin/users/{id}` | SYSTEM_ADMIN | Update profile, role and reporting manager |
| `PATCH` | `/api/admin/users/{id}/status` | SYSTEM_ADMIN | Activate or deactivate an account |

---

### 3. User provisioning and optional demo accounts

The application does **not** create accounts or use a built-in password by
default. On a new empty database, provision the first System Administrator from
environment variables as described below. An existing database is preserved;
the bootstrap configuration is ignored once it already contains users.

For a disposable local demo database only, set `ESSP_SEED_DEMO_DATA=true` and
provide a private `APP_DEMO_PASSWORD` of at least 12 characters. On an empty
database, this seeds fictional accounts:

1. **Employee:** `employee1` or `employee@essp.local`
2. **Manager:** `manager1` or `manager@essp.local`
3. **HR Admin:** `hradmin1` or `hradmin@essp.local`
4. **System Admin:** `sysadmin1` or `sysadmin@essp.local`

All demo accounts use the password you set in `APP_DEMO_PASSWORD`. Demo seeding
is opt-in and should not be enabled for production.

---

### 4. Running the Application

#### Prerequisites

- Java 17 or newer
- Maven 3.8 or newer
- Node.js 22.12 or newer (required by the current Vite/Vitest toolchain)

#### Backend (Spring Boot):
```bash
# From the Employee_Self-Service_Portal root
cd backend
mvn clean test
mvn spring-boot:run
```

#### Frontend (React / Vite):
```bash
cd frontend
npm ci
npm run dev
```

The frontend is available at `http://localhost:3000`. Login redirects each
authenticated role to its own dashboard: employee, manager, HR administrator,
or system administrator. Frontend navigation is a usability feature; backend
authorization independently protects API operations.

#### Database (H2):

The local database is file-backed and persists across backend restarts. To open
it in the H2 web console:

1. Start the backend from the `backend` directory:
   ```powershell
   cd D:\Employee_Self-Service_Portal\Employee_Self-Service_Portal\backend
   mvn spring-boot:run
   ```
2. While it is running, open [`http://localhost:8080/h2-console`](http://localhost:8080/h2-console).
3. Use these settings on the H2 login page:

   | Field | Value |
   |---|---|
   | Driver Class | `org.h2.Driver` |
   | JDBC URL | `jdbc:h2:file:./data/essp_db;DB_CLOSE_ON_EXIT=FALSE` |
   | User Name | `sa` |
   | Password | *(leave blank)* |

4. Click **Connect**. The database tables appear in the console's left panel.

The database files are stored under `backend/data`. Keep the JDBC URL and
backend working directory consistent so the console opens the same database
used by the application. Flyway applies the versioned schema migrations, and
Hibernate validates the schema without recreating it. Back up the database
files before moving or deleting them.

The H2 console is intended for local development only. Do not expose it to
untrusted networks or enable it as a production database administration tool.

OpenAPI documentation is available at `http://localhost:8080/swagger-ui/index.html`;
the JSON specification is at `http://localhost:8080/v3/api-docs`. Example
requests are collected in [`backend/requests.http`](./backend/requests.http).

The backend reads `DATABASE_URL`, `DATABASE_DRIVER`, `DATABASE_USERNAME`,
`DATABASE_PASSWORD`, and `APP_JWT_SECRET` from its environment. A random,
process-local JWT signing key is generated when `APP_JWT_SECRET` is omitted;
tokens then stop working after a backend restart. Set a private Base64-encoded
256-bit key for persistent deployments. A placeholder example is available in
[`backend/.env.example`](./backend/.env.example); Spring Boot does not load that
file automatically.
Login attempts are limited to 5 per account and 20 per direct client IP per
minute by default. When running behind a reverse proxy, configure the proxy and
network boundary so the backend receives trusted client addresses; untrusted
`X-Forwarded-For` values are not used for rate-limit identity.

The maximum payslip size defaults to 10 MB and can be changed with
`APP_PAYSLIP_MAX_FILE_SIZE_BYTES`. Leave this unset for the documented default.
Payslip provisioning is limited to 10 uploads per HR account per minute by
default; configure `APP_PAYSLIP_UPLOAD_RATE_LIMIT_CAPACITY` and
`APP_PAYSLIP_UPLOAD_RATE_LIMIT_REFILL_MINUTES` if the organization needs
different limits. The in-memory limiter applies per backend instance.

#### Securely create the initial System Administrator

On a new, empty database, configure these values in the same PowerShell window
before the first backend start: `APP_BOOTSTRAP_ADMIN_USERNAME`,
`APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD`,
`APP_BOOTSTRAP_ADMIN_EMPLOYEE_ID`, `APP_BOOTSTRAP_ADMIN_NAME`,
`APP_BOOTSTRAP_ADMIN_DEPARTMENT`, `APP_BOOTSTRAP_ADMIN_DESIGNATION`, and
`APP_BOOTSTRAP_ADMIN_JOINING_DATE` (format `YYYY-MM-DD`). The password must be
12–72 characters and is stored as a BCrypt hash. Keep it private and do not
commit it to `.env.example` or source control. Example profile values:

```powershell
$env:APP_BOOTSTRAP_ADMIN_USERNAME = "portal-admin"
$env:APP_BOOTSTRAP_ADMIN_EMAIL = "admin@example.org"
$env:APP_BOOTSTRAP_ADMIN_EMPLOYEE_ID = "ADM-0001"
$env:APP_BOOTSTRAP_ADMIN_NAME = "Portal Administrator"
$env:APP_BOOTSTRAP_ADMIN_DEPARTMENT = "IT Administration"
$env:APP_BOOTSTRAP_ADMIN_DESIGNATION = "System Administrator"
$env:APP_BOOTSTRAP_ADMIN_JOINING_DATE = "2026-01-01"
$securePassword = Read-Host "Initial administrator password" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
  $env:APP_BOOTSTRAP_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}
mvn spring-boot:run
```

The bootstrap user is created only when the user table is empty. Once created,
clear the bootstrap variables from the shell and use System Administrator
**User management** to create the rest of the accounts.

To generate a local signing key in PowerShell before starting the backend:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
mvn spring-boot:run
```

For PostgreSQL, set `DATABASE_URL` to a PostgreSQL JDBC URL, `DATABASE_DRIVER`
to `org.postgresql.Driver`, and provide the database username and password as
environment variables before starting the backend.

#### Leave management:

- `GET /api/leaves/balance` returns the authenticated employee's configured leave balances.
- `POST /api/leaves` submits a leave request using the authenticated employee identity.
- `GET /api/leaves/my?page=0&size=20` returns only the authenticated employee's paginated requests; optional `status` filtering is supported.
- `GET /api/leaves/pending?page=0&size=20` returns paginated requests available for the reviewer's role and reporting scope.
- `PATCH /api/leaves/{id}/decision` records an authorized approval or rejection.
- `PUT /api/leaves/balance` allows HR or system administrators to set an employee entitlement.

HR uses the active employee dropdown and centrally configured entitlement
values. The default allowed day choices are `0, 5, 10, 12, 15, 18, 20, 24, 30`;
configure `app.leave.entitlement-options` to match approved organizational
policy. The API validates the selected values. Entitlements are keyed by
employee profile and leave type, and cannot be lowered below used plus pending
leave.

System administrators can search, filter, sort, create, edit, activate and
deactivate accounts from **User management**. Manager assignments are validated
against active accounts with the `MANAGER` role. Managers cannot be deactivated
or demoted while employees remain assigned; reassign those employees first. The
last active System Administrator cannot be deactivated or demoted.

Leave duration currently counts **calendar days**, including both the start and
end dates. Pending requests reserve available days; approved requests move those
days into used leave. The local sample accounts are fictional and do not receive
fabricated leave balances. An HR administrator must configure an entitlement
before an employee can submit that leave type.

Authentication, access-denial, profile-address, and leave events are recorded in
the `audit_events` table with a request correlation ID. Address audit details
contain hashes rather than the address values themselves.

#### Employee profile endpoints:

- `GET /api/employees/me` returns the authenticated user's employment details and address.
- `PATCH /api/employees/me/address` updates only address fields. The authenticated
  username from the verified JWT determines ownership; client-supplied user IDs are
  not accepted.

Address updates require residential and mailing street, city, state, postal code,
and country fields. Address changes persist across logout/login and backend
restarts in the default file-backed database.

Payslip listing and protected PDF downloads are available through the
employee-only `/api/payslips` endpoints and **My payslips** in the employee UI.
Payslip metadata is stored in the database; PDF files are stored separately
under `APP_PAYSLIP_STORAGE_DIRECTORY` (default `./protected-payslips`, relative
to the backend working directory), outside the frontend's public assets. Each
download is scoped to the authenticated employee, returned as an attachment
with `Cache-Control: no-store`, and recorded in `audit_events`. Only trusted
HR administrators can provision payslips from **Payslip management** in the
HR workspace, or call `POST /api/hr/payslips` with multipart form fields
`employeeProfileId`, `payPeriodStart` (`YYYY-MM-DD`), `payPeriodEnd`
(`YYYY-MM-DD`), and `file`. The endpoint accepts only active employee profiles,
checks the PDF signature and maximum size, and rejects duplicate employee/pay
period records. System administrators and employees cannot use this endpoint.
The `storage_key` is a generated UUID-based `.pdf` filename stored inside the
configured directory; only that key, never an absolute path or uploaded
filename, is stored in the database. Do not place payslip files in a web-served
directory, and use fictional data for demonstrations and tests. Payslip
calculation and automatic PDF generation are not included; payroll must supply
the final PDF. Upload errors distinguish invalid input (400), missing or
inactive employee (404), duplicate period (409), oversized file (413), and
rate-limited HR uploads (429); unauthorized and forbidden requests return 401
and 403, respectively.

For a local API upload example, sign in as an HR administrator and use its
employee profile ID from `GET /api/hr/employees`:

```powershell
curl.exe -X POST "http://localhost:8080/api/hr/payslips" `
  -H "Authorization: Bearer $token" `
  -F "employeeProfileId=1" `
  -F "payPeriodStart=2030-01-01" `
  -F "payPeriodEnd=2030-01-31" `
  -F "file=@C:\path\to\fictional-payslip.pdf;type=application/pdf"
```

Successful requests return a `201 Created` response. The employee can then
sign in, open **My payslips**, and download that period's PDF. Do not use real
employee or payroll data in a demonstration.

All API error bodies include the request's `correlationId`; the same value is
returned in the `X-Correlation-ID` response header. The identifier is also
attached to persisted audit events when available.

Architecture, ER, and security notes are in [`ARCHITECTURE.md`](./ARCHITECTURE.md).
The four-role walkthrough is in [`DEMO_SCRIPT.md`](./DEMO_SCRIPT.md).
The runnable HTTP request collection is [`backend/requests.http`](./backend/requests.http).

Run the frontend unit and browser-level end-to-end tests with:

```powershell
cd frontend
npm test
npx playwright install chromium
npm run test:e2e
```

Run the backend tests, including the 25-concurrent-user paginated-list p95 check,
with `cd backend; mvn test`. The performance test creates synthetic users and
records across employee, manager, HR, and system-admin list endpoints locally.
It targets the 2-second p95 requirement; use a production-like database and
deployment for capacity certification.

The browser tests start an isolated in-memory backend with fictional demo
accounts and use ports `18080` and `13000`; they do not reuse the development
database or the usual application ports.
Latest verification results and unverified checks are summarized in
[`TEST_REPORT.md`](./TEST_REPORT.md).

---

### 5. Integration Guide for Other Team Members

The API uses the verified principal as the identity for profile, address, leave,
and manager-scope operations. Add new business endpoints through the existing
controller/service/repository patterns and protect them with server-side role
checks; do not rely on the role-aware frontend navigation as authorization.

The HR payslip upload is the payroll provisioning boundary; employee access is
separate and cannot upload or select another employee's records.
