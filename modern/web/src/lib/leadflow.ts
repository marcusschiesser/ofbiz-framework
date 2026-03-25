export type OpportunityStage = "NEW" | "BRIEF_READY" | "QUOTE_READY";

export type OpportunitySummary = {
  partyId: string;
  displayName: string;
  companyName: string | null;
  email: string | null;
  stage: OpportunityStage;
  currentValue: number;
  nextAction: string;
  latestQuoteId: string | null;
};

export type OpportunityCreateInput = {
  firstName: string;
  lastName: string;
  email: string;
  companyName?: string;
  title?: string;
};

export type OpportunityLineInput = {
  description: string;
  productId?: string;
  quantity: number;
  unitPrice: number;
};

export type ProductOption = {
  productId: string;
  displayName: string;
  productName: string | null;
  internalName: string | null;
};

export type OpportunityBriefInput = {
  title: string;
  notes?: string;
  lines: OpportunityLineInput[];
};

export type OpportunityLineDetail = {
  description: string;
  productId: string | null;
  quantity: number;
  unitPrice: number;
};

export type OpportunityBriefDetail = {
  title: string;
  notes: string | null;
  lines: OpportunityLineDetail[];
  locked: boolean;
};

export type OpportunityQuoteItemSummary = {
  seqId: string;
  description: string;
  productId: string | null;
  quantity: number;
  unitPrice: number;
};

export type OpportunityQuoteSummary = {
  quoteId: string;
  statusId: string;
  total: number;
  items: OpportunityQuoteItemSummary[];
};

export type OpportunityBackOfficeLinks = {
  contactRecordPath: string;
  accountRecordPath: string | null;
  quoteRecordPath: string | null;
};

export type OpportunityDetail = {
  partyId: string;
  displayName: string;
  companyPartyId: string | null;
  companyName: string | null;
  email: string | null;
  stage: OpportunityStage;
  currentValue: number;
  nextAction: string;
  latestQuoteId: string | null;
  brief: OpportunityBriefDetail | null;
  quote: OpportunityQuoteSummary | null;
  backOfficeLinks: OpportunityBackOfficeLinks;
};

export type RequestItemDetail = {
  seqId: string;
  description: string;
  productId: string | null;
  quantity: number;
  unitPrice: number;
  statusId: string;
};

export type RequestDetail = {
  custRequestId: string;
  name: string;
  description: string | null;
  statusId: string;
  leadPartyId: string;
  items: RequestItemDetail[];
  quoteIds: string[];
};

export type QuoteItemDetail = {
  seqId: string;
  productId: string | null;
  quantity: number;
  unitPrice: number;
  sourceRequestItemSeqId: string | null;
  comments: string | null;
};

export type QuoteDetail = {
  quoteId: string;
  quoteTypeId: string;
  statusId: string;
  partyId: string;
  quoteName: string | null;
  productStoreId: string | null;
  salesChannelEnumId: string | null;
  items: QuoteItemDetail[];
  orderIds: string[];
};

export type SalesOrderItemDetail = {
  seqId: string;
  productId: string | null;
  description: string | null;
  quantity: number;
  unitPrice: number;
  statusId: string;
};

export type SalesOrderDetail = {
  orderId: string;
  statusId: string;
  orderTypeId: string;
  partyId: string;
  quoteId: string | null;
  productStoreId: string | null;
  webSiteId: string | null;
  grandTotal: number;
  items: SalesOrderItemDetail[];
};

const apiBaseUrl =
  process.env.LEADFLOW_API_BASE_URL ?? "http://localhost:8081/api";
const ofbizBaseUrl = process.env.OFBIZ_BASE_URL ?? "https://localhost:8443";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  const hasBody = init?.body !== undefined;

  if (hasBody && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers,
    cache: "no-store",
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message || `Unable to complete the request (${response.status}).`,
    );
  }

  return (await response.json()) as T;
}

export function toOfbizUrl(path: string): string {
  return new URL(path, ofbizBaseUrl).toString();
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatStatus(statusId: string): string {
  return statusId
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

export function formatOpportunityStage(stage: OpportunityStage): string {
  return stage
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

export function totalFromItems(
  items: Array<{ quantity: number; unitPrice: number }>,
): number {
  return items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);
}

export async function listOpportunities(): Promise<OpportunitySummary[]> {
  return request<OpportunitySummary[]>("/opportunities");
}

export async function listProducts(query?: string): Promise<ProductOption[]> {
  const search = query ? `?query=${encodeURIComponent(query)}` : "";
  return request<ProductOption[]>(`/products${search}`);
}

export async function getOpportunity(
  partyId: string,
): Promise<OpportunityDetail> {
  return request<OpportunityDetail>(`/opportunities/${partyId}`);
}

export async function createOpportunity(
  input: OpportunityCreateInput,
): Promise<OpportunityDetail> {
  return request<OpportunityDetail>("/opportunities", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function saveOpportunityBrief(
  partyId: string,
  input: OpportunityBriefInput,
): Promise<OpportunityDetail> {
  return request<OpportunityDetail>(`/opportunities/${partyId}/brief`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export async function createOpportunityQuote(
  partyId: string,
): Promise<OpportunityDetail> {
  return request<OpportunityDetail>(`/opportunities/${partyId}/quote`, {
    method: "POST",
  });
}

export async function getRequest(
  custRequestId: string,
): Promise<RequestDetail> {
  return request<RequestDetail>(`/requests/${custRequestId}`);
}

export async function getQuote(quoteId: string): Promise<QuoteDetail> {
  return request<QuoteDetail>(`/quotes/${quoteId}`);
}

export async function getSalesOrder(
  orderId: string,
): Promise<SalesOrderDetail> {
  return request<SalesOrderDetail>(`/sales-orders/${orderId}`);
}
