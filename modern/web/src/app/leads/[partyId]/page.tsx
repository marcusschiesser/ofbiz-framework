import Link from "next/link";
import { createQuoteAction, saveBriefAction } from "@/app/actions";
import { AppShell } from "@/components/workflow/app-shell";
import { DealBriefForm } from "@/components/workflow/deal-brief-form";
import { OpportunityStageBadge } from "@/components/workflow/opportunity-stage-badge";
import { StatusBadge } from "@/components/workflow/status-badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { formatCurrency, getOpportunity, listProducts, toOfbizUrl } from "@/lib/leadflow";

type LeadDetailPageProps = {
  params: Promise<{ partyId: string }>;
};

export default async function LeadDetailPage({ params }: LeadDetailPageProps) {
  const { partyId } = await params;
  const [opportunity, products] = await Promise.all([getOpportunity(partyId), listProducts()]);
  const productsById = new Map(products.map((product) => [product.productId, product]));

  return (
    <AppShell
      eyebrow="Opportunity"
      title={opportunity.displayName}
      description="Keep the deal brief current, prepare pricing, and hand off once the quote is ready."
    >
      <section className="grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)]">
        <div className="flex flex-col gap-6" id="summary">
          <Card className="surface-panel">
            <CardHeader>
              <CardTitle>Opportunity summary</CardTitle>
              <CardDescription>Contact, account, stage, and current commercial value.</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm text-muted-foreground">Stage</span>
                <OpportunityStageBadge stage={opportunity.stage} />
              </div>
              <div className="surface-muted grid gap-3 p-4 text-sm">
                <div>
                  <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    Opportunity ID
                  </p>
                  <p className="mt-1 font-medium">{opportunity.partyId}</p>
                </div>
                <div>
                  <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    Account
                  </p>
                  <p className="mt-1 font-medium">
                    {opportunity.companyName ?? "No account assigned"}
                  </p>
                </div>
                <div>
                  <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    Contact
                  </p>
                  <p className="mt-1 font-medium">
                    {opportunity.email ?? "No email on file"}
                  </p>
                </div>
                <div>
                  <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    Current value
                  </p>
                  <p className="mt-1 font-medium">{formatCurrency(opportunity.currentValue)}</p>
                </div>
                <div>
                  <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                    Next action
                  </p>
                  <p className="mt-1 font-medium">{opportunity.nextAction}</p>
                </div>
              </div>
              <div className="flex flex-wrap gap-2 text-sm">
                <a
                  className="rounded-full border border-border/80 bg-background px-4 py-2 font-medium hover:bg-accent"
                  href={toOfbizUrl(opportunity.backOfficeLinks.contactRecordPath)}
                  rel="noreferrer"
                  target="_blank"
                >
                  Open contact record
                </a>
                {opportunity.backOfficeLinks.accountRecordPath ? (
                  <a
                    className="rounded-full border border-border/80 bg-background px-4 py-2 font-medium hover:bg-accent"
                    href={toOfbizUrl(opportunity.backOfficeLinks.accountRecordPath)}
                    rel="noreferrer"
                    target="_blank"
                  >
                    Open account record
                  </a>
                ) : null}
                {opportunity.backOfficeLinks.quoteRecordPath ? (
                  <a
                    className="rounded-full border border-border/80 bg-background px-4 py-2 font-medium hover:bg-accent"
                    href={toOfbizUrl(opportunity.backOfficeLinks.quoteRecordPath)}
                    rel="noreferrer"
                    target="_blank"
                  >
                    Open quote record
                  </a>
                ) : null}
              </div>
            </CardContent>
          </Card>

          <Card className="surface-panel" id="quote">
            <CardHeader>
              <CardTitle>Quote status</CardTitle>
              <CardDescription>The salesperson workflow ends once the quote is ready.</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-3">
              {opportunity.quote ? (
                <>
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm text-muted-foreground">Current quote</p>
                    <StatusBadge statusId={opportunity.quote.statusId} />
                  </div>
                  <div className="surface-muted grid gap-3 p-4 text-sm">
                    <div>
                      <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                        Quote ID
                      </p>
                      <p className="mt-1 font-medium">{opportunity.quote.quoteId}</p>
                    </div>
                    <div>
                      <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                        Total value
                      </p>
                      <p className="mt-1 font-medium">{formatCurrency(opportunity.quote.total)}</p>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <Link
                      className="inline-flex rounded-full bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90"
                      href={`/quotes/${opportunity.quote.quoteId}`}
                    >
                      Review quote
                    </Link>
                    {opportunity.backOfficeLinks.quoteRecordPath ? (
                      <a
                        className="rounded-full border border-border/80 bg-background px-4 py-2 text-sm font-medium hover:bg-accent"
                        href={toOfbizUrl(opportunity.backOfficeLinks.quoteRecordPath)}
                        rel="noreferrer"
                        target="_blank"
                      >
                        Open quote record
                      </a>
                    ) : null}
                  </div>
                </>
              ) : opportunity.brief ? (
                <>
                  <div className="surface-muted p-4 text-sm text-muted-foreground">
                    The brief is ready. Create a quote when pricing is complete.
                  </div>
                  <form action={createQuoteAction}>
                    <input name="partyId" type="hidden" value={opportunity.partyId} />
                    <Button type="submit">Create Quote</Button>
                  </form>
                </>
              ) : (
                <div className="surface-muted p-4 text-sm text-muted-foreground">
                  Save the deal brief first. Quote preparation opens once the customer need is captured.
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <Card className="surface-panel" id="brief">
          <CardHeader>
            <CardTitle>Deal brief</CardTitle>
            <CardDescription>
              Capture the customer need and item scope that pricing will work from.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            {opportunity.brief?.locked ? (
              <>
                <div className="surface-muted p-4 text-sm text-muted-foreground">
                  This brief is locked because pricing is already prepared for the opportunity.
                </div>
                <div className="surface-muted flex flex-col gap-4 p-4">
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                      Brief title
                    </p>
                    <p className="mt-1 font-medium">{opportunity.brief.title}</p>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                      Notes
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {opportunity.brief.notes ?? "No additional notes"}
                    </p>
                  </div>
                  <div className="flex flex-col gap-3">
                    {opportunity.brief.lines.map((line, index) => (
                      <div className="border border-border/70 bg-background/70 p-4" key={index}>
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="font-medium">{line.description}</p>
                            <p className="text-sm text-muted-foreground">
                              {line.productId
                                ? `${productsById.get(line.productId)?.displayName ?? line.productId} (${line.productId})`
                                : "Custom item"}
                            </p>
                          </div>
                          <p className="text-sm text-muted-foreground">
                            {line.quantity} × {formatCurrency(line.unitPrice)}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            ) : (
              <DealBriefForm
                action={saveBriefAction}
                brief={opportunity.brief}
                companyName={opportunity.companyName}
                displayName={opportunity.displayName}
                opportunityId={opportunity.partyId}
                products={products}
              />
            )}
          </CardContent>
        </Card>
      </section>
    </AppShell>
  );
}
