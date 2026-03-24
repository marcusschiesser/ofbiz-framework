import { Badge } from "@/components/ui/badge";
import { OpportunityStage, formatOpportunityStage } from "@/lib/leadflow";

type OpportunityStageBadgeProps = {
  stage: OpportunityStage;
};

function variantForStage(stage: OpportunityStage) {
  switch (stage) {
    case "QUOTE_READY":
      return "secondary" as const;
    case "BRIEF_READY":
      return "default" as const;
    default:
      return "outline" as const;
  }
}

export function OpportunityStageBadge({ stage }: OpportunityStageBadgeProps) {
  return <Badge variant={variantForStage(stage)}>{formatOpportunityStage(stage)}</Badge>;
}
