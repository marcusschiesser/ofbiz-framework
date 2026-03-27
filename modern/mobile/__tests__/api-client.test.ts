import { opportunityApi } from '@/api/client';

describe('opportunityApi', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('parses list response DTO', async () => {
    jest.spyOn(global, 'fetch' as never).mockResolvedValue({
      ok: true,
      json: async () => ({ opportunities: [{ partyId: 'P1', displayName: 'Ada', email: 'ada@example.com', companyName: 'Bergmann', stage: 'NEW', nextAction: 'Add request', hasRequest: false, createdAt: '2026-01-01T00:00:00Z' }] }),
    } as Response);

    const response = await opportunityApi.list();
    expect(response.opportunities[0].partyId).toBe('P1');
    expect(response.opportunities[0].stage).toBe('NEW');
  });

  it('maps API errors', async () => {
    jest.spyOn(global, 'fetch' as never).mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ status: 400, message: 'Validation failed' }),
    } as Response);

    await expect(opportunityApi.list()).rejects.toMatchObject({ status: 400, message: 'Validation failed' });
  });
});
