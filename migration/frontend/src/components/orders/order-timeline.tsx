import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { StatusBadge } from "@/components/orders/status-badge";
import { formatDateTime } from "@/lib/format";
import { type OrderEventDto } from "@/lib/api/generated";

export function OrderTimeline({ events }: { events: OrderEventDto[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Timeline</CardTitle>
        <CardDescription>
          Lifecycle events, edits, shipment updates, and refund history.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {events.map((event, index) => (
          <div key={event.id} className="space-y-4">
            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div className="space-y-1">
                <div className="flex items-center gap-3">
                  <span className="text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                    {event.eventType}
                  </span>
                  {event.statusAfter ? (
                    <StatusBadge status={event.statusAfter} />
                  ) : null}
                </div>
                <p className="text-sm text-foreground">{event.reason ?? "No reason recorded"}</p>
                <p className="text-xs text-muted-foreground">Actor: {event.actor}</p>
              </div>
              <div className="text-sm text-muted-foreground">
                {formatDateTime(event.occurredAt)}
              </div>
            </div>
            {Object.keys(event.payload).length > 0 ? (
              <pre className="overflow-x-auto rounded-2xl bg-slate-950 px-4 py-3 text-xs text-slate-100">
                {JSON.stringify(event.payload, null, 2)}
              </pre>
            ) : null}
            {index < events.length - 1 ? <Separator /> : null}
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
