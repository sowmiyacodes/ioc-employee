# Five-minute demo script

Use only a disposable local database, fictional employee records, and a private
demo password. Set `ESSP_SEED_DEMO_DATA=true` and `APP_DEMO_PASSWORD` before
starting the backend, then run the frontend using the README instructions.

1. **HR administrator (about 90 seconds):** sign in as `hradmin1`; configure
   an employee's leave entitlement and use **Payslip management** to upload a
   fictional PDF with its pay period.
2. **Employee (about 90 seconds):** sign in as `employee1`; show the employee
   dashboard, profile, address update, leave balances, and leave request
   history. Open **My payslips** and download the PDF HR provisioned.
3. **Manager (about 60 seconds):** sign in as `manager1`; show team-scoped
   pending requests and approve/reject a request from a direct report. Explain
   that a manager cannot decide requests outside their reporting scope.
4. **System administrator (about 60 seconds):** sign in as `sysadmin1`; show
   user management and account status controls. Confirm that the HR-only
   payslip upload and employee-only payslip list are denied to this role.
5. **Security evidence (about 30 seconds):** point to the automated tests for
   cross-employee payslip access, missing files, role denial, correlation IDs,
   leave ownership/scope, and audit events. Avoid displaying real personal or
   payroll data.
