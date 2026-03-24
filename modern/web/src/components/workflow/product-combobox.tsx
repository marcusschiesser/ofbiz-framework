"use client";

import { useId, useState } from "react";
import { Input } from "@/components/ui/input";
import { ProductOption } from "@/lib/leadflow";

type ProductComboboxProps = {
  inputId: string;
  name: string;
  defaultProductId?: string;
  products: ProductOption[];
};

function optionLabel(product: ProductOption): string {
  return `${product.displayName} (${product.productId})`;
}

function findSelectedProduct(products: ProductOption[], value: string): ProductOption | null {
  const normalized = value.trim().toLowerCase();

  if (normalized.length === 0) {
    return null;
  }

  return (
    products.find((product) => product.productId.toLowerCase() === normalized) ??
    products.find((product) => optionLabel(product).toLowerCase() === normalized) ??
    null
  );
}

export function ProductCombobox({
  inputId,
  name,
  defaultProductId,
  products,
}: ProductComboboxProps) {
  const listId = useId();
  const defaultProduct = products.find((product) => product.productId === defaultProductId) ?? null;
  const [selectionText, setSelectionText] = useState(
    defaultProduct ? optionLabel(defaultProduct) : defaultProductId ?? "",
  );
  const [selectedProductId, setSelectedProductId] = useState(defaultProduct?.productId ?? defaultProductId ?? "");

  const selectedProduct = products.find((product) => product.productId === selectedProductId) ?? null;
  const hasPendingText = selectionText.trim().length > 0 && selectedProduct == null;

  return (
    <div className="flex flex-col gap-2">
      <Input
        autoComplete="off"
        id={inputId}
        list={listId}
        onBlur={(event) => {
          const matchedProduct = findSelectedProduct(products, event.target.value);
          setSelectedProductId(matchedProduct?.productId ?? "");
          setSelectionText(matchedProduct ? optionLabel(matchedProduct) : event.target.value);
        }}
        onChange={(event) => {
          const nextValue = event.target.value;
          const matchedProduct = findSelectedProduct(products, nextValue);
          setSelectionText(nextValue);
          setSelectedProductId(matchedProduct?.productId ?? "");
        }}
        placeholder="Search by name or product code"
        value={selectionText}
      />
      <input name={name} type="hidden" value={selectedProductId} />
      <datalist id={listId}>
        {products.map((product) => (
          <option key={product.productId} value={optionLabel(product)}>
            {product.productId}
          </option>
        ))}
      </datalist>
      <p className="text-xs text-muted-foreground">
        {selectedProduct
          ? `Selected: ${selectedProduct.displayName} (${selectedProduct.productId})`
          : hasPendingText
            ? "No catalog match selected yet. Leave it blank to save the line as a custom item."
            : "Search by product name or product code. Leave it blank for a custom item."}
      </p>
    </div>
  );
}
