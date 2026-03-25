import Link from "next/link";
import { createLeadAction } from "@/app/actions";
import { AppShell } from "@/components/workflow/app-shell";
import { OpportunityStageBadge } from "@/components/workflow/opportunity-stage-badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  OpportunityStage,
  formatCurrency,
  listOpportunities,
} from "@/lib/leadflow";

const stageOrder: OpportunityStage[] = ["NEW", "BRIEF_READY", "QUOTE_READY"];
const stageCopy: Record<OpportunityStage, string> = {
  NEW: "New",
  BRIEF_READY: "Brief Ready",
  QUOTE_READY: "Quote Ready",
};

export default async function LeadsPage() {
  const opportunities = await listOpportunities();
  const totals = {
    NEW: opportunities.filter((opportunity) => opportunity.stage === "NEW")
      .length,
    BRIEF_READY: opportunities.filter(
      (opportunity) => opportunity.stage === "BRIEF_READY",
    ).length,
    QUOTE_READY: opportunities.filter(
      (opportunity) => opportunity.stage === "QUOTE_READY",
    ).length,
  };

  return (
    <AppShell
      eyebrow="Pipeline"
      title="Sales Pipeline"
      description="Create opportunities, capture the customer brief, and hand off once pricing is ready."
    >
      <section className="grid gap-6 xl:grid-cols-[360px_minmax(0,1fr)]">
        <div className="flex flex-col gap-6">
          <Card className="surface-panel">
            <CardHeader>
              <CardTitle>Add opportunity</CardTitle>
              <CardDescription>
                Start a new deal with the contact and account details the team
                needs to begin discovery.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form action={createLeadAction}>
                <FieldGroup>
                  <Field>
                    <FieldLabel htmlFor="firstName">First name</FieldLabel>
                    <Input id="firstName" name="firstName" required />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="lastName">Last name</FieldLabel>
                    <Input id="lastName" name="lastName" required />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="email">Email</FieldLabel>
                    <Input
                      id="email"
                      name="email"
                      required
                      type="email"
                      placeholder="buyer@example.com"
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="companyName">Company</FieldLabel>
                    <Input
                      id="companyName"
                      name="companyName"
                      placeholder="Northwind Devices"
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="title">Title</FieldLabel>
                    <Input
                      id="title"
                      name="title"
                      placeholder="Operations Director"
                    />
                    <FieldDescription>
                      Optional job title for the primary contact.
                    </FieldDescription>
                  </Field>
                  <Button type="submit">Save Lead</Button>
                </FieldGroup>
              </form>
            </CardContent>
          </Card>

          <Card className="surface-panel" size="sm">
            <CardHeader>
              <CardTitle>Snapshot</CardTitle>
              <CardDescription>Pipeline health by sales stage.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-3">
              {stageOrder.map((stage) => (
                <div
                  className="surface-muted flex items-center justify-between gap-3 p-4"
                  key={stage}
                >
                  <div>
                    <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                      {stageCopy[stage]}
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {stage === "NEW"
                        ? "Waiting for discovery notes"
                        : stage === "BRIEF_READY"
                          ? "Ready for pricing"
                          : "Ready for handoff"}
                    </p>
                  </div>
                  <p className="font-heading text-3xl">{totals[stage]}</p>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>

        <div className="flex flex-col gap-6">
          {stageOrder.map((stage) => {
            const stageOpportunities = opportunities.filter(
              (opportunity) => opportunity.stage === stage,
            );

            return (
              <Card className="surface-panel" key={stage}>
                <CardHeader>
                  <CardTitle>{stageCopy[stage]}</CardTitle>
                  <CardDescription>
                    {stage === "NEW"
                      ? "Fresh opportunities that still need a deal brief."
                      : stage === "BRIEF_READY"
                        ? "Qualified opportunities that are ready for pricing."
                        : "Quoted opportunities that are ready for handoff."}
                  </CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-3">
                  {stageOpportunities.length === 0 ? (
                    <div className="surface-muted p-5 text-sm text-muted-foreground">
                      No opportunities in this stage.
                    </div>
                  ) : (
                    stageOpportunities.map((opportunity) => (
                      <Link
                        className="surface-muted flex flex-col gap-4 p-4 transition-transform hover:-translate-y-0.5"
                        href={`/leads/${opportunity.partyId}`}
                        key={opportunity.partyId}
                      >
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div className="space-y-1">
                            <p className="font-heading text-xl">
                              {opportunity.displayName}
                            </p>
                            <p className="text-sm text-muted-foreground">
                              {opportunity.companyName ?? "No account assigned"}
                            </p>
                          </div>
                          <OpportunityStageBadge stage={opportunity.stage} />
                        </div>
                        <div className="grid gap-3 text-sm text-muted-foreground md:grid-cols-3">
                          <span>{opportunity.email ?? "No email on file"}</span>
                          <span>
                            {formatCurrency(opportunity.currentValue)}
                          </span>
                          <span>{opportunity.nextAction}</span>
                        </div>
                        <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                          Opportunity {opportunity.partyId}
                        </p>
                      </Link>
                    ))
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>
    </AppShell>
  );
}
