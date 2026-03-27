import { useRef, useState } from 'react';
import { router } from 'expo-router';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { opportunityApi } from '@/api/client';
import { ErrorBanner } from '@/components/error-banner';

export default function NewLeadRoute() {
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', companyName: '', title: '', dataSourceId: '' });
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const lastNameRef = useRef<TextInput>(null);
  const emailRef = useRef<TextInput>(null);

  async function submit() {
    if (!form.firstName.trim() || !form.lastName.trim() || !form.email.trim()) {
      setError('First name, last name, and email are required.');
      return;
    }

    setIsSaving(true);
    setError(null);
    try {
      const created = await opportunityApi.create({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        companyName: form.companyName || undefined,
        title: form.title || undefined,
        dataSourceId: form.dataSourceId || undefined,
      });
      router.replace(`/leads/${created.partyId}`);
    } catch (err) {
      setError((err as { message?: string }).message ?? 'Failed to save lead');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <ScrollView contentInsetAdjustmentBehavior="automatic" contentContainerStyle={{ padding: 16, gap: 12 }}>
      {error ? <ErrorBanner message={error} /> : null}
      <TextInput
        autoFocus
        placeholder="First name *"
        value={form.firstName}
        onChangeText={(value) => setForm((prev) => ({ ...prev, firstName: value }))}
        returnKeyType="next"
        onSubmitEditing={() => lastNameRef.current?.focus()}
        style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }}
      />
      <TextInput
        ref={lastNameRef}
        placeholder="Last name *"
        value={form.lastName}
        onChangeText={(value) => setForm((prev) => ({ ...prev, lastName: value }))}
        returnKeyType="next"
        onSubmitEditing={() => emailRef.current?.focus()}
        style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }}
      />
      <TextInput
        ref={emailRef}
        placeholder="Email *"
        value={form.email}
        keyboardType="email-address"
        onChangeText={(value) => setForm((prev) => ({ ...prev, email: value }))}
        style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }}
      />
      <TextInput placeholder="Company name" value={form.companyName} onChangeText={(value) => setForm((prev) => ({ ...prev, companyName: value }))} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }} />
      <TextInput placeholder="Title" value={form.title} onChangeText={(value) => setForm((prev) => ({ ...prev, title: value }))} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }} />
      <TextInput placeholder="Data source" value={form.dataSourceId} onChangeText={(value) => setForm((prev) => ({ ...prev, dataSourceId: value }))} style={{ borderWidth: 1, borderColor: '#DFE3EA', borderRadius: 12, padding: 12 }} />

      <Pressable onPress={submit} disabled={isSaving} style={{ backgroundColor: '#1B4DFF', borderRadius: 12, padding: 14, alignItems: 'center' }}>
        <Text selectable style={{ color: 'white', fontWeight: '700' }}>{isSaving ? 'Saving…' : 'Save Lead'}</Text>
      </Pressable>
    </ScrollView>
  );
}
