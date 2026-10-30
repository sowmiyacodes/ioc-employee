# Employee Self-Service Portal (ESSP)
## Member 64: Authentication, Authorization & Security Module

This module implements the complete authentication backend, security architecture, and frontend login/session flow for the **Employee Self-Service Portal**.

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
- **Audit Logging:** Dedicated `SecurityAuditService` capturing login success, failures, logout, and access violations
- **Frontend Stack:** React 18, TypeScript, Vite

---

### 2. Available Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Authenticates credentials and returns JWT token |
| `POST` | `/api/auth/logout` | Authenticated | Invalidates JWT token and clears session |
| `GET` | `/api/auth/me` | Authenticated | Retrieves current authenticated profile & role |
| `GET` | `/api/employee/dashboard` | EMPLOYEE+ | Employee resource area |
| `GET` | `/api/manager/team-overview` | MANAGER+ | Manager management area |
| `GET` | `/api/hr/staff-directory` | HR_ADMIN+ | HR administrative area |
| `GET` | `/api/admin/system-status` | SYSTEM_ADMIN | System admin configuration area |

---

### 3. Pre-Seeded Accounts for Testing

All seeded accounts have the compliant password: `Password1234!` (13 characters)

1. **Employee:** `employee1` or `employee@essp.local`
2. **Manager:** `manager1` or `manager@essp.local`
3. **HR Admin:** `hradmin1` or `hradmin@essp.local`
4. **System Admin:** `sysadmin1` or `sysadmin@essp.local`

---

### 4. Running the Application

#### Backend (Spring Boot):
```bash
# In Employee_Self-Service_Portal root
mvn clean test
mvn spring-boot:run
```

#### Frontend (React / Vite):
```bash
cd frontend
npm install
npm run dev
```

---

### 5. Integration Guide for Other Team Members

#### For Member 70 (Employee Profile & Address):
Secure your controller endpoints using `@PreAuthorize`:
```java
@PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN')")
@GetMapping("/api/employees/{id}")
public ResponseEntity<EmployeeProfile> getProfile(@PathVariable Long id) { ... }
```

#### For Member 96 (Leave Management):
Protect leave submissions and approvals:
```java
@PreAuthorize("hasRole('EMPLOYEE')")
@PostMapping("/api/leaves")
public ResponseEntity<?> submitLeave(...) { ... }

@PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
@PutMapping("/api/leaves/{id}/approve")
public ResponseEntity<?> approveLeave(...) { ... }
```

#### For Member 114 (Payslip & Shared Audit):
Integrate with `SecurityAuditService` or log payslip downloads:
```java
@PreAuthorize("hasAnyRole('EMPLOYEE', 'HR_ADMIN', 'SYSTEM_ADMIN')")
@GetMapping("/api/payslips/{id}")
public ResponseEntity<?> downloadPayslip(...) { ... }
```
