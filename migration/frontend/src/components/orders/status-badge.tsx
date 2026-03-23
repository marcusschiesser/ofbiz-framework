import { Badge } from "@/components/ui/badge";
import { type OrderStatus } from "@/lib/api/generated";

const variantByStatus: Record<
  OrderStatus,
  "neutral" | "success" | "warning" | "destructive" | "outline"
> = {
  PENDING: "warning",
  PAID: "neutral",
  SHIPPED: "outline",
  COMPLETED: "success",
  CANCELLED: "destructive",
};

export function StatusBadge({ status }: { status: OrderStatus }) {
  return <Badge variant={variantByStatus[status]}>{status}</Badge>;
}
