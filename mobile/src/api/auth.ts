import { post } from './client';
import { IdentityUser, OtpChallenge, TokenResponse } from './types';

export const requestOtp = (phone: string) => post<OtpChallenge>('/auth/otp/request', { phone });
export const verifyOtp = (phone: string, code: string, challengeId: string) => post<TokenResponse>('/auth/otp/verify', { phone, code, challengeId });
export const refreshSession = (refreshToken: string) => post<TokenResponse>('/auth/refresh', { refreshToken });
export const logoutSession = (refreshToken: string) => post<void>('/auth/logout', { refreshToken });
export const getMe = () => import('./client').then(({ get }) => get<IdentityUser>('/users/me'));
export const updateMe = (body: { displayName?: string; avatarKey?: string }) => import('./client').then(({ patch }) => patch<IdentityUser>('/users/me', body));
