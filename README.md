# Employee Self-Service Portal (ESSP)
The portal combines Spring Boot authentication and authorization, employee profile
and address management, leave management, and a React self-service interface.

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
npm install
npm run dev
```

The frontend is available at `http://localhost:3000`. Login redirects each
authenticated role to its own dashboard: employee, manager, HR administrator,
or system administrator. Frontend navigation is a usability feature; backend
authorization independently protects API operations.

#### Database (H2):

- JDBC URL: `jdbc:h2:file:./data/essp_db;DB_CLOSE_ON_EXIT=FALSE` (run the backend from `backend`)
- Username: `sa`
- Password: leave blank
- H2 console: `http://localhost:8080/h2-console` (available while the backend is running)

The default H2 database is file-backed under `backend/data`, so application data
survives backend restarts. Flyway applies the versioned schema migrations, and
Hibernate validates the schema without recreating it. Back up the database files
before moving or deleting them.

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
the `audit_events` table. Address audit details contain hashes rather than the
address values themselves.

#### Employee profile endpoints:

- `GET /api/employees/me` returns the authenticated user's employment details and address.
- `PATCH /api/employees/me/address` updates only address fields. The authenticated
  username from the verified JWT determines ownership; client-supplied user IDs are
  not accepted.

Address updates require residential and mailing street, city, state, postal code,
and country fields. Address changes persist across logout/login and backend
restarts in the default file-backed database.

The current repository does not include a payslip module or payslip API.
Accordingly, payslip access and the payslip-protection acceptance criterion are
not implemented or verified here.

---

### 5. Integration Guide for Other Team Members

The API uses the verified principal as the identity for profile, address, leave,
and manager-scope operations. Add new business endpoints through the existing
controller/service/repository patterns and protect them with server-side role
checks; do not rely on the role-aware frontend navigation as authorization.

The current repository does not contain a payslip module, payslip data model,
or download API. Payslip access control and downloads therefore remain
unimplemented and require a separate payslip feature before that acceptance
criterion can be verified.
