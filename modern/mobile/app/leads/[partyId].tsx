import { useEffect, useState } from 'react';
import { Link, Stack, useLocalSearchParams } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { opportunityApi } from '@/api/client';
import { ErrorBanner } from '@/components/error-banner';
import { StageBadge } from '@/components/stage-badge';
import type { OpportunityDetail } from '@/types/opportunity';

export default function LeadDetailRoute() {
  const { partyId } = useLocalSearchParams<{ partyId: string }>();
  const [detail, setDetail] = useState<OpportunityDetail | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      setIsLoading(true);
      setError(null);
      try {
        setDetail(await opportunityApi.get(partyId));
      } catch (err) {
        setError((err as { message?: string }).message ?? 'Failed to load lead');
      } finally {
        setIsLoading(false);
      }
    }

    void load();
  }, [partyId]);

  return (
    <ScrollView contentInsetAdjustmentBehavior="automatic" contentContainerStyle={{ padding: 16, gap: 12 }}>
      <Stack.Screen options={{ title: detail?.displayName ?? 'Lead Detail' }} />
      {isLoading ? <ActivityIndicator /> : null}
      {error ? <ErrorBanner message={error} /> : null}
      {detail ? (
        <>
          <Text selectable style={{ fontSize: 20, fontWeight: '700' }}>{detail.displayName}</Text>
          <Text selectable>{detail.companyName ?? 'No company'}</Text>
          <Text selectable>{detail.email}</Text>
          <StageBadge stage={detail.stage} />
          <Text selectable>Next action: {detail.nextAction}</Text>

          <View style={{ backgroundColor: '#F8FAFC', borderRadius: 12, padding: 12, gap: 8 }}>
            <Text selectable style={{ fontWeight: '700' }}>Request Summary</Text>
            <Text selectable>{detail.request ? detail.request.name : 'No request captured yet.'}</Text>
            <Text selectable>{detail.request ? `${detail.request.lines.length} line item(s)` : 'Add a request to continue.'}</Text>
            <Link href={`/leads/${partyId}/request`} asChild>
              <Pressable style={{ backgroundColor: '#1B4DFF', borderRadius: 10, padding: 10, alignItems: 'center' }}>
                <Text selectable style={{ color: 'white', fontWeight: '700' }}>{detail.request ? 'Update Request' : 'Add Request'}</Text>
              </Pressable>
            </Link>
          </View>
        </>
      ) : null}
    </ScrollView>
  );
}
