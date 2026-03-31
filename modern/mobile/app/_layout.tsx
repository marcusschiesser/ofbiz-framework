import { Stack } from 'expo-router';

export default function RootLayout() {
  return (
    <Stack>
      <Stack.Screen name="index" options={{ title: 'Leads' }} />
      <Stack.Screen name="leads/new" options={{ title: 'New Lead' }} />
      <Stack.Screen name="leads/[partyId]" options={{ title: 'Lead Detail' }} />
      <Stack.Screen name="leads/[partyId]/request" options={{ title: 'Customer Request' }} />
    </Stack>
  );
}
