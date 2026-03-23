/**
 * Generated from the backend OpenAPI spec.
 * Regenerate with `npm run generate:api` after the backend is running.
 */
export type OrderStatus =
  | "PENDING"
  | "PAID"
  | "SHIPPED"
  | "COMPLETED"
  | "CANCELLED";

export type OrderEventType =
  | "CREATED"
  | "UPDATED"
  | "PAID"
  | "SHIPPED"
  | "COMPLETED"
  | "CANCELLED"
  | "REFUNDED";

export interface MoneyDto {
  amount: number;
  currencyCode: string;
}

export interface CustomerDto {
  id: string;
  customerNumber: string;
  name: string;
  email: string;
}

export interface ProductDto {
  id: string;
  sku: string;
  name: string;
  unitPrice: MoneyDto;
  active: boolean;
}

export interface OrderItemDto {
  id: string;
  lineNumber: number;
  productId: string | null;
  sku: string;
  productName: string;
  quantity: number;
  unitPrice: MoneyDto;
  lineTotal: MoneyDto;
}

export interface OrderEventDto {
  id: string;
  eventType: OrderEventType;
  statusAfter: OrderStatus | null;
  actor: string;
  reason: string | null;
  payload: Record<string, unknown>;
  occurredAt: string;
}

export interface OrderRefundDto {
  id: string;
  amount: MoneyDto;
  actor: string;
  reason: string;
  createdAt: string;
}

export interface OrderSummaryDto {
  id: string;
  orderNumber: string;
  version: number;
  status: OrderStatus;
  customer: CustomerDto;
  subtotal: MoneyDto;
  paidTotal: MoneyDto;
  refundedTotal: MoneyDto;
  outstandingTotal: MoneyDto;
  createdAt: string;
  updatedAt: string;
}

export interface OrderDetailDto extends OrderSummaryDto {
  notes: string | null;
  trackingNumber: string | null;
  paymentReference: string | null;
  paidAt: string | null;
  shippedAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  canEdit: boolean;
  canCancel: boolean;
  canRefund: boolean;
  canShip: boolean;
  canComplete: boolean;
  items: OrderItemDto[];
  events: OrderEventDto[];
  refunds: OrderRefundDto[];
}

export interface OrderListResponse {
  data: OrderSummaryDto[];
}

export interface ReferenceDataResponse {
  customers: CustomerDto[];
  products: ProductDto[];
}
