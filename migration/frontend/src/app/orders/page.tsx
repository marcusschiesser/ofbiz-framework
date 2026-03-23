import { OrderList } from "@/components/orders/order-list";
import { PageShell } from "@/components/orders/page-shell";
import { type OrderStatus } from "@/lib/api/generated";
import { listOrders } from "@/lib/api/client";

export const dynamic = "force-dynamic";

export default async function OrdersPage({
  searchParams,
}: {
  searchParams: Promise<{ query?: string; status?: OrderStatus }>;
}) {
  const params = await searchParams;
  const orders = await listOrders(params.query, params.status);

  return (
    <PageShell
      title="Track the order lifecycle"
      description="Monitor pending, paid, shipped, completed, and cancelled orders with a full event timeline and lightweight operator actions."
      actionHref="/orders/new"
      actionLabel="Create Order"
    >
      <OrderList orders={orders.data} query={params.query} status={params.status ?? ""} />
    </PageShell>
  );
}
