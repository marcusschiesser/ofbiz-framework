import { ApiClientError } from '@/api/client';
import { fetchOpportunities } from '@/api/opportunities';

describe('opportunities api', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  it('parses list response', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      json: async () => ({
        items: [
          {
            partyId: 'Demo',
            displayName: 'Demo Lead',
            email: 'demo@example.com',
            companyName: 'Bergmann',
            stage: 'NEW',
            nextAction: 'Add request',
            hasRequest: false,
            createdAt: '2026-03-27T00:00:00Z',
          },
        ],
      }),
    });

    const response = await fetchOpportunities();

    expect(response.items).toHaveLength(1);
    expect(response.items[0].partyId).toBe('Demo');
  });

  it('maps api errors', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ status: 400, message: 'Validation failed' }),
    });

    await expect(fetchOpportunities()).rejects.toMatchObject(new ApiClientError(400, 'Validation failed'));
  });
});
