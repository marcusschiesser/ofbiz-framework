import type {
  ApiError,
  OpportunityCreateRequest,
  OpportunityDetail,
  OpportunityListResponse,
  OpportunityRequestInput,
} from '@/types/opportunity';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });

  if (!response.ok) {
    const payload = (await response.json()) as Partial<ApiError>;
    throw {
      status: payload.status ?? response.status,
      message: payload.message ?? 'Request failed',
    } as ApiError;
  }

  return (await response.json()) as T;
}

export const opportunityApi = {
  list: () => request<OpportunityListResponse>('/api/opportunities'),
  create: (input: OpportunityCreateRequest) =>
    request<OpportunityDetail>('/api/opportunities', {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  get: (partyId: string) => request<OpportunityDetail>(`/api/opportunities/${partyId}`),
  saveRequest: (partyId: string, input: OpportunityRequestInput) =>
    request<OpportunityDetail>(`/api/opportunities/${partyId}/request`, {
      method: 'PUT',
      body: JSON.stringify(input),
    }),
};
