import type { ReactNode } from "react";
import Link from "next/link";
import { ArrowLeft, PackageCheck, Receipt, Truck } from "lucide-react";
import { OrderDetailActions } from "@/components/orders/order-detail-actions";
import { PageShell } from "@/components/orders/page-shell";
import { StatusBadge } from "@/components/orders/status-badge";
import { OrderTimeline } from "@/components/orders/order-timeline";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { getOrder } from "@/lib/api/client";

export const dynamic = "force-dynamic";

function statCard(title: string, value: string, hint: string, icon: ReactNode) {
  return (
    <Card>
      <CardHeader>
        <CardDescription>{title}</CardDescription>
        <CardTitle className="flex items-center justify-between text-2xl">
          <span>{value}</span>
          <span className="rounded-full bg-muted p-2 text-primary">{icon}</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-0 text-sm text-muted-foreground">{hint}</CardContent>
    </Card>
  );
}

export default async function OrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const order = await getOrder(id);

  return (
    <PageShell
      title={`${order.orderNumber} · ${order.customer.name}`}
      description="Lifecycle status, timeline, refunds, and lightweight editing are all centered here."
    >
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Link className="inline-flex items-center gap-2 hover:text-foreground" href="/orders">
          <ArrowLeft className="size-4" />
          Back to orders
        </Link>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        {statCard(
          "Current status",
          order.status,
          `Last updated ${formatDateTime(order.updatedAt)}`,
          <PackageCheck className="size-5" />,
        )}
        {statCard(
          "Order subtotal",
          formatCurrency(order.subtotal.amount, order.subtotal.currencyCode),
          `Refunded ${formatCurrency(order.refundedTotal.amount, order.refundedTotal.currencyCode)}`,
          <Receipt className="size-5" />,
        )}
        {statCard(
          "Shipment",
          order.trackingNumber ?? "Not assigned",
          order.shippedAt ? formatDateTime(order.shippedAt) : "Awaiting shipment",
          <Truck className="size-5" />,
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-3">
                Overview
                <StatusBadge status={order.status} />
              </CardTitle>
              <CardDescription>
                Customer, money, timestamps, and operator-facing edit actions.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <dl className="grid gap-4 md:grid-cols-2">
                <div>
                  <dt className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                    Customer
                  </dt>
                  <dd className="mt-1 text-sm font-medium">{order.customer.name}</dd>
                  <dd className="text-sm text-muted-foreground">
                    {order.customer.customerNumber} · {order.customer.email}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                    Paid total
                  </dt>
                  <dd className="mt-1 text-sm font-medium">
                    {formatCurrency(order.paidTotal.amount, order.paidTotal.currencyCode)}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                    Payment reference
                  </dt>
                  <dd className="mt-1 text-sm">{order.paymentReference ?? "Not captured"}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                    Created
                  </dt>
                  <dd className="mt-1 text-sm">{formatDateTime(order.createdAt)}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                    Notes
                  </dt>
                  <dd className="mt-1 text-sm">{order.notes ?? "No notes"}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                    Version
                  </dt>
                  <dd className="mt-1 text-sm font-mono">v{order.version}</dd>
                </div>
              </dl>
              <OrderDetailActions order={order} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Items</CardTitle>
              <CardDescription>Snapshot of the order lines currently attached to this order.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {order.items.map((item) => (
                <div
                  key={item.id}
                  className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 md:grid-cols-[1.4fr_120px_140px]"
                >
                  <div>
                    <div className="font-medium">{item.productName}</div>
                    <div className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                      {item.sku}
                    </div>
                  </div>
                  <div className="font-mono text-sm">Qty {item.quantity}</div>
                  <div className="text-right font-medium">
                    {formatCurrency(item.lineTotal.amount, item.lineTotal.currencyCode)}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <OrderTimeline events={order.events} />
          <Card>
            <CardHeader>
              <CardTitle>Refunds</CardTitle>
              <CardDescription>Refunds stay financial and audited instead of becoming lifecycle states.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {order.refunds.length === 0 ? (
                <p className="text-sm text-muted-foreground">No refunds recorded for this order.</p>
              ) : (
                order.refunds.map((refund) => (
                  <div key={refund.id} className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                    <div className="flex items-center justify-between gap-3">
                      <div className="font-medium">
                        {formatCurrency(refund.amount.amount, refund.amount.currencyCode)}
                      </div>
                      <div className="text-sm text-muted-foreground">
                        {formatDateTime(refund.createdAt)}
                      </div>
                    </div>
                    <p className="mt-2 text-sm text-foreground">{refund.reason}</p>
                    <p className="mt-1 text-xs uppercase tracking-[0.18em] text-muted-foreground">
                      {refund.actor}
                    </p>
                  </div>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </PageShell>
  );
}
