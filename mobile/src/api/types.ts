export interface ApiEnvelope<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface ApiProblem {
  type?: string;
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
  message?: string;
}

export class ApiError extends Error {
  constructor(public readonly status: number, public readonly code: string, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export interface OtpChallenge { challengeId: string; expiresIn: number; retryAfterSeconds: number; }
export interface TokenResponse { accessToken: string; refreshToken: string; isNewUser: boolean; expiresIn: number; }
export interface IdentityUser {
  id: string; phone: string; email?: string; displayName?: string; avatarKey?: string;
  neighborhoodId?: string; status: string; trustScore: number; createdAt: string;
}
