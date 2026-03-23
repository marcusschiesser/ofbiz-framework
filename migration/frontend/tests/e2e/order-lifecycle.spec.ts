import { expect, test, type Page } from "@playwright/test";

test.describe.configure({ mode: "serial" });

function orderStatusBadge(page: Page, status: string) {
  return page.getByText(new RegExp(`^${status}$`)).first();
}

async function createOrder(page: Page, quantity: number) {
  await page.goto("/orders/new");
  await page.locator('input[name="qty-00000000-0000-0000-0000-000000000201"]').fill(String(quantity));
  await page.getByRole("button", { name: "Create Order" }).click();
  await expect(page).toHaveURL(/\/orders\/[^/]+$/);
}

test("operators can create, update, pay, ship, and complete an order", async ({ page }) => {
  await createOrder(page, 2);
  await expect(orderStatusBadge(page, "PENDING")).toBeVisible();
  await expect(page.getByText("v0")).toBeVisible();

  await page.getByLabel("Notes").last().fill("Priority handling requested");
  await page.getByRole("button", { name: "Save Updates" }).click();
  await expect(page.getByText("v1")).toBeVisible();
  await expect(page.getByLabel("Notes").last()).toHaveValue("Priority handling requested");
  await expect(
    page.getByRole("definition").filter({ hasText: "Priority handling requested" }),
  ).toBeVisible();

  await page.getByRole("button", { name: "Mark Paid" }).click();
  await page.getByLabel("Payment reference").fill("PAY-E2E-100");
  await page.getByLabel("Amount").fill("99.80");
  await page.getByRole("button", { name: "Confirm Payment" }).click();
  await expect(orderStatusBadge(page, "PAID")).toBeVisible();
  await expect(page.getByText("PAY-E2E-100")).toBeVisible();

  await page.getByRole("button", { name: "Ship Order" }).click();
  await page.getByLabel("Tracking number").fill("TRACK-E2E-100");
  await page.getByRole("button", { name: "Confirm Shipment" }).click();
  await expect(orderStatusBadge(page, "SHIPPED")).toBeVisible();
  await expect(page.getByText("TRACK-E2E-100")).toBeVisible();

  await page.getByRole("button", { name: "Complete Order" }).click();
  await expect(orderStatusBadge(page, "COMPLETED")).toBeVisible();
});

test("operators can cancel a seeded pending order", async ({ page }) => {
  await createOrder(page, 1);
  await page.getByRole("button", { name: "Cancel" }).click();
  await page.getByLabel("Reason").fill("Customer changed mind");
  await page.getByRole("button", { name: "Confirm Cancellation" }).click();

  await expect(orderStatusBadge(page, "CANCELLED")).toBeVisible();
  await expect(page.getByText("Customer changed mind")).toBeVisible();
});

test("operators can refund a completed order without changing its lifecycle", async ({ page }) => {
  await createOrder(page, 1);

  await page.getByRole("button", { name: "Mark Paid" }).click();
  await page.getByLabel("Payment reference").fill("PAY-E2E-REFUND");
  await page.getByLabel("Amount").fill("49.90");
  await page.getByRole("button", { name: "Confirm Payment" }).click();

  await page.getByRole("button", { name: "Ship Order" }).click();
  await page.getByLabel("Tracking number").fill("TRACK-E2E-REFUND");
  await page.getByRole("button", { name: "Confirm Shipment" }).click();

  await page.getByRole("button", { name: "Complete Order" }).click();

  await page.getByRole("button", { name: "Refund" }).click();
  await page.getByLabel("Amount").fill("10.00");
  await page.getByLabel("Reason").fill("Courtesy adjustment");
  await page.getByRole("button", { name: "Submit Refund" }).click();

  await expect(orderStatusBadge(page, "COMPLETED")).toBeVisible();
  await expect(page.getByText("Courtesy adjustment").first()).toBeVisible();
});
