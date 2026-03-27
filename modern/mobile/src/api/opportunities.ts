import { requestJson } from '@/api/client';
import type {
  OpportunityCreatePayload,
  OpportunityDetail,
  OpportunityListResponse,
  OpportunityRequestInput,
} from '@/api/types';

export function fetchOpportunities(): Promise<OpportunityListResponse> {
  return requestJson<OpportunityListResponse>('/api/opportunities');
}

export function createOpportunity(payload: OpportunityCreatePayload): Promise<OpportunityDetail> {
  return requestJson<OpportunityDetail>('/api/opportunities', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function fetchOpportunity(partyId: string): Promise<OpportunityDetail> {
  return requestJson<OpportunityDetail>(`/api/opportunities/${partyId}`);
}

export function saveOpportunityRequest(partyId: string, payload: OpportunityRequestInput): Promise<OpportunityDetail> {
  return requestJson<OpportunityDetail>(`/api/opportunities/${partyId}/request`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}
