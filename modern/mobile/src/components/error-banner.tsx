import { Text, View } from 'react-native';

type Props = { message: string };

export function ErrorBanner({ message }: Props) {
  return (
    <View
      style={{
        borderRadius: 12,
        borderCurve: 'continuous',
        padding: 12,
        backgroundColor: '#fee2e2',
      }}
    >
      <Text selectable style={{ color: '#991b1b', fontWeight: '600' }}>
        {message}
      </Text>
    </View>
  );
}
