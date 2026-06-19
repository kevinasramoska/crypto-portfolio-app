import { expect, test } from "@playwright/test";

test("registers, logs in, creates a transaction, and shows dashboard data", async ({ page }) => {
  const uniqueSuffix = Date.now();
  const email = `e2e-${uniqueSuffix}@example.com`;
  const password = "Password123!";

  await page.goto("/register");

  await page.getByLabel("First name").fill("Satoshi");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByLabel("Confirm password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Register" }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("button", { name: "Logout" })).toBeVisible();

  await page.getByRole("button", { name: "Logout" }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Login" }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText("Portfolio summary")).toBeVisible();

  await page.getByLabel("Coin").selectOption("BTC");
  await expect(page.getByLabel("Symbol")).toHaveValue("BTC");
  await expect(page.getByLabel("Name")).toHaveValue("Bitcoin");

  await page.getByLabel("Quantity").fill("1");
  await page.getByLabel("Price USD").fill("50000");
  await page.getByRole("button", { name: "Save transaction" }).click();

  await expect(page.getByTestId("holding-row-BTC")).toContainText("Bitcoin");
  await expect(page.getByTestId("holding-row-BTC")).toContainText("1");
  await expect(page.getByTestId("holding-row-BTC")).toContainText("$50,000.00");
  await expect(page.getByTestId("holding-row-BTC")).toContainText("$60,000.00");
  await expect(page.getByTestId("transaction-row-BTC-buy")).toContainText("BUY");
  await expect(page.getByTestId("transaction-row-BTC-buy")).toContainText("$50,000.00");

  await expect(page.getByTestId("portfolio-card-current-value")).toContainText("$60,000.00");
  await expect(page.getByTestId("portfolio-card-invested")).toContainText("$50,000.00");
  await expect(page.getByTestId("portfolio-card-unrealised-p-l")).toContainText("$10,000.00");
  await expect(page.getByTestId("portfolio-card-total-p-l")).toContainText("$10,000.00");
  await expect(page.getByTestId("portfolio-card-assets")).toContainText("1");
});
