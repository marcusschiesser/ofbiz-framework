import { Pressable, Text, TextInput, View } from 'react-native';

export type LineItemDraft = {
  description: string;
  quantity: string;
  unitPrice: string;
};

type Props = {
  index: number;
  value: LineItemDraft;
  onChange: (next: LineItemDraft) => void;
  onRemove: () => void;
  removable: boolean;
  disabled?: boolean;
};

export function LineItemEditor({ index, value, onChange, onRemove, removable, disabled }: Props) {
  return (
    <View style={{ gap: 8, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, borderCurve: 'continuous', padding: 12 }}>
      <Text selectable style={{ fontWeight: '600' }}>Line {index + 1}</Text>
      <TextInput
        value={value.description}
        editable={!disabled}
        onChangeText={(description) => onChange({ ...value, description })}
        placeholder="Description"
        style={{ borderWidth: 1, borderColor: '#d1d5db', borderRadius: 10, padding: 10 }}
      />
      <View style={{ flexDirection: 'row', gap: 10 }}>
        <TextInput
          value={value.quantity}
          editable={!disabled}
          keyboardType="numeric"
          onChangeText={(quantity) => onChange({ ...value, quantity })}
          placeholder="Qty"
          style={{ flex: 1, borderWidth: 1, borderColor: '#d1d5db', borderRadius: 10, padding: 10 }}
        />
        <TextInput
          value={value.unitPrice}
          editable={!disabled}
          keyboardType="decimal-pad"
          onChangeText={(unitPrice) => onChange({ ...value, unitPrice })}
          placeholder="Unit price"
          style={{ flex: 1, borderWidth: 1, borderColor: '#d1d5db', borderRadius: 10, padding: 10 }}
        />
      </View>
      {removable ? (
        <Pressable onPress={onRemove} disabled={disabled} style={{ paddingVertical: 6 }}>
          <Text selectable style={{ color: disabled ? '#9ca3af' : '#b91c1c', fontWeight: '600' }}>Remove line</Text>
        </Pressable>
      ) : null}
    </View>
  );
}
