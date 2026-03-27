import { Text, View } from 'react-native';
import type { OpportunityStage } from '@/types/opportunity';

const STAGE_STYLES: Record<OpportunityStage, { label: string; backgroundColor: string }> = {
  NEW: { label: 'New', backgroundColor: '#E8F0FF' },
  REQUEST_READY: { label: 'Request Ready', backgroundColor: '#E8F8EF' },
  QUOTE_READY: { label: 'Quote Ready', backgroundColor: '#FFF3E0' },
};

export function StageBadge({ stage }: { stage: OpportunityStage }) {
  const style = STAGE_STYLES[stage];

  return (
    <View style={{ backgroundColor: style.backgroundColor, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 4 }}>
      <Text selectable style={{ fontSize: 12, fontWeight: '600' }}>{style.label}</Text>
    </View>
  );
}
