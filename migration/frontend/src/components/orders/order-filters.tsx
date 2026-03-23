"use client";

import { Search, SlidersHorizontal, X } from "lucide-react";
import { type FormEvent, useState, useTransition } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { type OrderStatus } from "@/lib/api/generated";

const STATUSES: Array<{ value: OrderStatus; label: string }> = [
  { value: "PENDING", label: "Pending" },
  { value: "PAID", label: "Paid" },
  { value: "SHIPPED", label: "Shipped" },
  { value: "COMPLETED", label: "Completed" },
  { value: "CANCELLED", label: "Cancelled" },
];

export function OrderFilters({
  query,
  status,
}: {
  query?: string;
  status?: OrderStatus | "";
}) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();
  const [draftQuery, setDraftQuery] = useState(query ?? "");
  const [draftStatus, setDraftStatus] = useState<OrderStatus | "">(status ?? "");

  function navigate(nextQuery: string, nextStatus: OrderStatus | "") {
    const params = new URLSearchParams(searchParams.toString());
    if (nextQuery.trim()) {
      params.set("query", nextQuery.trim());
    } else {
      params.delete("query");
    }
    if (nextStatus) {
      params.set("status", nextStatus);
    } else {
      params.delete("status");
    }

    const suffix = params.toString() ? `?${params.toString()}` : "";
    startTransition(() => {
      router.push(`${pathname}${suffix}`);
    });
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    navigate(draftQuery, draftStatus);
  }

  function clearFilters() {
    setDraftQuery("");
    setDraftStatus("");
    startTransition(() => {
      router.push(pathname);
    });
  }

  return (
    <div className="space-y-4 rounded-2xl border border-border/70 bg-muted/20 p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="inline-flex items-center gap-2 text-sm font-medium text-foreground">
          <SlidersHorizontal className="size-4 text-primary" />
          Filters
        </div>
        {(query || status) ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={clearFilters}
          >
            <X className="size-4" />
            Clear Filters
          </Button>
        ) : null}
      </div>
      <form className="grid gap-4 lg:grid-cols-[1.4fr_240px_auto]" onSubmit={submit}>
        <div className="space-y-2">
          <Label htmlFor="orders-query">Search Orders</Label>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="orders-query"
              className="pl-10"
              name="query"
              placeholder="Order number, customer, or email"
              value={draftQuery}
              onChange={(event) => setDraftQuery(event.target.value)}
            />
          </div>
        </div>
        <div className="space-y-2">
          <Label htmlFor="orders-status">Status Filter</Label>
          <select
            id="orders-status"
            aria-label="Status filter"
            className="h-10 w-full rounded-xl border border-input bg-card px-3 py-2 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
            value={draftStatus}
            onChange={(event) => {
              const nextStatus = event.target.value as OrderStatus | "";
              setDraftStatus(nextStatus);
              navigate(draftQuery, nextStatus);
            }}
          >
            <option value="">All statuses</option>
            {STATUSES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-end">
          <Button type="submit" className="w-full lg:w-auto" disabled={isPending}>
            Apply Filters
          </Button>
        </div>
      </form>
    </div>
  );
}
