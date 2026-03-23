import Link from "next/link";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { OrderFilters } from "@/components/orders/order-filters";
import { StatusBadge } from "@/components/orders/status-badge";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { type OrderStatus, type OrderSummaryDto } from "@/lib/api/generated";

export function OrderList({
  orders,
  query,
  status,
}: {
  orders: OrderSummaryDto[];
  query?: string;
  status?: OrderStatus | "";
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Orders</CardTitle>
        <CardDescription>
          Search by order number, customer number, name, or email, then narrow the list with a status filter.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <OrderFilters
          key={`${query ?? ""}:${status ?? ""}`}
          query={query}
          status={status}
        />
        {orders.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border/80 bg-muted/15 p-8 text-sm text-muted-foreground">
            No orders matched the current search criteria.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full border-separate border-spacing-y-3">
              <thead>
                <tr className="text-left text-xs uppercase tracking-[0.2em] text-muted-foreground">
                  <th className="px-4">Order</th>
                  <th className="px-4">Customer</th>
                  <th className="px-4">Status</th>
                  <th className="px-4">Subtotal</th>
                  <th className="px-4">Refunded</th>
                  <th className="px-4">Updated</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} className="rounded-2xl bg-muted/30 shadow-sm">
                    <td className="rounded-l-2xl px-4 py-4 align-top">
                      <Link
                        className="font-semibold text-primary hover:text-primary/80"
                        href={`/orders/${order.id}`}
                      >
                        {order.orderNumber}
                      </Link>
                      <div className="mt-1 text-xs text-muted-foreground">
                        v{order.version}
                      </div>
                    </td>
                    <td className="px-4 py-4 align-top">
                      <div className="font-medium">{order.customer.name}</div>
                      <div className="text-xs text-muted-foreground">
                        {order.customer.customerNumber}
                      </div>
                    </td>
                    <td className="px-4 py-4 align-top">
                      <StatusBadge status={order.status} />
                    </td>
                    <td className="px-4 py-4 align-top font-medium">
                      {formatCurrency(
                        order.subtotal.amount,
                        order.subtotal.currencyCode,
                      )}
                    </td>
                    <td className="px-4 py-4 align-top">
                      {formatCurrency(
                        order.refundedTotal.amount,
                        order.refundedTotal.currencyCode,
                      )}
                    </td>
                    <td className="rounded-r-2xl px-4 py-4 align-top text-sm text-muted-foreground">
                      {formatDateTime(order.updatedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
