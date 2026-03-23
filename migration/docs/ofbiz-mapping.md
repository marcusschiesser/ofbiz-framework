# OFBiz Mapping Notes

This application deliberately does not mirror the full OFBiz order model. It extracts only the lifecycle view needed for operators.

## Source Inspiration in OFBiz

The design is based on the intent of:

- `OrderHeader`
- `OrderStatus`
- order payment history
- shipment tracking history
- OFBiz seed and demo orders

## Mapping

| OFBiz concept | New application concept |
| --- | --- |
| `OrderHeader.orderId` | `orders.id` plus human-readable `order_number` |
| `OrderHeader.statusId` | simplified `OrderStatus` enum |
| `ORDER_CREATED` | `PENDING` |
| payment received / settled | `PAID` |
| shipment sent / shipped | `SHIPPED` |
| `ORDER_COMPLETED` | `COMPLETED` |
| `ORDER_CANCELLED` | `CANCELLED` |
| `OrderStatus` rows | `order_events` timeline and audit trail |
| OFBiz order adjustments / refunds | `order_refunds` plus `REFUNDED` events |

## Intentional Simplifications

- The wider OFBiz workflow engine is not ported.
- Accounting, invoicing, tax, and fulfillment orchestration are out of scope.
- Refunds do not change lifecycle state.
- Versioned optimistic locking replaces broader OFBiz service orchestration for concurrent edits.
- The UI is centered around the operator lifecycle view rather than generic order maintenance.

## Tables

- `customers`
- `products`
- `orders`
- `order_items`
- `order_events`
- `order_refunds`

## Lifecycle Rules

- update: only `PENDING` or `PAID`
- cancel: only `PENDING` or `PAID`
- ship: only `PAID`
- complete: only `SHIPPED`
- refund: only if `paid_total > refunded_total`

