import { defineConfig } from '@playwright/test';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendDirectory = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: false,
  workers: 1,
  reporter: 'list',
  testMatch: '**/*.e2e.ts',
  use: {
    baseURL: 'http://127.0.0.1:13000',
    headless: true,
  },
  webServer: [
    {
      command: 'mvn spring-boot:run',
      cwd: resolve(frontendDirectory, '../backend'),
      url: 'http://127.0.0.1:18080/api/auth/health',
      timeout: 120_000,
      reuseExistingServer: false,
      env: {
        DATABASE_URL: 'jdbc:h2:mem:essp_playwright;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
        ESSP_SEED_DEMO_DATA: 'true',
        APP_DEMO_PASSWORD: 'E2E-Portal-Demo-Password-12!',
        APP_JWT_SECRET: Buffer.alloc(32, 1).toString('base64'),
        APP_PAYSLIP_STORAGE_DIRECTORY: 'target/e2e-payslips',
        APP_SECURITY_CORS_ALLOWED_ORIGINS: 'http://127.0.0.1:13000',
        SERVER_PORT: '18080',
      },
    },
    {
      command: 'npm run dev -- --host 127.0.0.1 --port 13000 --strictPort',
      cwd: frontendDirectory,
      url: 'http://127.0.0.1:13000',
      timeout: 60_000,
      reuseExistingServer: false,
      env: {
        ESSP_BACKEND_URL: 'http://127.0.0.1:18080',
      },
    },
  ],
});
