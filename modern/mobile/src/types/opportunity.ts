export type OpportunityStage = 'NEW' | 'REQUEST_READY' | 'QUOTE_READY';

export type OpportunityListItem = {
  partyId: string;
  displayName: string;
  email: string;
  companyName?: string | null;
  stage: OpportunityStage;
  nextAction: string;
  hasRequest: boolean;
  createdAt: string;
};

export type OpportunityListResponse = {
  opportunities: OpportunityListItem[];
};

export type OpportunityRequestLine = {
  seqId?: string;
  description: string;
  productId?: string | null;
  quantity: string;
  unitPrice: string;
  story?: string | null;
  statusId?: string;
};

export type OpportunityRequest = {
  requestId: string;
  name: string;
  description?: string | null;
  story?: string | null;
  lines: OpportunityRequestLine[];
  isLocked: boolean;
};

export type OpportunityDetail = {
  partyId: string;
  displayName: string;
  email: string;
  companyPartyId?: string | null;
  companyName?: string | null;
  stage: OpportunityStage;
  nextAction: string;
  request?: OpportunityRequest | null;
};

export type OpportunityCreateRequest = {
  firstName: string;
  lastName: string;
  email: string;
  companyName?: string;
  title?: string;
  dataSourceId?: string;
};

export type OpportunityRequestInput = {
  name: string;
  description?: string;
  story?: string;
  lines: Array<{
    description: string;
    productId?: string;
    quantity: string;
    unitPrice: string;
    story?: string;
  }>;
};

export type ApiError = {
  status: number;
  message: string;
};
