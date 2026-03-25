import { AppShell } from "@/components/workflow/app-shell";
import { StatusBadge } from "@/components/workflow/status-badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  formatCurrency,
  formatStatus,
  getSalesOrder,
  toOfbizUrl,
} from "@/lib/leadflow";

type SalesOrderDetailPageProps = {
  params: Promise<{ orderId: string }>;
};

export default async function SalesOrderDetailPage({
  params,
}: SalesOrderDetailPageProps) {
  const { orderId } = await params;
  const order = await getSalesOrder(orderId);

  return (
    <AppShell
      eyebrow="Order"
      title={order.orderId}
      description="The order is now ready for review and downstream processing."
    >
      <section className="grid gap-6 xl:grid-cols-[340px_minmax(0,1fr)]">
        <Card className="surface-panel">
          <CardHeader>
            <CardTitle>Order header</CardTitle>
            <CardDescription>
              Commercial summary for the draft order.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="flex items-center justify-between gap-3">
              <span className="text-sm text-muted-foreground">Status</span>
              <StatusBadge statusId={order.statusId} />
            </div>
            <div className="surface-muted grid gap-3 p-4 text-sm">
              <div>
                <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                  Order category
                </p>
                <p className="mt-1 font-medium">
                  {formatStatus(order.orderTypeId)}
                </p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                  Customer ID
                </p>
                <p className="mt-1 font-medium">{order.partyId}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                  Grand total
                </p>
                <p className="mt-1 font-medium">
                  {formatCurrency(order.grandTotal)}
                </p>
              </div>
              {order.quoteId ? (
                <div>
                  <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    Origin quote
                  </p>
                  <p className="mt-1 font-medium">{order.quoteId}</p>
                </div>
              ) : null}
            </div>
            <a
              className="rounded-full border border-border/80 bg-background px-4 py-2 text-center text-sm font-medium hover:bg-accent"
              href={toOfbizUrl(
                `/ordermgr/control/findorders?orderId=${order.orderId}`,
              )}
              rel="noreferrer"
              target="_blank"
            >
              Open order record
            </a>
          </CardContent>
        </Card>

        <Card className="surface-panel">
          <CardHeader>
            <CardTitle>Order items</CardTitle>
            <CardDescription>
              Line items currently prepared for the order.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {order.items.map((item) => (
              <div
                className="surface-muted flex flex-col gap-3 p-4"
                key={item.seqId}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-1">
                    <p className="font-medium">
                      {item.description ?? item.productId ?? item.seqId}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      {item.productId ?? "Custom item"} · line {item.seqId}
                    </p>
                  </div>
                  <StatusBadge statusId={item.statusId} />
                </div>
                <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                  <span>Qty {item.quantity}</span>
                  <span>Unit {formatCurrency(item.unitPrice)}</span>
                  <span>
                    Extended {formatCurrency(item.quantity * item.unitPrice)}
                  </span>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </section>
    </AppShell>
  );
}
