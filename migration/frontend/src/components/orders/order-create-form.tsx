import { createOrderAction } from "@/app/orders/actions";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { formatCurrency } from "@/lib/format";
import { type ReferenceDataResponse } from "@/lib/api/generated";

export function OrderCreateForm({
  referenceData,
}: {
  referenceData: ReferenceDataResponse;
}) {
  return (
    <form action={createOrderAction} className="grid gap-6 lg:grid-cols-[1.1fr_1.3fr]">
      <Card>
        <CardHeader>
          <CardTitle>Order Header</CardTitle>
          <CardDescription>
            Choose the customer and add internal notes for the order.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          <div className="space-y-2">
            <Label htmlFor="customerId">Customer</Label>
            <select
              className="flex h-10 w-full rounded-xl border border-input bg-card px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              id="customerId"
              name="customerId"
              defaultValue={referenceData.customers[0]?.id}
            >
              {referenceData.customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.customerNumber} · {customer.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="notes">Notes</Label>
            <Textarea
              id="notes"
              name="notes"
              placeholder="Optional internal note for the lifecycle view"
            />
          </div>
          <input type="hidden" name="actor" value="frontend-operator" />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Order Lines</CardTitle>
          <CardDescription>
            Enter quantities for any products you want to include.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3">
            {referenceData.products.map((product) => (
              <div
                key={product.id}
                className="grid items-center gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 md:grid-cols-[1.4fr_0.8fr_120px]"
              >
                <div>
                  <div className="font-medium">{product.name}</div>
                  <div className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                    {product.sku}
                  </div>
                </div>
                <div className="font-mono text-sm">
                  {formatCurrency(
                    product.unitPrice.amount,
                    product.unitPrice.currencyCode,
                  )}
                </div>
                <Input
                  min={0}
                  step={1}
                  type="number"
                  name={`qty-${product.id}`}
                  defaultValue={0}
                />
              </div>
            ))}
          </div>
          <div className="flex justify-end">
            <Button type="submit" size="lg">
              Create Order
            </Button>
          </div>
        </CardContent>
      </Card>
    </form>
  );
}
