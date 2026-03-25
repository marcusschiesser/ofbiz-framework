import { Badge } from "@/components/ui/badge";
import { formatStatus } from "@/lib/leadflow";

type StatusBadgeProps = {
  statusId: string;
};

function variantForStatus(statusId: string) {
  if (statusId.startsWith("ORDER") || statusId.startsWith("ITEM")) {
    return "default" as const;
  }

  if (statusId.startsWith("QUO")) {
    return "secondary" as const;
  }

  return "outline" as const;
}

export function StatusBadge({ statusId }: StatusBadgeProps) {
  return (
    <Badge variant={variantForStatus(statusId)}>{formatStatus(statusId)}</Badge>
  );
}
