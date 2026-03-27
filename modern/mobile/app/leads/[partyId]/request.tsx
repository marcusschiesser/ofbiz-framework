import { useEffect, useMemo, useState } from 'react';
import { router, useLocalSearchParams } from 'expo-router';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { fetchOpportunity, saveOpportunityRequest } from '@/api/opportunities';
import { ErrorBanner } from '@/components/error-banner';
import { LineItemEditor, type LineItemDraft } from '@/components/line-item-editor';

export default function RequestFormScreen() {
  const { partyId } = useLocalSearchParams<{ partyId: string }>();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [story, setStory] = useState('');
  const [lines, setLines] = useState<LineItemDraft[]>([{ description: '', quantity: '', unitPrice: '' }]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [locked, setLocked] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      if (!partyId) {
        return;
      }
      setLoading(true);
      try {
        const detail = await fetchOpportunity(partyId);
        if (detail.request) {
          setName(detail.request.name);
          setDescription(detail.request.description ?? '');
          setStory(detail.request.story ?? '');
          setLines(
            detail.request.lines.length > 0
              ? detail.request.lines.map((line) => ({ description: line.description, quantity: String(line.quantity), unitPrice: String(line.unitPrice) }))
              : [{ description: '', quantity: '', unitPrice: '' }],
          );
          setLocked(detail.request.isLocked);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unable to load request.');
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, [partyId]);

  const validationError = useMemo(() => {
    if (!name.trim()) return 'Request name is required.';
    if (lines.length < 1) return 'At least one line item is required.';
    for (const line of lines) {
      if (!line.description.trim()) return 'Each line needs a description.';
      if (Number(line.quantity) <= 0 || Number.isNaN(Number(line.quantity))) return 'Each line needs a positive quantity.';
      if (Number(line.unitPrice) < 0 || Number.isNaN(Number(line.unitPrice))) return 'Each line needs a valid unit price.';
    }
    return null;
  }, [lines, name]);

  async function onSave() {
    if (!partyId || locked || saving) return;
    if (validationError) {
      setError(validationError);
      return;
    }

    setSaving(true);
    setError(null);

    try {
      await saveOpportunityRequest(partyId, {
        name,
        description,
        story,
        lines: lines.map((line) => ({ description: line.description, quantity: Number(line.quantity), unitPrice: Number(line.unitPrice) })),
      });
      router.replace(`/leads/${partyId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to save request.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <ScrollView contentInsetAdjustmentBehavior="automatic" contentContainerStyle={{ padding: 16, gap: 12 }}>
      {loading ? <Text selectable>Loading request...</Text> : null}
      {locked ? <ErrorBanner message="This request is locked because a quote already exists." /> : null}
      {error ? <ErrorBanner message={error} /> : null}

      <TextInput autoFocus={!name} editable={!locked} value={name} onChangeText={setName} placeholder="Request name *" style={fieldStyle} />
      <TextInput editable={!locked} value={description} onChangeText={setDescription} placeholder="Description" style={fieldStyle} multiline />
      <TextInput editable={!locked} value={story} onChangeText={setStory} placeholder="Story" style={fieldStyle} multiline />

      <View style={{ gap: 8 }}>
        {lines.map((line, index) => (
          <LineItemEditor
            key={`line-${index}`}
            index={index}
            value={line}
            disabled={locked}
            onChange={(next) => setLines((prev) => prev.map((item, i) => (i === index ? next : item)))}
            onRemove={() => setLines((prev) => prev.filter((_, i) => i !== index))}
            removable={lines.length > 1}
          />
        ))}
      </View>

      {!locked ? (
        <Pressable
          onPress={() => setLines((prev) => [...prev, { description: '', quantity: '', unitPrice: '' }])}
          style={{ borderWidth: 1, borderColor: '#2563eb', borderRadius: 12, borderCurve: 'continuous', padding: 12 }}
        >
          <Text selectable style={{ color: '#2563eb', textAlign: 'center', fontWeight: '700' }}>Add line item</Text>
        </Pressable>
      ) : null}

      <Pressable
        onPress={onSave}
        disabled={locked}
        style={{ backgroundColor: locked ? '#9ca3af' : '#2563eb', borderRadius: 12, padding: 14 }}
      >
        <Text selectable style={{ color: 'white', textAlign: 'center', fontWeight: '700' }}>{saving ? 'Saving...' : 'Save Request'}</Text>
      </Pressable>
    </ScrollView>
  );
}

const fieldStyle = { borderWidth: 1, borderColor: '#d1d5db', borderRadius: 12, borderCurve: 'continuous' as const, padding: 12 };
