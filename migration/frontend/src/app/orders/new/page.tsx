import { PageShell } from "@/components/orders/page-shell";
import { OrderCreateForm } from "@/components/orders/order-create-form";
import { getReferenceData } from "@/lib/api/client";

export const dynamic = "force-dynamic";

export default async function NewOrderPage() {
  const referenceData = await getReferenceData();

  return (
    <PageShell
      title="Create a new order"
      description="Create a pending order from the seeded customer and product catalog. Quantities above zero become order lines."
    >
      <OrderCreateForm referenceData={referenceData} />
    </PageShell>
  );
}
