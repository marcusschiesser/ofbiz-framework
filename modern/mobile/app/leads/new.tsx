import { useState } from 'react';
import { router } from 'expo-router';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { createOpportunity } from '@/api/opportunities';
import { ErrorBanner } from '@/components/error-banner';

export default function NewLeadScreen() {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [title, setTitle] = useState('');
  const [dataSourceId, setDataSourceId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const invalid = !firstName.trim() || !lastName.trim() || !email.trim();

  async function onSave() {
    if (invalid || saving) {
      setError('First name, last name, and email are required.');
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const detail = await createOpportunity({
        firstName,
        lastName,
        email,
        companyName: companyName || undefined,
        title: title || undefined,
        dataSourceId: dataSourceId || undefined,
      });
      router.replace(`/leads/${detail.partyId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not create lead.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <ScrollView contentInsetAdjustmentBehavior="automatic" contentContainerStyle={{ padding: 16, gap: 12 }}>
      {error ? <ErrorBanner message={error} /> : null}
      <TextInput autoFocus value={firstName} onChangeText={setFirstName} placeholder="First name *" style={fieldStyle} />
      <TextInput value={lastName} onChangeText={setLastName} placeholder="Last name *" style={fieldStyle} />
      <TextInput value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" placeholder="Email *" style={fieldStyle} />
      <TextInput value={companyName} onChangeText={setCompanyName} placeholder="Company name" style={fieldStyle} />
      <TextInput value={title} onChangeText={setTitle} placeholder="Title" style={fieldStyle} />
      <TextInput value={dataSourceId} onChangeText={setDataSourceId} placeholder="Data source" style={fieldStyle} />
      <Pressable onPress={onSave} style={{ backgroundColor: '#2563eb', borderRadius: 12, padding: 14, opacity: saving ? 0.7 : 1 }}>
        <Text selectable style={{ color: 'white', textAlign: 'center', fontWeight: '700' }}>{saving ? 'Saving...' : 'Save Lead'}</Text>
      </Pressable>
    </ScrollView>
  );
}

const fieldStyle = { borderWidth: 1, borderColor: '#d1d5db', borderRadius: 12, borderCurve: 'continuous' as const, padding: 12 };
