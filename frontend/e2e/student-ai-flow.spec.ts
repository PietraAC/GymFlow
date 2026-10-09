import { expect, test } from '@playwright/test';

test('student creates a draft, reviews the AI proposal and applies it', async ({ page }) => {
  const captureDocs = process.env['GYMFLOW_CAPTURE_DOCS'] === 'true';
  const requestedDays = captureDocs ? 5 : 3;
  const expectedProvider = process.env['GYMFLOW_AI_PROVIDER'] === 'gemini' ? 'Proposta do Gemini' : 'Proposta em modo demo';

  await page.goto('/');
  if (captureDocs) {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.screenshot({ path: '../docs/assets/gymflow-home-stage6.png', fullPage: true });
  }
  await page.getByRole('button', { name: 'Começar agora', exact: false }).click();

  await page.locator('#username').fill('aluna.demo');
  await page.locator('#password').fill('gymflow-student-a');
  await page.locator('#kc-login').click();
  await expect(page).toHaveURL('http://localhost:4200/');
  await expect(page.getByRole('button', { name: 'Sair', exact: true })).toBeVisible();

  await page.getByRole('link', { name: 'Meu perfil', exact: true }).click();
  await page.getByLabel('Objetivo').selectOption('STRENGTH');
  await page.getByLabel('Experiência').selectOption('BEGINNER');
  await page.getByLabel('Dias por semana').fill(String(requestedDays));
  await page.getByLabel('Duração por sessão (min)').fill('45');
  await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click();
  await expect(page.getByText('Perfil salvo.', { exact: true })).toBeVisible();

  await page.getByRole('link', { name: 'Meus treinos', exact: true }).click();
  await page.getByRole('link', { name: 'Novo plano', exact: true }).click();
  await expect(page.getByLabel('Unidade')).not.toHaveValue('');
  await page.getByRole('button', { name: 'Continuar', exact: false }).click();

  const planName = captureDocs ? 'Hipertrofia - 5 dias' : `Plano E2E IA ${Date.now()}`;
  await page.getByLabel('Nome do plano').fill(planName);
  await page.getByLabel('Objetivo').selectOption('STRENGTH');
  await page.getByLabel('Dias por semana').fill(String(requestedDays));
  await page.getByRole('button', { name: 'Criar rascunho', exact: true }).click();
  await expect(page.getByRole('heading', { name: planName, exact: true })).toBeVisible({ timeout: 15_000 });

  await page.getByRole('button', { name: 'Completar com IA', exact: true }).click();
  const generate = page.getByRole('button', { name: /Gerar (plano inicial|complemento)/ });
  await expect(generate).toBeEnabled();
  await generate.click();

  await expect(page.getByText(expectedProvider, { exact: true })).toBeVisible({ timeout: 30_000 });
  await expect(page.getByText('Montei a estrutura completa do treino com base no objetivo, na frequência semanal e no catálogo da unidade.', { exact: true })).toBeVisible();
  await expect(page.getByText('Revise os exercícios e ajuste cargas manualmente antes de ativar o plano.', { exact: true })).toBeVisible();
  if (captureDocs) {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.screenshot({ path: '../docs/assets/gymflow-ai-workout-completion.png' });
  }
  await page.getByRole('button', { name: 'Aplicar ao rascunho', exact: true }).click();
  await expect(page.getByText('Sugestão aplicada ao rascunho para sua revisão.', { exact: true })).toBeVisible({ timeout: 30_000 });
  await expect(page.locator('.day-tabs button')).toHaveCount(requestedDays);
  await expect(page.locator('.exercise-card').first()).toBeVisible();

});
