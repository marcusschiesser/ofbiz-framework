import { useEffect, useMemo, useState } from 'react';
import { router, useLocalSearchParams } from 'expo-router';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { opportunityApi } from '@/api/client';
import { ErrorBanner } from '@/components/error-banner';
import type { OpportunityRequestInput } from '@/types/opportunity';

type EditableLine = { description: string; quantity: string; unitPrice: string };

const BLANK_LINE: EditableLine = { description: '', quantity: '1', unitPrice: '0.00' };

export default function LeadRequestRoute() {
  const { partyId } = useLocalSearchParams<{ partyId: string }>();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [story, setStory] = useState('');
  const [lines, setLines] = useState<EditableLine[]>([{ ...BLANK_LINE }]);
  const [isLocked, setIsLocked] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const detail = await opportunityApi.get(partyId);
        if (detail.request) {
          setName(detail.request.name ?? '');
          setDescription(detail.request.description ?? '');
          setStory(detail.request.story ?? '');
          setIsLocked(detail.request.isLocked);
          setLines(
            detail.request.lines.length > 0
              ? detail.request.lines.map((line) => ({
                  description: line.description,
                  quantity: String(line.quantity),
                  unitPrice: String(line.unitPrice),
                }))
              : [{ ...BLANK_LINE }],
          );
        }
      } catch (err) {
        setError((err as { message?: string }).message ?? 'Failed to load request data');
      }
    }

    void load();
  }, [partyId]);

  const canSave = useMemo(() => !isLocked && !isSaving, [isLocked, isSaving]);

  function addLine() {
    setLines((current) => [...current, { ...BLANK_LINE }]);
  }

  function removeLine(index: number) {
    setLines((current) => (current.length === 1 ? current : current.filter((_, i) => i !== index)));
  }

  async function submit() {
    if (!name.trim()) {
      setError('Request name is required.');
      return;
    }

    if (lines.length < 1 || lines.some((line) => !line.description.trim())) {
      setError('At least one line with description is required.');
      return;
    }

    const payload: OpportunityRequestInput = {
      name,
      description: description || undefined,
      story: story || undefined,
      lines: lines.map((line) => ({
        description: line.description,
        quantity: line.quantity,
        unitPrice: line.unitPrice,
      })),
    };

    setIsSaving(true);
    setError(null);
    try {
      await opportunityApi.saveRequest(partyId, payload);
      router.replace(`/leads/${partyId}`);
    } catch (err) {
      setError((err as { message?: string }).message ?? 'Failed to save request');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <ScrollView contentInsetAdjustmentBehavior="automatic" contentContainerStyle={{ padding: 16, gap: 12 }}>
      {isLocked ? <ErrorBanner message="This request is locked because a quote already exists." /> : null}
      {error ? <ErrorBanner message={error} /> : null}

      <TextInput editable={!isLocked} autoFocus={!isLocked} placeholder="Request name *" value={name} onChangeText={setName} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }} />
      <TextInput editable={!isLocked} placeholder="Description" value={description} onChangeText={setDescription} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }} />
      <TextInput editable={!isLocked} placeholder="Story" value={story} onChangeText={setStory} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }} />

      <Text selectable style={{ fontWeight: '700' }}>Line Items</Text>
      {lines.map((line, index) => (
        <View key={`line-${index}`} style={{ borderWidth: 1, borderColor: '#E5E7EB', borderRadius: 12, padding: 12, gap: 8 }}>
          <TextInput editable={!isLocked} placeholder="Description *" value={line.description} onChangeText={(value) => setLines((prev) => prev.map((item, i) => (i === index ? { ...item, description: value } : item)))} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 10, padding: 10 }} />
          <TextInput editable={!isLocked} keyboardType="decimal-pad" placeholder="Quantity *" value={line.quantity} onChangeText={(value) => setLines((prev) => prev.map((item, i) => (i === index ? { ...item, quantity: value } : item)))} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 10, padding: 10 }} />
          <TextInput editable={!isLocked} keyboardType="decimal-pad" placeholder="Unit price *" value={line.unitPrice} onChangeText={(value) => setLines((prev) => prev.map((item, i) => (i === index ? { ...item, unitPrice: value } : item)))} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 10, padding: 10 }} />
          {!isLocked ? (
            <Pressable onPress={() => removeLine(index)} style={{ padding: 8, alignItems: 'flex-start' }}>
              <Text selectable style={{ color: '#B42318', fontWeight: '600' }}>Remove line</Text>
            </Pressable>
          ) : null}
        </View>
      ))}

      {!isLocked ? (
        <Pressable onPress={addLine} style={{ borderWidth: 1, borderColor: '#1B4DFF', borderRadius: 10, padding: 10, alignItems: 'center' }}>
          <Text selectable style={{ color: '#1B4DFF', fontWeight: '600' }}>Add line item</Text>
        </Pressable>
      ) : null}

      <Pressable onPress={submit} disabled={!canSave} style={{ backgroundColor: canSave ? '#1B4DFF' : '#9CA3AF', borderRadius: 12, padding: 14, alignItems: 'center' }}>
        <Text selectable style={{ color: 'white', fontWeight: '700' }}>{isSaving ? 'Saving…' : 'Save Request'}</Text>
      </Pressable>
    </ScrollView>
  );
}
