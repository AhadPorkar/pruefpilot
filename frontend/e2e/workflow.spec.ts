import { test, expect } from '@playwright/test';
const assetId='00000000-0000-4000-8000-000000000201';
test.beforeEach(async({page})=>{await page.goto('/?demo=1#/overview');await expect(page.locator('pruef-pilot')).not.toHaveAttribute('aria-busy','true');await expect(page.locator('[data-change="role"]')).toBeVisible();});
test('English default, all primary screens, no horizontal overflow',async({page})=>{
  await expect(page.locator('html')).toHaveAttribute('lang','en');
  for(const route of ['overview','assets','inspections','schedules','actions','reports','templates','team','audit','help']){
    await page.goto('/?demo=1#/'+route);await expect(page.locator('main')).toBeVisible();
    await expect.poll(()=>page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  }
});
test('language and theme persist, passport QR prints real identification',async({page})=>{
  await page.locator('[data-action="language"]').click();await expect(page.locator('html')).toHaveAttribute('lang','de');
  await page.locator('[data-action="theme"]').click();await page.reload();
  await expect(page.locator('html')).toHaveAttribute('lang','de');await expect(page.locator('html')).toHaveAttribute('data-theme','dark');
  await page.goto('/?demo=1#/asset/'+assetId);await expect(page.locator('main')).toContainText('PP-BER-001');
  await page.locator('[data-action="qr"]').first().click();await expect(page.locator('dialog svg[role="img"]')).toBeVisible();
});
test('repair evidence then independent supervisor closure',async({page})=>{
  await page.locator('[data-change="role"]').selectOption('TECHNICIAN');
  await page.goto('/?demo=1#/actions');
  // A full navigation resets the explicit in-memory preview, so choose role after navigation.
  await page.locator('[data-change="role"]').selectOption('TECHNICIAN');
  await page.locator('[data-action="finding"][data-id="00000000-0000-4000-8000-000000000801"]').click();
  await page.locator('dialog input[type="file"]').setInputFiles({name:'repair.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF-1.7\nFictional test evidence')});
  await expect(page.locator('dialog')).toContainText('repair.pdf');
  await page.locator('form[data-form="complete-finding"] [name="note"]').fill('Replaced test connection and checked function.');
  await page.locator('form[data-form="complete-finding"] button[type="submit"]').click();await expect(page.locator('dialog')).not.toBeVisible();
  await page.locator('[data-change="role"]').selectOption('SUPERVISOR');
  await page.locator('[data-action="finding"][data-id="00000000-0000-4000-8000-000000000801"]').click();
  await page.locator('form[data-form="verify-finding"] [name="note"]').fill('Independently checked operation and original evidence.');
  await page.locator('form[data-form="verify-finding"] button[value="approve"]').click();
  await expect(page.locator('dialog')).not.toBeVisible();await expect(page.locator('main')).toContainText('Closed');
});
test('viewer cannot see management forms and report downloads',async({page})=>{
  await page.locator('[data-change="role"]').selectOption('VIEWER');
  await page.evaluate(()=>{location.hash='#/assets';});
  await expect(page.locator('[data-action="new-asset"]')).toHaveCount(0);
  await page.evaluate(()=>{location.hash='#/reports';});
  const downloadPromise=page.waitForEvent('download');await page.locator('#export-form button[type="submit"]').click();
  const download=await downloadPromise;expect(download.suggestedFilename()).toMatch(/PruefPilot-.*-en.zip/);
});
