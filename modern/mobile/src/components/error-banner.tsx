import { Text, View } from 'react-native';

export function ErrorBanner({ message }: { message: string }) {
  return (
    <View style={{ backgroundColor: '#FEECEC', borderRadius: 12, padding: 12 }}>
      <Text selectable style={{ color: '#9B1C1C', fontWeight: '600' }}>{message}</Text>
    </View>
  );
}
