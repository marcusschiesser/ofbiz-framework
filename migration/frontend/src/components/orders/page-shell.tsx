import type { ReactNode } from "react";
import Link from "next/link";
import { ArrowRight, Package2 } from "lucide-react";
import { Button } from "@/components/ui/button";

export function PageShell({
  title,
  description,
  children,
  actionHref,
  actionLabel,
}: {
  title: string;
  description: string;
  children: ReactNode;
  actionHref?: string;
  actionLabel?: string;
}) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col gap-8 px-6 py-8 lg:px-10">
      <header className="grid gap-6 rounded-[2rem] border border-border/80 bg-card/95 px-6 py-8 shadow-[0_24px_80px_rgba(15,23,42,0.08)] lg:grid-cols-[1.6fr_auto] lg:items-end">
        <div className="space-y-4">
          <div className="inline-flex items-center gap-3 rounded-full border border-border/80 bg-background/80 px-4 py-2 text-xs font-semibold uppercase tracking-[0.26em] text-muted-foreground">
            <Package2 className="size-4 text-primary" />
            Order Lifecycle View
          </div>
          <div className="space-y-2">
            <h1 className="text-3xl font-semibold tracking-tight text-foreground lg:text-4xl">
              {title}
            </h1>
            <p className="max-w-3xl text-sm leading-7 text-muted-foreground lg:text-base">
              {description}
            </p>
          </div>
        </div>
        {actionHref && actionLabel ? (
          <div className="flex justify-start lg:justify-end">
            <Button asChild size="lg">
              <Link href={actionHref}>
                {actionLabel}
                <ArrowRight data-icon="inline-end" className="size-4" />
              </Link>
            </Button>
          </div>
        ) : null}
      </header>
      {children}
    </div>
  );
}
