import Link from "next/link";
import { Badge } from "@/components/ui/badge";

type AppShellProps = {
  eyebrow: string;
  title: string;
  description: string;
  children: React.ReactNode;
};

const navLinkClassName =
  "rounded-full border border-border/80 bg-background/80 px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-accent";

export function AppShell({
  eyebrow,
  title,
  description,
  children,
}: AppShellProps) {
  return (
    <div className="relative min-h-screen overflow-hidden">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-64 bg-[radial-gradient(circle_at_top,oklch(0.77_0.14_205_/_0.18),transparent_60%)]" />
      <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-8 px-5 py-6 sm:px-8 lg:px-10 lg:py-8">
        <header className="surface-panel relative overflow-hidden px-6 py-6 sm:px-8">
          <div className="absolute inset-y-0 right-0 w-72 bg-[radial-gradient(circle_at_right,oklch(0.89_0.11_56_/_0.32),transparent_65%)]" />
          <div className="relative flex flex-col gap-6">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-start">
              <div className="max-w-3xl space-y-4">
                <div className="flex flex-wrap items-center gap-3">
                  <Badge variant="secondary">Sales Workspace</Badge>
                  <Badge variant="outline">Quote Handoff</Badge>
                </div>
                <div className="space-y-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                    {eyebrow}
                  </p>
                  <h1 className="font-heading text-4xl leading-tight tracking-tight text-balance sm:text-5xl">
                    {title}
                  </h1>
                  <p className="max-w-2xl text-sm leading-7 text-muted-foreground sm:text-base">
                    {description}
                  </p>
                </div>
              </div>
              <div className="surface-muted flex min-w-72 flex-col gap-3 p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-muted-foreground">
                  Quick Access
                </p>
                <div className="flex flex-wrap gap-2">
                  <a
                    className={navLinkClassName}
                    href="https://localhost:8443/partymgr/"
                    rel="noreferrer"
                    target="_blank"
                  >
                    Contact Records
                  </a>
                  <a
                    className={navLinkClassName}
                    href="https://localhost:8443/ordermgr/control/findquotes"
                    rel="noreferrer"
                    target="_blank"
                  >
                    Quote Search
                  </a>
                </div>
              </div>
            </div>
            <nav className="flex flex-wrap gap-3">
              <Link className={navLinkClassName} href="/leads">
                Pipeline
              </Link>
              <a
                className={navLinkClassName}
                href="https://localhost:8443/partymgr/control/main"
                rel="noreferrer"
                target="_blank"
              >
                Contacts
              </a>
              <a
                className={navLinkClassName}
                href="https://localhost:8443/ordermgr/control/findquotes"
                rel="noreferrer"
                target="_blank"
              >
                Quotes
              </a>
            </nav>
          </div>
        </header>
        <main className="flex flex-1 flex-col gap-6">{children}</main>
      </div>
    </div>
  );
}
