import { expect, test } from "@playwright/test";

test("salesperson can move an opportunity from creation to quote handoff", async ({
  page,
}) => {
  const suffix = Date.now().toString().slice(-6);

  await page.goto("/leads");

  await page.getByLabel("First name").fill(`Codex${suffix}`);
  await page.getByLabel("Last name").fill("Buyer");
  await page.getByLabel("Email").fill(`codex+${suffix}@example.com`);
  await page.getByLabel("Company").fill(`Codex Logistics ${suffix}`);
  await page.getByLabel("Title").fill("Warehouse Director");
  await page.getByRole("button", { name: "Save Lead" }).click();

  await expect(page).toHaveURL(/\/leads\/.+/);
  await expect(page.getByText("Opportunity summary")).toBeVisible();

  await page.getByLabel("Brief title").fill(`Warehouse refresh ${suffix}`);
  await page
    .getByLabel("Customer notes")
    .fill("Modernize handheld picking gear for a new warehouse aisle.");
  await page
    .getByLabel("Item description")
    .fill("Two round gizmos for the launch pod");
  await page.getByLabel("Product").fill("Round Gizmo (GZ-2644)");
  await page.getByLabel("Quantity").fill("2");
  await page.getByLabel("Unit price").fill("24.50");
  await page.getByRole("button", { name: "Add line" }).click();
  await page.getByLabel("Item description").nth(1).fill("Backup calibration pack");
  await page.getByLabel("Product").nth(1).fill("Micro Chrome Widget (WG-1111)");
  await page.getByLabel("Quantity").nth(1).fill("1");
  await page.getByLabel("Unit price").nth(1).fill("12.00");
  await page.getByRole("button", { name: "Save brief" }).click();

  await expect(page).toHaveURL(/\/leads\/.+/);
  await expect(page.getByRole("button", { name: "Create Quote" })).toBeVisible();

  await page.getByRole("button", { name: "Create Quote" }).click();
  await expect(page).toHaveURL(/\/leads\/.+/);
  await expect(page.getByRole("link", { name: "Review quote" })).toBeVisible();
  await expect(page.getByText("This brief is locked")).toBeVisible();
  await expect(page.getByRole("button", { name: "Create Draft Order" })).toHaveCount(0);

  await page.getByRole("link", { name: "Review quote" }).click();
  await expect(page).toHaveURL(/\/quotes\/.+/);
  await expect(page.getByText("Quoted items")).toBeVisible();
  await expect(page.getByRole("link", { name: "Open quote record" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Create Draft Order" })).toHaveCount(0);
});
