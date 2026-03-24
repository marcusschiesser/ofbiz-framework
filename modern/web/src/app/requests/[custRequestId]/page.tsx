import { redirect } from "next/navigation";
import { getRequest } from "@/lib/leadflow";

type RequestDetailPageProps = {
  params: Promise<{ custRequestId: string }>;
};

export default async function RequestDetailPage({
  params,
}: RequestDetailPageProps) {
  const { custRequestId } = await params;
  const request = await getRequest(custRequestId);
  redirect(`/leads/${request.leadPartyId}#brief`);
}
