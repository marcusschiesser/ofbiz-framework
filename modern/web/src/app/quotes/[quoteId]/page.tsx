import Link from "next/link";
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
  getQuote,
  toOfbizUrl,
  totalFromItems,
} from "@/lib/leadflow";

type QuoteDetailPageProps = {
  params: Promise<{ quoteId: string }>;
};

export default async function QuoteDetailPage({
  params,
}: QuoteDetailPageProps) {
  const { quoteId } = await params;
  const quote = await getQuote(quoteId);
  const total = totalFromItems(quote.items);

  return (
    <AppShell
      eyebrow="Quote Review"
      title={quote.quoteName ?? quote.quoteId}
      description="Review the prepared pricing package and use the quote as the handoff point for downstream processing."
    >
      <section className="grid gap-6 xl:grid-cols-[340px_minmax(0,1fr)]">
        <Card className="surface-panel">
          <CardHeader>
            <CardTitle>Quote summary</CardTitle>
            <CardDescription>
              Pricing, status, and back-office follow-up links.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="flex items-center justify-between gap-3">
              <span className="text-sm text-muted-foreground">Status</span>
              <StatusBadge statusId={quote.statusId} />
            </div>
            <div className="surface-muted grid gap-3 p-4 text-sm">
              <div>
                <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                  Quote ID
                </p>
                <p className="mt-1 font-medium">{quote.quoteId}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                  Opportunity ID
                </p>
                <p className="mt-1 font-medium">{quote.partyId}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
                  Total value
                </p>
                <p className="mt-1 font-medium">{formatCurrency(total)}</p>
              </div>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link
                className="inline-flex rounded-full border border-border/80 bg-background px-4 py-2 text-sm font-medium hover:bg-accent"
                href={`/leads/${quote.partyId}#quote`}
              >
                Back to opportunity
              </Link>
              <a
                className="rounded-full border border-border/80 bg-background px-4 py-2 font-medium hover:bg-accent"
                href={toOfbizUrl(
                  `/ordermgr/control/findquotes?quoteId=${quote.quoteId}`,
                )}
                rel="noreferrer"
                target="_blank"
              >
                Open quote record
              </a>
            </div>
          </CardContent>
        </Card>

        <Card className="surface-panel">
          <CardHeader>
            <CardTitle>Quoted items</CardTitle>
            <CardDescription>
              Customer-facing pricing lines included in this quote.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {quote.items.map((item) => (
              <div
                className="surface-muted flex flex-col gap-3 p-4"
                key={item.seqId}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-1">
                    <p className="font-medium">
                      {item.comments ??
                        item.productId ??
                        `Quote item ${item.seqId}`}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      {item.productId ?? "Custom item"} · line {item.seqId}
                    </p>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    {item.quantity} × {formatCurrency(item.unitPrice)}
                  </p>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </section>
    </AppShell>
  );
}
