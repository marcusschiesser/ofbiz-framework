import { useEffect } from 'react';
import { Link, Stack } from 'expo-router';
import { ActivityIndicator, FlatList, Pressable, Text, TextInput, View } from 'react-native';
import { ErrorBanner } from '@/components/error-banner';
import { StageBadge } from '@/components/stage-badge';
import { useOpportunities } from '@/hooks/use-opportunities';

export default function LeadListRoute() {
  const { filtered, search, isLoading, error, setSearch, load } = useOpportunities();

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <View style={{ flex: 1 }}>
      <Stack.Screen
        options={{
          headerRight: () => (
            <Link href="/leads/new" asChild>
              <Pressable accessibilityRole="button">
                <Text selectable style={{ fontWeight: '700', color: '#1B4DFF' }}>New Lead</Text>
              </Pressable>
            </Link>
          ),
        }}
      />
      <FlatList
        contentInsetAdjustmentBehavior="automatic"
        data={filtered}
        keyExtractor={(item) => item.partyId}
        refreshing={isLoading}
        onRefresh={load}
        ListHeaderComponent={
          <View style={{ padding: 16, gap: 12 }}>
            <TextInput
              value={search}
              onChangeText={setSearch}
              placeholder="Search name, company, or email"
              style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }}
            />
            {error ? <ErrorBanner message={error} /> : null}
            {isLoading ? <ActivityIndicator /> : null}
          </View>
        }
        renderItem={({ item }) => (
          <Link href={`/leads/${item.partyId}`} asChild>
            <Pressable style={{ paddingHorizontal: 16, paddingVertical: 12, gap: 6 }}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                <Text selectable style={{ fontSize: 16, fontWeight: '700' }}>{item.displayName}</Text>
                <StageBadge stage={item.stage} />
              </View>
              <Text selectable>{item.companyName ?? 'No company'}</Text>
              <Text selectable style={{ color: '#4B5563' }}>{item.email}</Text>
              <Text selectable style={{ color: '#1F2937', fontWeight: '500' }}>Next: {item.nextAction}</Text>
            </Pressable>
          </Link>
        )}
      />
    </View>
  );
}
