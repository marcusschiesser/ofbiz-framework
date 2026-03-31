import { useCallback, useMemo, useState } from 'react';
import { opportunityApi } from '@/api/client';
import type { OpportunityListItem } from '@/types/opportunity';

export function useOpportunities() {
  const [items, setItems] = useState<OpportunityListItem[]>([]);
  const [search, setSearch] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await opportunityApi.list();
      setItems(response.opportunities);
    } catch (err) {
      setError((err as { message?: string }).message ?? 'Failed to load opportunities');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const filtered = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    if (!normalized) {
      return items;
    }

    return items.filter((item) => {
      return [item.displayName, item.companyName ?? '', item.email]
        .join(' ')
        .toLowerCase()
        .includes(normalized);
    });
  }, [items, search]);

  return {
    items,
    filtered,
    search,
    isLoading,
    error,
    setSearch,
    load,
  };
}
