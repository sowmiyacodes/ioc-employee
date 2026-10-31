import { expect, test } from '@playwright/test';

const demoPassword = 'E2E-Portal-Demo-Password-12!';

async function signIn(page: import('@playwright/test').Page, username: string) {
  await page.goto('/login');
  await page.getByLabel('Username or work email').fill(username);
  await page.locator('#password').fill(demoPassword);
  await page.getByRole('button', { name: 'Sign in securely' }).click();
}

test('employee saves an address that is still present after signing in again', async ({ page }) => {
  await signIn(page, 'employee1');
  await page.getByRole('button', { name: 'My profile' }).click();
  await page.getByRole('button', { name: 'Edit address' }).click();

  const address = {
    residentialStreet: '99 Fictional Road',
    residentialCity: 'Example City',
    residentialState: 'Example State',
    residentialPostalCode: '12345',
    residentialCountry: 'Example Country',
    mailingStreet: '99 Fictional Road',
    mailingCity: 'Example City',
    mailingState: 'Example State',
    mailingPostalCode: '12345',
    mailingCountry: 'Example Country',
  };
  for (const [field, value] of Object.entries(address)) {
    await page.locator(`#${field}`).fill(value);
  }
  await page.getByRole('button', { name: 'Save address' }).click();
  await expect(page.getByRole('status')).toContainText('Your address has been saved.');

  await page.getByRole('button', { name: 'Sign out' }).first().click();
  await signIn(page, 'employee1');
  await page.getByRole('button', { name: 'My profile' }).click();
  await expect(page.getByText('99 Fictional Road', { exact: true }).first()).toBeVisible();
});

test('employee-submitted markup is displayed as text instead of executable HTML', async ({ page }) => {
  const markup = '<img src=x onerror="document.body.dataset.xss=\'executed\'">';
  await signIn(page, 'employee1');
  await page.getByRole('button', { name: 'My profile' }).click();
  await page.getByRole('button', { name: 'Edit address' }).click();
  await page.locator('#residentialStreet').fill(markup);
  await page.getByRole('button', { name: 'Save address' }).click();

  await expect(page.getByRole('alert')).toContainText('Residential street may contain');
  await expect(page.locator('img[src="x"]')).toHaveCount(0);
});

test('HR provisions a payslip that the employee can download', async ({ page }) => {
  await signIn(page, 'hradmin1');
  await page.getByRole('button', { name: 'Payslip management' }).click();
  await page.locator('select').selectOption({ label: 'Alex Johnson · EMP-0001' });
  await page.locator('input[type="date"]').nth(0).fill('2031-01-01');
  await page.locator('input[type="date"]').nth(1).fill('2031-01-31');
  await page.locator('input[type="file"]').setInputFiles({
    name: 'fictional-payslip.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.4\nFictional employee payslip\n%%EOF', 'utf8'),
  });
  await page.getByRole('button', { name: 'Upload payslip' }).click();
  await expect(page.getByRole('status')).toContainText('The employee can now find it under My payslips.');

  await page.getByRole('button', { name: 'Sign out' }).first().click();
  await signIn(page, 'employee1');
  await page.getByRole('button', { name: 'My payslips' }).click();
  const downloadWaiter = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Download PDF' }).click();
  const download = await downloadWaiter;
  expect(download.suggestedFilename()).toBe('payslip-2031-01.pdf');
});
