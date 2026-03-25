"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { OpportunityBriefDetail, ProductOption } from "@/lib/leadflow";
import { ProductCombobox } from "@/components/workflow/product-combobox";

type LineDraft = {
  id: string;
  description: string;
  productId: string;
  quantity: string;
  unitPrice: string;
};

type DealBriefFormProps = {
  action: (formData: FormData) => void | Promise<void>;
  opportunityId: string;
  displayName: string;
  companyName: string | null;
  brief: OpportunityBriefDetail | null;
  products: ProductOption[];
};

function lineId(): string {
  return Math.random().toString(36).slice(2, 10);
}

function emptyLine(): LineDraft {
  return {
    id: lineId(),
    description: "",
    productId: "",
    quantity: "1",
    unitPrice: "0.00",
  };
}

function initialLines(brief: OpportunityBriefDetail | null): LineDraft[] {
  if (!brief || brief.lines.length === 0) {
    return [emptyLine()];
  }

  return brief.lines.map((line) => ({
    id: lineId(),
    description: line.description,
    productId: line.productId ?? "",
    quantity: line.quantity.toString(),
    unitPrice: line.unitPrice.toString(),
  }));
}

export function DealBriefForm({
  action,
  opportunityId,
  displayName,
  companyName,
  brief,
  products,
}: DealBriefFormProps) {
  const [lines, setLines] = useState<LineDraft[]>(() => initialLines(brief));

  const defaultTitle =
    brief?.title ?? `${companyName ?? displayName} pricing brief`;
  const defaultNotes =
    brief?.notes ??
    "Summarize the customer need, commercial context, and any delivery notes for pricing.";

  return (
    <form action={action}>
      <input name="partyId" type="hidden" value={opportunityId} />
      <FieldGroup>
        <Field>
          <FieldLabel htmlFor="title">Brief title</FieldLabel>
          <Input defaultValue={defaultTitle} id="title" name="title" required />
        </Field>
        <Field>
          <FieldLabel htmlFor="notes">Customer notes</FieldLabel>
          <Textarea
            defaultValue={defaultNotes}
            id="notes"
            name="notes"
            rows={5}
          />
          <FieldDescription>
            Capture the buying context, timing, and any pricing considerations.
          </FieldDescription>
        </Field>

        <div className="flex flex-col gap-4">
          {lines.map((line, index) => (
            <div
              className="surface-muted flex flex-col gap-4 p-4"
              key={line.id}
            >
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-medium">Line {index + 1}</p>
                {lines.length > 1 ? (
                  <Button
                    onClick={() => {
                      setLines((current) =>
                        current.filter(
                          (currentLine) => currentLine.id !== line.id,
                        ),
                      );
                    }}
                    type="button"
                    variant="outline"
                  >
                    Remove
                  </Button>
                ) : null}
              </div>
              <Field>
                <FieldLabel htmlFor={`lineDescription-${index}`}>
                  Item description
                </FieldLabel>
                <Input
                  defaultValue={line.description}
                  id={`lineDescription-${index}`}
                  name="lineDescription"
                  required
                />
              </Field>
              <div className="grid gap-4 md:grid-cols-3">
                <Field>
                  <FieldLabel htmlFor={`productId-${index}`}>
                    Product
                  </FieldLabel>
                  <ProductCombobox
                    defaultProductId={line.productId}
                    inputId={`productId-${index}`}
                    name="productId"
                    products={products}
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor={`quantity-${index}`}>
                    Quantity
                  </FieldLabel>
                  <Input
                    defaultValue={line.quantity}
                    id={`quantity-${index}`}
                    min="0.01"
                    name="quantity"
                    required
                    step="0.01"
                    type="number"
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor={`unitPrice-${index}`}>
                    Unit price
                  </FieldLabel>
                  <Input
                    defaultValue={line.unitPrice}
                    id={`unitPrice-${index}`}
                    min="0"
                    name="unitPrice"
                    required
                    step="0.01"
                    type="number"
                  />
                </Field>
              </div>
            </div>
          ))}
        </div>

        <div className="flex flex-wrap gap-3">
          <Button
            onClick={() => {
              setLines((current) => [...current, emptyLine()]);
            }}
            type="button"
            variant="outline"
          >
            Add line
          </Button>
          <Button type="submit">Save brief</Button>
        </div>
      </FieldGroup>
    </form>
  );
}
