"use server";

import { redirect } from "next/navigation";
import {
  createOpportunity,
  createOpportunityQuote,
  saveOpportunityBrief,
} from "@/lib/leadflow";

function requiredText(formData: FormData, key: string): string {
  const value = formData.get(key);

  if (typeof value !== "string" || value.trim().length === 0) {
    throw new Error(`Missing required field: ${key}`);
  }

  return value.trim();
}

function optionalText(formData: FormData, key: string): string | undefined {
  const value = formData.get(key);

  if (typeof value !== "string") {
    return undefined;
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function parseRequiredNumber(value: string, key: string): number {
  const parsed = Number(value);

  if (!Number.isFinite(parsed)) {
    throw new Error(`Invalid number field: ${key}`);
  }

  return parsed;
}

export async function createLeadAction(formData: FormData) {
  const opportunity = await createOpportunity({
    firstName: requiredText(formData, "firstName"),
    lastName: requiredText(formData, "lastName"),
    email: requiredText(formData, "email"),
    companyName: optionalText(formData, "companyName"),
    title: optionalText(formData, "title"),
  });

  redirect(`/leads/${opportunity.partyId}`);
}

function readLineInputs(formData: FormData) {
  const descriptions = formData.getAll("lineDescription");
  const productIds = formData.getAll("productId");
  const quantities = formData.getAll("quantity");
  const unitPrices = formData.getAll("unitPrice");

  const length = Math.max(
    descriptions.length,
    productIds.length,
    quantities.length,
    unitPrices.length,
  );

  const lines = [];

  for (let index = 0; index < length; index += 1) {
    const description = String(descriptions[index] ?? "").trim();
    const productId = String(productIds[index] ?? "").trim();
    const quantity = String(quantities[index] ?? "").trim();
    const unitPrice = String(unitPrices[index] ?? "").trim();

    const isBlank =
      description.length === 0 &&
      productId.length === 0 &&
      quantity.length === 0 &&
      unitPrice.length === 0;

    if (isBlank) {
      continue;
    }

    if (description.length === 0 || quantity.length === 0 || unitPrice.length === 0) {
      throw new Error("Each line needs an item description, quantity, and unit price.");
    }

    lines.push({
      description,
      productId: productId.length > 0 ? productId : undefined,
      quantity: parseRequiredNumber(quantity, `quantity-${index}`),
      unitPrice: parseRequiredNumber(unitPrice, `unitPrice-${index}`),
    });
  }

  if (lines.length === 0) {
    throw new Error("Add at least one item before saving the brief.");
  }

  return lines;
}

export async function saveBriefAction(formData: FormData) {
  const partyId = requiredText(formData, "partyId");
  await saveOpportunityBrief(partyId, {
    title: requiredText(formData, "title"),
    notes: optionalText(formData, "notes"),
    lines: readLineInputs(formData),
  });

  redirect(`/leads/${partyId}?view=brief#brief`);
}

export async function createQuoteAction(formData: FormData) {
  const partyId = requiredText(formData, "partyId");
  await createOpportunityQuote(partyId);
  redirect(`/leads/${partyId}?view=quote#quote`);
}
