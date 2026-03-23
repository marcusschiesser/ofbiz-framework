export function formatCurrency(amount: number | string, currencyCode: string) {
  const numericAmount =
    typeof amount === "string" ? Number.parseFloat(amount) : amount;

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: currencyCode,
    minimumFractionDigits: 2,
  }).format(numericAmount);
}

export function formatDateTime(value?: string | null) {
  if (!value) return "Not set";

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
