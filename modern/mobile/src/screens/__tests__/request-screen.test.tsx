import { fireEvent, render, waitFor } from '@testing-library/react-native';
import RequestFormScreen from '../../../app/leads/[partyId]/request';

jest.mock('expo-router', () => ({
  useLocalSearchParams: () => ({ partyId: 'Lead100' }),
  router: { replace: jest.fn() },
}));

const mockFetchOpportunity = jest.fn();
const mockSaveOpportunityRequest = jest.fn();

jest.mock('@/api/opportunities', () => ({
  fetchOpportunity: (...args: unknown[]) => mockFetchOpportunity(...args),
  saveOpportunityRequest: (...args: unknown[]) => mockSaveOpportunityRequest(...args),
}));

describe('request form screen', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('requires at least one valid line item before save', async () => {
    mockFetchOpportunity.mockResolvedValue({
      partyId: 'Lead100',
      displayName: 'Lead 100',
      email: 'lead100@example.com',
      companyPartyId: null,
      companyName: null,
      stage: 'NEW',
      nextAction: 'Add request',
      request: null,
    });

    const screen = render(<RequestFormScreen />);

    await waitFor(() => expect(mockFetchOpportunity).toHaveBeenCalled());
    fireEvent.press(screen.getByText('Save Request'));

    expect(await screen.findByText('Request name is required.')).toBeTruthy();
    expect(mockSaveOpportunityRequest).not.toHaveBeenCalled();
  });

  it('shows locked state as read-only', async () => {
    mockFetchOpportunity.mockResolvedValue({
      partyId: 'Lead100',
      displayName: 'Lead 100',
      email: 'lead100@example.com',
      companyPartyId: null,
      companyName: null,
      stage: 'QUOTE_READY',
      nextAction: 'Handed off',
      request: {
        requestId: 'CR100',
        name: 'Locked',
        description: 'locked desc',
        story: 'story',
        isLocked: true,
        lines: [{ seqId: '0001', description: 'Item', productId: null, quantity: 1, unitPrice: 10, story: null, statusId: 'CRQ_ACCEPTED' }],
      },
    });

    const screen = render(<RequestFormScreen />);

    expect(await screen.findByText('This request is locked because a quote already exists.')).toBeTruthy();
    expect(screen.queryByText('Add line item')).toBeNull();
  });
});
