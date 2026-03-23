"use client";

import { useState } from "react";
import {
  cancelOrderAction,
  completeOrderAction,
  payOrderAction,
  refundOrderAction,
  shipOrderAction,
  updateOrderAction,
} from "@/app/orders/actions";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { type OrderDetailDto } from "@/lib/api/generated";

export function OrderDetailActions({ order }: { order: OrderDetailDto }) {
  const [openDialog, setOpenDialog] = useState<string | null>(null);

  return (
    <div className="grid gap-4">
      <div className="flex flex-wrap gap-3">
        {order.status === "PENDING" ? (
          <Dialog open={openDialog === "pay"} onOpenChange={(open) => setOpenDialog(open ? "pay" : null)}>
            <DialogTrigger asChild>
              <Button>Mark Paid</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Capture payment</DialogTitle>
                <DialogDescription>Move the order from pending to paid.</DialogDescription>
              </DialogHeader>
              <form action={payOrderAction.bind(null, order.id)} className="space-y-4">
                <input type="hidden" name="version" value={order.version} />
                <input type="hidden" name="actor" value="frontend-operator" />
                <div className="space-y-2">
                  <Label htmlFor="paymentReference">Payment reference</Label>
                  <Input id="paymentReference" name="paymentReference" placeholder="PAY-2026-001" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="amount">Amount</Label>
                  <Input
                    id="amount"
                    name="amount"
                    type="number"
                    min="0"
                    step="0.01"
                    defaultValue={order.subtotal.amount}
                  />
                </div>
                <Button type="submit">Confirm Payment</Button>
              </form>
            </DialogContent>
          </Dialog>
        ) : null}

        {order.status === "PAID" ? (
          <Dialog open={openDialog === "ship"} onOpenChange={(open) => setOpenDialog(open ? "ship" : null)}>
            <DialogTrigger asChild>
              <Button variant="secondary">Ship Order</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Ship order</DialogTitle>
                <DialogDescription>Add a tracking number and move the order to shipped.</DialogDescription>
              </DialogHeader>
              <form action={shipOrderAction.bind(null, order.id)} className="space-y-4">
                <input type="hidden" name="version" value={order.version} />
                <input type="hidden" name="actor" value="frontend-operator" />
                <div className="space-y-2">
                  <Label htmlFor="trackingNumber">Tracking number</Label>
                  <Input id="trackingNumber" name="trackingNumber" placeholder="TRACK-2026-1001" />
                </div>
                <Button type="submit">Confirm Shipment</Button>
              </form>
            </DialogContent>
          </Dialog>
        ) : null}

        {order.status === "SHIPPED" ? (
          <form action={completeOrderAction.bind(null, order.id)}>
            <input type="hidden" name="version" value={order.version} />
            <input type="hidden" name="actor" value="frontend-operator" />
            <Button variant="secondary" type="submit">
              Complete Order
            </Button>
          </form>
        ) : null}

        {order.canCancel ? (
          <Dialog open={openDialog === "cancel"} onOpenChange={(open) => setOpenDialog(open ? "cancel" : null)}>
            <DialogTrigger asChild>
              <Button variant="destructive">Cancel</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Cancel order</DialogTitle>
                <DialogDescription>Cancellation is only allowed before shipment.</DialogDescription>
              </DialogHeader>
              <form action={cancelOrderAction.bind(null, order.id)} className="space-y-4">
                <input type="hidden" name="version" value={order.version} />
                <input type="hidden" name="actor" value="frontend-operator" />
                <div className="space-y-2">
                  <Label htmlFor="cancelReason">Reason</Label>
                  <Textarea id="cancelReason" name="reason" placeholder="Customer changed mind" />
                </div>
                <Button type="submit" variant="destructive">
                  Confirm Cancellation
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        ) : null}

        {order.canRefund ? (
          <Dialog open={openDialog === "refund"} onOpenChange={(open) => setOpenDialog(open ? "refund" : null)}>
            <DialogTrigger asChild>
              <Button variant="outline">Refund</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Refund order</DialogTitle>
                <DialogDescription>Refunds are tracked as financial events without changing lifecycle status.</DialogDescription>
              </DialogHeader>
              <form action={refundOrderAction.bind(null, order.id)} className="space-y-4">
                <input type="hidden" name="version" value={order.version} />
                <input type="hidden" name="actor" value="frontend-operator" />
                <div className="space-y-2">
                  <Label htmlFor="refundAmount">Amount</Label>
                  <Input id="refundAmount" name="amount" type="number" min="0.01" step="0.01" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="refundReason">Reason</Label>
                  <Textarea id="refundReason" name="reason" placeholder="Customer return or adjustment reason" />
                </div>
                <Button type="submit">Submit Refund</Button>
              </form>
            </DialogContent>
          </Dialog>
        ) : null}
      </div>

      {order.canEdit ? (
        <form action={updateOrderAction.bind(null, order.id)} className="space-y-4 rounded-2xl border border-border/70 bg-muted/20 p-5">
          <input type="hidden" name="version" value={order.version} />
          <input type="hidden" name="actor" value="frontend-operator" />
          <div className="space-y-2">
            <Label htmlFor="notes">Notes</Label>
            <Textarea id="notes" name="notes" defaultValue={order.notes ?? ""} />
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            {order.items.map((item) => (
              <div key={item.id} className="rounded-xl border border-border/70 bg-card p-4">
                <div className="font-medium">{item.productName}</div>
                <div className="mb-3 text-xs uppercase tracking-[0.18em] text-muted-foreground">
                  {item.sku}
                </div>
                <input type="hidden" name={`productId-${item.productId}`} value={item.productId ?? ""} />
                <Label htmlFor={`qty-${item.productId}`}>Quantity</Label>
                <Input
                  id={`qty-${item.productId}`}
                  name={`qty-${item.productId}`}
                  type="number"
                  min={1}
                  step={1}
                  defaultValue={item.quantity}
                />
              </div>
            ))}
          </div>
          <Button type="submit" variant="outline">
            Save Updates
          </Button>
        </form>
      ) : null}
    </div>
  );
}
