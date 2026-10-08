import { expect, test } from '@playwright/test';

test('student creates a draft and lets AI complete the weekly workout', async ({ page }) => {
  const captureDocs = process.env['GYMFLOW_CAPTURE_DOCS'] === 'true';
  const requestedDays = captureDocs ? 5 : 3;
  const expectedProvider = process.env['GYMFLOW_AI_PROVIDER'] === 'gemini' ? 'Gemini' : 'Modo demo';
  await page.goto('/');
  await page.getByRole('button', { name: 'Entrar no GymFlow', exact: true }).click();

  await page.locator('#username').fill('aluna.demo');
  await page.locator('#password').fill('gymflow-student-a');
  await page.locator('#kc-login').click();
  await expect(page).toHaveURL('http://localhost:4200/');
  await expect(page.getByRole('button', { name: 'Sair', exact: true })).toBeVisible();

  if (captureDocs) {
    await page.setViewportSize({ width: 1862, height: 923 });
    await page.screenshot({ path: '../docs/assets/gymflow-home-stage6.png', fullPage: true });
  }

  await page.getByRole('link', { name: 'Meu perfil', exact: true }).click();
  await page.getByLabel('Objetivo').selectOption('STRENGTH');
  await page.getByLabel('Experiência').selectOption('BEGINNER');
  await page.getByLabel('Dias por semana').fill(String(requestedDays));
  await page.getByLabel('Duração por sessão (min)').fill('45');
  await page.getByRole('button', { name: 'Salvar perfil', exact: true }).click();
  await expect(page.getByText('Perfil salvo.', { exact: true })).toBeVisible();

  await page.getByRole('link', { name: 'Meus treinos', exact: true }).click();
  await expect(page.getByLabel('Unidade')).not.toHaveValue('');
  const completedDocumentedPlan = page.locator('.list-item')
    .filter({ hasText: 'Hipertrofia - 5 dias' })
    .filter({ hasText: '5 de 5 dias' })
    .first();
  if (captureDocs && await completedDocumentedPlan.count()) {
    await completedDocumentedPlan.getByRole('link', { name: 'Abrir', exact: true }).click();
    await expect(page.locator('.day')).toHaveCount(5);
    await page.getByRole('button', { name: 'Recolher todos', exact: true }).click();
    await page.setViewportSize({ width: 1920, height: 955 });
    await page.locator('.topbar').evaluate(element => element.setAttribute('style', 'visibility:hidden'));
    await page.evaluate(() => window.scrollTo(0, 330));
    await page.screenshot({ path: '../docs/assets/gymflow-ai-workout-completion.png' });
    return;
  }
  const planName = captureDocs
    ? 'Hipertrofia - 5 dias'
    : `Plano E2E IA ${Date.now()}`;
  await page.getByLabel('Nome').fill(planName);
  await page.getByLabel('Objetivo').selectOption('STRENGTH');
  await page.getByLabel('Dias por semana').fill(String(requestedDays));
  await page.getByRole('button', { name: 'Criar rascunho', exact: true }).click();
  await expect(page.getByRole('heading', { name: planName, exact: true })).toBeVisible({ timeout: 15_000 });

  const addSuggestion = page.getByRole('button', { name: 'Montar meu treino com IA', exact: true });
  await expect(addSuggestion).toBeEnabled();
  await addSuggestion.click();

  await expect(page.getByText('Treino completado pela IA e mantido como rascunho para sua revisão.', { exact: true }))
    .toBeVisible({ timeout: 30_000 });
  await expect(page.getByText(expectedProvider, { exact: true })).toBeVisible();
  await expect(page.locator('.day')).toHaveCount(requestedDays);
  await expect(page.locator('.item').first()).toBeVisible();

  if (captureDocs) {
    await page.getByRole('button', { name: 'Recolher todos', exact: true }).click();
    await page.setViewportSize({ width: 1920, height: 955 });
    await page.locator('.topbar').evaluate(element => element.setAttribute('style', 'visibility:hidden'));
    await page.evaluate(() => window.scrollTo(0, 330));
    await page.screenshot({ path: '../docs/assets/gymflow-ai-workout-completion.png' });
  }
});
