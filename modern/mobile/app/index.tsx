import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useFocusEffect, useNavigation } from 'expo-router';
import { FlatList, Pressable, Text, TextInput, View } from 'react-native';
import { fetchOpportunities } from '@/api/opportunities';
import type { OpportunityListItem } from '@/api/types';
import { ErrorBanner } from '@/components/error-banner';
import { LeadRow } from '@/components/lead-row';

export default function LeadListScreen() {
  const navigation = useNavigation();
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [items, setItems] = useState<OpportunityListItem[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchOpportunities();
      setItems(response.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load leads.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    navigation.setOptions({
      headerRight: () => (
        <Link href="/leads/new" asChild>
          <Pressable>
            <Text selectable style={{ color: '#2563eb', fontWeight: '700' }}>New Lead</Text>
          </Pressable>
        </Link>
      ),
    });
  }, [navigation]);

  useEffect(() => {
    void load();
  }, [load]);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  const filtered = useMemo(() => {
    const value = query.trim().toLowerCase();
    if (!value) {
      return items;
    }

    return items.filter((item) =>
      [item.displayName, item.companyName ?? '', item.email].some((field) => field.toLowerCase().includes(value)),
    );
  }, [items, query]);

  return (
    <FlatList
      contentInsetAdjustmentBehavior="automatic"
      contentContainerStyle={{ padding: 16, gap: 12 }}
      data={filtered}
      keyExtractor={(item) => item.partyId}
      ListHeaderComponent={
        <View style={{ gap: 10 }}>
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder="Search by name, company, or email"
            style={{ borderWidth: 1, borderColor: '#d1d5db', borderRadius: 12, borderCurve: 'continuous', padding: 12 }}
          />
          {loading ? <Text selectable>Loading leads...</Text> : null}
          {error ? <ErrorBanner message={error} /> : null}
        </View>
      }
      renderItem={({ item }) => <LeadRow lead={item} />}
      ListEmptyComponent={!loading ? <Text selectable>No leads match your search.</Text> : null}
      ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
    />
  );
}
