import { get, post } from './client';

export type ExchangeRequestStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'EXPIRED';

export type HandoverStatus =
  | 'PENDING_CODE'
  | 'CONFIRMED'
  | 'EXPIRED'
  | 'NO_SHOW'
  | 'PENDING_REVIEW';

export interface CreateExchangeRequestInput {
  requesterId: string;
  ownerId: string;
  message: string;
}

export interface ConfirmHandoverInput {
  providerId: string;
  code: string;
}

export interface ListingRequestDto {
  id: string;
  listingId: string;
  requesterId: string;
  ownerId: string;
  message: string | null;
  status: ExchangeRequestStatus;
  createdAt: string;
}

export interface HandoverDto {
  id: string;
  requestId: string;
  listingId: string;
  providerId: string;
  receiverId: string;
  /** Only returned when the authenticated flow requests the code for the receiver. */
  confirmationCode: string | null;
  status: HandoverStatus;
  failedAttempts: number;
  expiresAt: string;
  confirmedAt: string | null;
}

const exchangePath = '/exchange';

export const exchangeApi = {
  createRequest: (listingId: string, input: CreateExchangeRequestInput) =>
    post<ListingRequestDto>(`${exchangePath}/listings/${encodeURIComponent(listingId)}/requests`, input),

  requestsForListing: (listingId: string) =>
    get<ListingRequestDto[]>(`${exchangePath}/listings/${encodeURIComponent(listingId)}/requests`),

  accept: (requestId: string, ownerId: string) =>
    post<HandoverDto>(
      `${exchangePath}/requests/${encodeURIComponent(requestId)}/accept?ownerId=${encodeURIComponent(ownerId)}`,
    ),

  code: (handoverId: string, userId: string) =>
    get<HandoverDto>(
      `${exchangePath}/handovers/${encodeURIComponent(handoverId)}/code?userId=${encodeURIComponent(userId)}`,
    ),

  confirm: (handoverId: string, input: ConfirmHandoverInput) =>
    post<HandoverDto>(
      `${exchangePath}/handovers/${encodeURIComponent(handoverId)}/confirm`,
      input,
    ),
};
