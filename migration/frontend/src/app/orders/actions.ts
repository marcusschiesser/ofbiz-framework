"use server";

import { updateTag } from "next/cache";
import { redirect } from "next/navigation";
import { mutateOrder, patchOrder } from "@/lib/api/client";

function extractItems(formData: FormData) {
  const items: Array<{ productId: string; quantity: number }> = [];

  for (const [key, value] of formData.entries()) {
    if (!key.startsWith("qty-")) continue;
    const quantity = Number(value);
    if (!Number.isFinite(quantity) || quantity <= 0) continue;
    items.push({
      productId: key.replace("qty-", ""),
      quantity,
    });
  }

  return items;
}

function invalidate(orderId?: string) {
  updateTag("orders");
  if (orderId) updateTag(`order:${orderId}`);
}

export async function createOrderAction(formData: FormData) {
  const items = extractItems(formData);
  const order = await mutateOrder("/api/orders", {
    customerId: formData.get("customerId"),
    notes: formData.get("notes"),
    actor: formData.get("actor") ?? "frontend-operator",
    items,
  });

  invalidate(order.id);
  redirect(`/orders/${order.id}`);
}

export async function updateOrderAction(orderId: string, formData: FormData) {
  await patchOrder(orderId, {
    version: Number(formData.get("version")),
    actor: formData.get("actor") ?? "frontend-operator",
    notes: formData.get("notes"),
    items: extractItems(formData),
    reason: "Order updated from lifecycle view",
  });

  invalidate(orderId);
  redirect(`/orders/${orderId}`);
}

export async function payOrderAction(orderId: string, formData: FormData) {
  await mutateOrder(`/api/orders/${orderId}/actions/pay`, {
    version: Number(formData.get("version")),
    actor: formData.get("actor") ?? "frontend-operator",
    paymentReference: formData.get("paymentReference"),
    amount: Number(formData.get("amount")),
  });

  invalidate(orderId);
  redirect(`/orders/${orderId}`);
}

export async function shipOrderAction(orderId: string, formData: FormData) {
  await mutateOrder(`/api/orders/${orderId}/actions/ship`, {
    version: Number(formData.get("version")),
    actor: formData.get("actor") ?? "frontend-operator",
    trackingNumber: formData.get("trackingNumber"),
  });

  invalidate(orderId);
  redirect(`/orders/${orderId}`);
}

export async function completeOrderAction(orderId: string, formData: FormData) {
  await mutateOrder(`/api/orders/${orderId}/actions/complete`, {
    version: Number(formData.get("version")),
    actor: formData.get("actor") ?? "frontend-operator",
  });

  invalidate(orderId);
  redirect(`/orders/${orderId}`);
}

export async function cancelOrderAction(orderId: string, formData: FormData) {
  await mutateOrder(`/api/orders/${orderId}/actions/cancel`, {
    version: Number(formData.get("version")),
    actor: formData.get("actor") ?? "frontend-operator",
    reason: formData.get("reason"),
  });

  invalidate(orderId);
  redirect(`/orders/${orderId}`);
}

export async function refundOrderAction(orderId: string, formData: FormData) {
  await mutateOrder(`/api/orders/${orderId}/actions/refund`, {
    version: Number(formData.get("version")),
    actor: formData.get("actor") ?? "frontend-operator",
    amount: Number(formData.get("amount")),
    reason: formData.get("reason"),
  });

  invalidate(orderId);
  redirect(`/orders/${orderId}`);
}
