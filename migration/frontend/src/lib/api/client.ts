import "server-only";

import {
  type OrderDetailDto,
  type OrderListResponse,
  type ReferenceDataResponse,
} from "@/lib/api/generated";

const API_BASE_URL =
  process.env.BACKEND_URL?.replace(/\/$/, "") ?? "http://localhost:8080";

type NextCacheConfig = {
  next?: {
    revalidate?: number | false;
    tags?: string[];
  };
};

async function apiFetch<T>(
  path: string,
  init?: RequestInit & NextCacheConfig,
): Promise<T> {
  const method = init?.method?.toUpperCase() ?? "GET";
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    cache: init?.cache ?? (method === "GET" ? "force-cache" : "no-store"),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`API ${response.status}: ${text}`);
  }

  return (await response.json()) as T;
}

export async function listOrders(query?: string, status?: string) {
  const params = new URLSearchParams();
  if (query) params.set("query", query);
  if (status) params.set("status", status);
  const suffix = params.toString() ? `?${params.toString()}` : "";

  return apiFetch<OrderListResponse>(`/api/orders${suffix}`, {
    next: { tags: ["orders"] },
  });
}

export async function getOrder(orderId: string) {
  return apiFetch<OrderDetailDto>(`/api/orders/${orderId}`, {
    next: { tags: ["orders", `order:${orderId}`] },
  });
}

export async function getReferenceData() {
  return apiFetch<ReferenceDataResponse>("/api/reference-data", {
    next: { tags: ["reference-data"] },
  });
}

export async function mutateOrder<T>(path: string, body: T) {
  return apiFetch<OrderDetailDto>(path, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function patchOrder<T>(orderId: string, body: T) {
  return apiFetch<OrderDetailDto>(`/api/orders/${orderId}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}
