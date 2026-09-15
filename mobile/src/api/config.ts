/** Gateway URL; the mobile app must not connect directly to a microservice. */
// Android emulator: 10.0.2.2. For a physical phone, replace the host with the
// development machine LAN IP or use `adb reverse tcp:8080 tcp:8080`.
// Set EXPO_PUBLIC_API_URL for a physical device (for example,
// http://192.168.1.20:8080/api/v1). Emulator and adb-reverse keep the default.
export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'http://10.0.2.2:8080/api/v1').replace(/\/$/, '');

export const REQUEST_TIMEOUT_MS = 15000;
