import * as SecureStore from 'expo-secure-store';

const KEYCHAIN_SERVICE = 'com.atik.app.session';
const REFRESH_TOKEN_KEY = 'refresh-token';

export async function saveRefreshToken(refreshToken: string): Promise<void> {
  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken, {
    keychainService: KEYCHAIN_SERVICE,
  });
}

export async function readRefreshToken(): Promise<string | null> {
  return SecureStore.getItemAsync(REFRESH_TOKEN_KEY, {
    keychainService: KEYCHAIN_SERVICE,
  });
}

export async function clearRefreshToken(): Promise<void> {
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY, {
    keychainService: KEYCHAIN_SERVICE,
  });
}
