import { get, post } from './client';

export { exchangeApi } from './exchange';
export type {
  ConfirmHandoverInput,
  CreateExchangeRequestInput,
  ExchangeRequestStatus,
  HandoverDto,
  HandoverStatus,
  ListingRequestDto,
} from './exchange';

export interface ListingDto {
  id: string; ownerId: string; categoryId: string; categoryCode?: string; categoryName?: string; title: string; description?: string;
  quantityBand?: string; condition?: string; approxLatitude: number; approxLongitude: number;
  neighborhoodId?: string; status?: string; publishedAt?: string; createdAt?: string;
}
export interface CreateListingInput {
  ownerId: string; categoryId: string; title: string; description: string;
  quantityBand: string; condition: string; latitude: number; longitude: number; neighborhoodId?: string;
}
export interface CategoryDto { id: string; code: string; name: string; reusable: boolean; coinMultiplier: number; sortOrder: number; children?: CategoryDto[]; }
export interface WalletDto { userId: string; balance: number; totalEarned: number; dailyEarnedToday: number; monthlyEarnedThisMonth: number; dailyCapRemaining: number; monthlyCapRemaining: number; }
export interface UserBadgeDto { id: string; userId: string; badgeCode: string; badgeName: string; description?: string; iconKey?: string; earnedAt: string; }
export interface UserLevelDto { userId: string; level: number; totalPointsEarned: number; pointsToNextLevel: number; }
export interface CoinEntryDto { id: string; transactionId: string; account: string; amount: number; createdAt: string; }
export interface PageDto<T> { content: T[]; number: number; size: number; totalElements: number; totalPages: number; last: boolean; }
export interface QuestDto { id: string; period: string; code: string; title: string; targetMetric: string; targetValue: number; rewardCoins: number; }

export const catalogApi = {
  list: (query = '') => get<ListingDto[]>(`/catalog/listings${query ? `?${query}` : ''}`),
  get: (id: string) => get<ListingDto>(`/catalog/listings/${id}`),
  create: (input: CreateListingInput) => post<ListingDto>('/catalog/listings', input),
  publish: (id: string) => post<ListingDto>(`/catalog/listings/${id}/publish`),
  mine: (ownerId: string) => get<ListingDto[]>(`/catalog/listings/mine?ownerId=${encodeURIComponent(ownerId)}`),
  withdraw: (id: string, ownerId: string) => post<ListingDto>(`/catalog/listings/${id}/withdraw?ownerId=${encodeURIComponent(ownerId)}`),
  categories: () => get<CategoryDto[]>('/catalog/categories'),
};

export const ecoCoinApi = {
  wallet: (userId: string) => get<WalletDto>(`/ecocoin/wallet/${userId}`),
  entries: (userId: string, page = 0, size = 20) => get<PageDto<CoinEntryDto>>(`/ecocoin/wallet/${userId}/entries?page=${page}&size=${size}`),
};

export const gamificationApi = {
  badges: (userId: string) => get<UserBadgeDto[]>(`/gamification/users/${userId}/badges`),
  level: (userId: string) => get<UserLevelDto>(`/gamification/users/${userId}/level`),
  quests: (period?: string) => get<QuestDto[]>(`/gamification/quests${period ? `?period=${encodeURIComponent(period)}` : ''}`),
  leaderboard: (period?: string) => get<unknown[]>(`/gamification/leaderboard${period ? `?period=${encodeURIComponent(period)}` : ''}`),
};

export const wasteAiApi = {
  classify: (imageUrl: string, latitude?: number, longitude?: number) => post<unknown>('/wasteai/classify', { imageUrl, latitude, longitude }),
};
