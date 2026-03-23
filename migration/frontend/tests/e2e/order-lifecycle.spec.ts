import { expect, test } from "@playwright/test";

test.describe.configure({ mode: "serial" });

test("operators can create, update, pay, ship, and complete an order", async ({ page }) => {
  await page.goto("/orders/new");

  await page.getByRole("spinbutton").first().fill("2");
  await page.getByRole("button", { name: "Create Order" }).click();

  await expect(page).toHaveURL(/\/orders\/[^/]+$/);
  await expect(page.getByText("PENDING")).toBeVisible();

  await page.getByLabel("Notes").last().fill("Priority handling requested");
  await page.getByRole("button", { name: "Save Updates" }).click();
  await expect(page.getByText("Priority handling requested")).toBeVisible();

  await page.getByRole("button", { name: "Mark Paid" }).click();
  await page.getByLabel("Payment reference").fill("PAY-E2E-100");
  await page.getByLabel("Amount").fill("99.80");
  await page.getByRole("button", { name: "Confirm Payment" }).click();
  await expect(page.getByText("PAID")).toBeVisible();
  await expect(page.getByText("PAY-E2E-100")).toBeVisible();

  await page.getByRole("button", { name: "Ship Order" }).click();
  await page.getByLabel("Tracking number").fill("TRACK-E2E-100");
  await page.getByRole("button", { name: "Confirm Shipment" }).click();
  await expect(page.getByText("SHIPPED")).toBeVisible();
  await expect(page.getByText("TRACK-E2E-100")).toBeVisible();

  await page.getByRole("button", { name: "Complete Order" }).click();
  await expect(page.getByText("COMPLETED")).toBeVisible();
});

test("operators can cancel a seeded pending order", async ({ page }) => {
  await page.goto("/orders");
  await page.getByRole("link", { name: "ORD-001000" }).click();

  await page.getByRole("button", { name: "Cancel" }).click();
  await page.getByLabel("Reason").fill("Customer changed mind");
  await page.getByRole("button", { name: "Confirm Cancellation" }).click();

  await expect(page.getByText("CANCELLED")).toBeVisible();
  await expect(page.getByText("Customer changed mind")).toBeVisible();
});

test("operators can refund a completed order without changing its lifecycle", async ({ page }) => {
  await page.goto("/orders");
  await page.getByRole("link", { name: "ORD-001005" }).click();

  await page.getByRole("button", { name: "Refund" }).click();
  await page.getByLabel("Amount").fill("10.00");
  await page.getByLabel("Reason").fill("Courtesy adjustment");
  await page.getByRole("button", { name: "Submit Refund" }).click();

  await expect(page.getByText("COMPLETED")).toBeVisible();
  await expect(page.getByText("Courtesy adjustment")).toBeVisible();
});
