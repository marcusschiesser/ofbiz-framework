import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';
import type { OpportunityListItem } from '@/api/types';

const stageColors: Record<OpportunityListItem['stage'], string> = {
  NEW: '#dbeafe',
  REQUEST_READY: '#dcfce7',
  QUOTE_READY: '#ede9fe',
};

export function LeadRow({ lead }: { lead: OpportunityListItem }) {
  return (
    <Link href={`/leads/${lead.partyId}`} asChild>
      <Pressable
        style={{
          borderWidth: 1,
          borderColor: '#e5e7eb',
          borderRadius: 16,
          borderCurve: 'continuous',
          padding: 14,
          gap: 8,
          backgroundColor: '#ffffff',
        }}
      >
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
          <Text selectable style={{ fontWeight: '700', fontSize: 16, flexShrink: 1 }}>
            {lead.displayName}
          </Text>
          <View style={{ backgroundColor: stageColors[lead.stage], borderRadius: 999, paddingHorizontal: 10, paddingVertical: 4 }}>
            <Text selectable style={{ fontWeight: '600', fontSize: 12 }}>
              {lead.stage.replace('_', ' ')}
            </Text>
          </View>
        </View>
        <Text selectable style={{ color: '#4b5563' }}>{lead.companyName ?? 'No company'}</Text>
        <Text selectable style={{ color: '#1d4ed8' }}>{lead.email}</Text>
        <Text selectable style={{ color: '#374151' }}>Next: {lead.nextAction}</Text>
      </Pressable>
    </Link>
  );
}
