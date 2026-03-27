export type OpportunityStage = 'NEW' | 'REQUEST_READY' | 'QUOTE_READY';

export type OpportunityListItem = {
  partyId: string;
  displayName: string;
  email: string;
  companyName: string | null;
  stage: OpportunityStage;
  nextAction: string;
  hasRequest: boolean;
  createdAt: string | null;
};

export type OpportunityListResponse = {
  items: OpportunityListItem[];
};

export type OpportunityRequestLine = {
  seqId: string;
  description: string;
  productId: string | null;
  quantity: number;
  unitPrice: number;
  story: string | null;
  statusId: string;
};

export type OpportunityRequestDetail = {
  requestId: string;
  name: string;
  description: string | null;
  story: string | null;
  lines: OpportunityRequestLine[];
  isLocked: boolean;
};

export type OpportunityDetail = {
  partyId: string;
  displayName: string;
  email: string;
  companyPartyId: string | null;
  companyName: string | null;
  stage: OpportunityStage;
  nextAction: string;
  request: OpportunityRequestDetail | null;
};

export type OpportunityCreatePayload = {
  firstName: string;
  lastName: string;
  email: string;
  companyName?: string;
  title?: string;
  dataSourceId?: string;
};

export type OpportunityRequestLineInput = {
  description: string;
  quantity: number;
  unitPrice: number;
  story?: string;
  productId?: string;
};

export type OpportunityRequestInput = {
  name: string;
  description?: string;
  story?: string;
  lines: OpportunityRequestLineInput[];
};

export type ApiError = {
  status: number;
  message: string;
};
