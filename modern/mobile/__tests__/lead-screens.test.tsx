import React from 'react';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import LeadListRoute from '../app/index';
import NewLeadRoute from '../app/leads/new';
import LeadRequestRoute from '../app/leads/[partyId]/request';
import { opportunityApi } from '@/api/client';

jest.mock('expo-router', () => ({
  Link: ({ children }: { children: React.ReactNode }) => children,
  Stack: { Screen: () => null },
  router: { replace: jest.fn() },
  useLocalSearchParams: () => ({ partyId: 'P100' }),
}));

jest.mock('@/api/client', () => ({
  opportunityApi: {
    list: jest.fn(),
    create: jest.fn(),
    get: jest.fn(),
    saveRequest: jest.fn(),
  },
}));

describe('screens', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('renders startup lead list', async () => {
    (opportunityApi.list as jest.Mock).mockResolvedValue({
      opportunities: [{ partyId: 'P1', displayName: 'A Lead', email: 'a@x.com', companyName: 'Bergmann', stage: 'NEW', nextAction: 'Add request', hasRequest: false, createdAt: '2026-01-01T00:00:00Z' }],
    });

    const screen = render(<LeadListRoute />);
    await waitFor(() => expect(screen.getByText('A Lead')).toBeTruthy());
  });

  it('validates new lead form required fields', async () => {
    const screen = render(<NewLeadRoute />);
    fireEvent.press(screen.getByText('Save Lead'));
    await waitFor(() => expect(screen.getByText('First name, last name, and email are required.')).toBeTruthy());
  });

  it('supports successful lead creation', async () => {
    (opportunityApi.create as jest.Mock).mockResolvedValue({ partyId: 'P99' });
    const screen = render(<NewLeadRoute />);

    fireEvent.changeText(screen.getByPlaceholderText('First name *'), 'Ada');
    fireEvent.changeText(screen.getByPlaceholderText('Last name *'), 'Lovelace');
    fireEvent.changeText(screen.getByPlaceholderText('Email *'), 'ada@example.com');
    fireEvent.press(screen.getByText('Save Lead'));

    await waitFor(() => expect(opportunityApi.create).toHaveBeenCalled());
  });

  it('validates request form requires at least one line', async () => {
    (opportunityApi.get as jest.Mock).mockResolvedValue({ request: null });
    const screen = render(<LeadRequestRoute />);

    fireEvent.changeText(screen.getByPlaceholderText('Request name *'), 'Need quote');
    fireEvent.changeText(screen.getByPlaceholderText('Description *'), '');
    fireEvent.press(screen.getByText('Save Request'));

    await waitFor(() => expect(screen.getByText('At least one line with description is required.')).toBeTruthy());
  });

  it('shows read-only behavior for locked requests', async () => {
    (opportunityApi.get as jest.Mock).mockResolvedValue({
      request: { name: 'Locked request', description: '', story: '', isLocked: true, lines: [{ description: 'Widget', quantity: '1', unitPrice: '10.00' }] },
    });

    const screen = render(<LeadRequestRoute />);
    await waitFor(() => expect(screen.getByText('This request is locked because a quote already exists.')).toBeTruthy());
    expect(screen.queryByText('Add line item')).toBeNull();
  });
});
