import { useCallback, useEffect, useState } from 'react';
import { Link, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { fetchOpportunity } from '@/api/opportunities';
import type { OpportunityDetail } from '@/api/types';
import { ErrorBanner } from '@/components/error-banner';

export default function LeadDetailScreen() {
  const { partyId } = useLocalSearchParams<{ partyId: string }>();
  const [detail, setDetail] = useState<OpportunityDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!partyId) return;
    try {
      const response = await fetchOpportunity(partyId);
      setDetail(response);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load lead details.');
    }
  }, [partyId]);

  useEffect(() => {
    void load();
  }, [load]);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  return (
    <ScrollView contentInsetAdjustmentBehavior="automatic" contentContainerStyle={{ padding: 16, gap: 12 }}>
      {error ? <ErrorBanner message={error} /> : null}
      {!detail ? <Text selectable>Loading lead details...</Text> : null}
      {detail ? (
        <>
          <Text selectable style={{ fontSize: 22, fontWeight: '700' }}>{detail.displayName}</Text>
          <Text selectable>{detail.companyName ?? 'No company'}</Text>
          <Text selectable>{detail.email}</Text>
          <Text selectable>Stage: {detail.stage}</Text>
          <Text selectable>Next Action: {detail.nextAction}</Text>

          <View style={{ borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, borderCurve: 'continuous', padding: 12, gap: 8 }}>
            <Text selectable style={{ fontWeight: '700' }}>Request Summary</Text>
            {detail.request ? (
              <>
                <Text selectable>Name: {detail.request.name}</Text>
                <Text selectable>Lines: {detail.request.lines.length}</Text>
                <Text selectable>{detail.request.isLocked ? 'Read only after quote creation.' : 'Editable'}</Text>
              </>
            ) : (
              <Text selectable>No request captured yet.</Text>
            )}
          </View>

          <Link href={`/leads/${partyId}/request`} asChild>
            <Pressable style={{ backgroundColor: '#2563eb', borderRadius: 12, padding: 14 }}>
              <Text selectable style={{ color: 'white', textAlign: 'center', fontWeight: '700' }}>
                {detail.request ? 'Review Request' : 'Add Request'}
              </Text>
            </Pressable>
          </Link>
        </>
      ) : null}
    </ScrollView>
  );
}
