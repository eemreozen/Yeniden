import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { requestOtp as requestOtpApi, verifyOtp as verifyOtpApi, refreshSession, logoutSession, getMe, updateMe } from '../api/auth';
import { setAccessToken, setRefreshHandler } from '../api/client';
import { clearRefreshToken, readRefreshToken, saveRefreshToken } from '../auth/tokenStorage';
import { toE164 } from '../auth/phone';
import { ApiError, IdentityUser, OtpChallenge } from '../api/types';
import { catalogApi, ListingDto, ecoCoinApi, gamificationApi, UserBadgeDto, QuestDto, CoinEntryDto, exchangeApi } from '../api/backend';

// TypeScript Interfaces from design guide
export type MaterialCategory = 'KOLI' | 'KARTON' | 'CAM' | 'AHSAP' | 'HOBI' | 'DIGER';

const toMaterialCategory = (code?: string): MaterialCategory => {
  const normalized = code?.trim().toUpperCase();
  if (normalized === 'AMBALAJ_KOLI') return 'KOLI';
  if (normalized === 'AMBALAJ_KARTON') return 'KARTON';
  if (normalized?.startsWith('CAM_')) return 'CAM';
  if (normalized?.startsWith('AHSAP_')) return 'AHSAP';
  if (normalized?.startsWith('HOBI_')) return 'HOBI';
  return normalized === 'KOLI' || normalized === 'KARTON' || normalized === 'CAM'
    || normalized === 'AHSAP' || normalized === 'HOBI' || normalized === 'DIGER'
    ? normalized
    : 'DIGER';
};

const mapBadge = (badge: UserBadgeDto): Badge => ({
  id: badge.id,
  title: badge.badgeName,
  description: badge.description ?? '',
  iconName: badge.iconKey ?? 'award',
  earnedAt: badge.earnedAt,
});

const mapQuest = (quest: QuestDto): Quest => ({
  id: quest.id,
  title: quest.title,
  targetMetric: quest.targetMetric,
  targetValue: quest.targetValue,
  rewardCoins: quest.rewardCoins,
});

const mapCoinEntry = (entry: CoinEntryDto): CoinEntry => ({
  id: entry.id,
  amount: entry.amount,
  createdAt: entry.createdAt,
});

export interface Badge {
  id: string;
  title: string;
  description: string;
  iconName: string;
  earnedAt: string;
}

export interface SustainabilityStats {
  co2SavedKg: number;
  waterSavedLiters: number;
  landfillDivertedKg: number;
}

export interface Quest {
  id: string;
  title: string;
  targetMetric: string;
  targetValue: number;
  rewardCoins: number;
}

export interface CoinEntry {
  id: string;
  amount: number;
  createdAt: string;
}

export interface User {
  id: string;
  phoneNumber: string;
  name: string;
  ecoCoinBalance: number;
  level: number;
  xp: number; // 0 to 100
  badges: Badge[];
  sharedCount: number;
  receivedCount: number;
  stats: SustainabilityStats;
}

export interface Ad {
  id: string;
  ownerId: string;
  ownerName: string;
  ownerLevel: number;
  title: string;
  description: string;
  category: MaterialCategory;
  mediaUrl: string;
  mediaType: 'photo' | 'video';
  approximateLocation: {
    latitude: number;
    longitude: number;
    radiusInMeters: number;
    regionName: string;
  };
  status: 'ACTIVE' | 'DELIVERED' | 'CANCELLED';
  createdAt: string;
}

export interface NeedAd {
  id: string;
  requesterId: string;
  requesterName: string;
  title: string;
  description?: string;
  category: MaterialCategory;
  status: 'ACTIVE' | 'RESOLVED';
  createdAt: string;
}

export interface Transaction {
  id: string;
  adId: string;
  adTitle: string;
  category: MaterialCategory;
  donorId: string;
  donorName: string;
  receiverId: string;
  receiverName: string;
  status: 'REQUESTED' | 'AGREED' | 'DELIVERED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
  handoverId?: string;
  confirmationCode?: string;
  handoverStatus?: 'PENDING_CODE' | 'CONFIRMED' | 'EXPIRED' | 'NO_SHOW' | 'PENDING_REVIEW';
  failedAttempts?: number;
  expiresAt?: string;
}

interface AppStateContextType {
  user: User | null;
  ads: Ad[];
  isLoadingAds: boolean;
  adsError: string | null;
  refreshAds: () => Promise<void>;
  isLoadingProfileStats: boolean;
  profileStatsError: string | null;
  refreshProfileStats: () => Promise<void>;
  quests: Quest[];
  coinEntries: CoinEntry[];
  needAds: NeedAd[];
  transactions: Transaction[];
  transactionsLoading: boolean;
  transactionsError: string | null;
  refreshTransactions: () => Promise<void>;
  acceptExchangeRequest: (transactionId: string) => Promise<void>;
  getHandoverCode: (transactionId: string) => Promise<string>;
  confirmHandover: (transactionId: string, code: string) => Promise<void>;
  isAuthenticated: boolean;
  onboardingRequired: boolean;
  isBootstrapping: boolean;
  authError: string | null;
  requestOtp: (phone: string) => Promise<OtpChallenge>;
  verifyOtp: (phone: string, code: string, challengeId: string) => Promise<void>;
  completeOnboarding: (displayName: string, avatarKey?: string) => Promise<void>;
  logout: () => Promise<void>;
  postAd: (title: string, category: MaterialCategory, description: string, imagePath: string) => Promise<void>;
  deleteAd: (adId: string) => Promise<void>;
  postNeedAd: (title: string, category: MaterialCategory, description: string) => void;
  sendInterest: (ad: Ad) => Promise<Transaction | null>;
  confirmAction: (transactionId: string, action: 'DONOR_CONFIRM' | 'RECEIVER_CONFIRM') => Promise<void>;
  loginDemo: () => void;
}

const AppStateContext = createContext<AppStateContextType | undefined>(undefined);

// Initial Mock Data
const INITIAL_BADGES: Badge[] = [
  { id: 'b1', title: 'İlk Adım', description: 'İlk malzemenizi paylaştınız.', iconName: 'sprout', earnedAt: '12.05.2026' },
  { id: 'b2', title: 'Su Dostu', description: 'Cam atıkları dönüştürerek su tasarrufu sağladınız.', iconName: 'droplet', earnedAt: '18.06.2026' },
  { id: 'b3', title: 'Paketçi', description: '10 adet karton koli paylaştınız.', iconName: 'package', earnedAt: '20.07.2026' },
];

const ALL_MOCK_BADGES: Badge[] = [
  ...INITIAL_BADGES,
  { id: 'b4', title: 'Yeşil Kahraman', description: '50 kg CO2 tasarrufu sağladınız.', iconName: 'trophy', earnedAt: '' },
  { id: 'b5', title: 'Usta Dağıtıcı', description: '20 farklı işlem tamamladınız.', iconName: 'award', earnedAt: '' },
];

const INITIAL_ADS: Ad[] = [
  {
    id: 'ad_1',
    ownerId: 'user_2',
    ownerName: 'Ahmet Y.',
    ownerLevel: 4,
    title: '15 Adet Temiz Taşınma Kolisi',
    description: 'Taşınma sonrası boşta kalan temiz, hasarsız karton koliler.',
    category: 'KOLI',
    mediaUrl: 'https://images.pexels.com/photos/7203699/pexels-photo-7203699.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9208,
      longitude: 32.8541,
      radiusInMeters: 400,
      regionName: 'Çankaya, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'ad_2',
    ownerId: 'user_3',
    ownerName: 'Selin K.',
    ownerLevel: 2,
    title: '10 Adet Cam Kavanoz (1 Litrelik)',
    description: 'Konserve veya hobi amaçlı kullanılabilecek yıkanmış cam kavanozlar.',
    category: 'CAM',
    mediaUrl: 'https://images.unsplash.com/photo-1543352632-fea6d4f83e78?auto=format&fit=crop&w=900&q=82',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9230,
      longitude: 32.8240,
      radiusInMeters: 500,
      regionName: 'Bahçelievler, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 3600000).toISOString(),
  },
  {
    id: 'ad_3',
    ownerId: 'user_4',
    ownerName: 'Can M.',
    ownerLevel: 5,
    title: 'Ahşap Palet Parçaları & Tahtalar',
    description: 'Kendin Yap (DIY) projeleri için uygun, zımparalanmaya hazır ahşaplar.',
    category: 'AHSAP',
    mediaUrl: 'https://images.pexels.com/photos/8817836/pexels-photo-8817836.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9650,
      longitude: 32.8090,
      radiusInMeters: 600,
      regionName: 'Yenimahalle, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 7200000).toISOString(),
  },
  {
    id: 'ad_4',
    ownerId: 'user_8',
    ownerName: 'Zeynep B.',
    ownerLevel: 3,
    title: 'Büyük Boy Arşiv Kutuları & Klasörler',
    description: 'Ofis kapanışından kalan sağlam karton klasörler ve arşiv kolileri.',
    category: 'KARTON',
    mediaUrl: 'https://images.pexels.com/photos/8371705/pexels-photo-8371705.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9179,
      longitude: 32.8627,
      radiusInMeters: 300,
      regionName: 'Kızılay, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 10800000).toISOString(),
  },
  {
    id: 'ad_5',
    ownerId: 'user_9',
    ownerName: 'Murat D.',
    ownerLevel: 1,
    title: 'Hobi / Seramik Çamuru Artıkları',
    description: 'Atölyeden artan, kurumamış formda değerlendirilebilir kil hamuru.',
    category: 'HOBI',
    mediaUrl: 'https://images.pexels.com/photos/34460213/pexels-photo-34460213.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9015,
      longitude: 32.8460,
      radiusInMeters: 450,
      regionName: 'Ayrancı, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 14400000).toISOString(),
  },
  {
    id: 'ad_6',
    ownerId: 'user_10',
    ownerName: 'Deniz A.',
    ownerLevel: 6,
    title: 'Şeffaf Ambalaj Baloncuklu Naylon',
    description: 'Hassas eşya paketlemede kullanılabilecek rulo baloncuklu naylon.',
    category: 'DIGER',
    mediaUrl: 'https://images.pexels.com/photos/3616731/pexels-photo-3616731.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9010,
      longitude: 32.8090,
      radiusInMeters: 550,
      regionName: 'Çukurambar, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 18000000).toISOString(),
  },
  {
    id: 'ad_7',
    ownerId: 'user_11',
    ownerName: 'Emre T.',
    ownerLevel: 2,
    title: 'Renkli Cam Şişeler (Decor Projeleri)',
    description: 'Süsleme veya ışıklandırma projesine uygun 8 adet koyu yeşil ve kahve şişe.',
    category: 'CAM',
    mediaUrl: 'https://images.pexels.com/photos/14567702/pexels-photo-14567702.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 40.0050,
      longitude: 32.8660,
      radiusInMeters: 350,
      regionName: 'Keçiören, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 21600000).toISOString(),
  },
  {
    id: 'ad_8',
    ownerId: 'demo-user',
    ownerName: 'Demo Kullanıcı',
    ownerLevel: 3,
    title: '12 Adet Taşınma Kolisi (Temiz & Katlanmış)',
    description: 'Ev taşıma sonrası boşta kalan çift oluklu mukavva koliler.',
    category: 'KOLI',
    mediaUrl: 'https://images.pexels.com/photos/7464201/pexels-photo-7464201.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9208,
      longitude: 32.8541,
      radiusInMeters: 200,
      regionName: 'Çankaya, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 7200000).toISOString(),
  },
  {
    id: 'ad_9',
    ownerId: 'demo-user',
    ownerName: 'Demo Kullanıcı',
    ownerLevel: 3,
    title: '10 Adet Cam Kavanoz',
    description: 'Kavanoz kapaklarıyla birlikte temiz durumda.',
    category: 'CAM',
    mediaUrl: 'https://images.pexels.com/photos/10420420/pexels-photo-10420420.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9208,
      longitude: 32.8541,
      radiusInMeters: 200,
      regionName: 'Çankaya, Ankara',
    },
    status: 'DELIVERED',
    createdAt: new Date(Date.now() - 86400000).toISOString(),
  },
  {
    id: 'ad_10',
    ownerId: 'user_12',
    ownerName: 'Oğuz H.',
    ownerLevel: 4,
    title: 'Paketleme Köpüğü ve Dolgu Malzemesi',
    description: 'Kırılabilir eşya paketlemesinde tekrar kullanılabilecek temiz polistiren dolgu malzemesi.',
    category: 'DIGER',
    mediaUrl: 'https://images.pexels.com/photos/18372333/pexels-photo-18372333.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9460,
      longitude: 32.6700,
      radiusInMeters: 400,
      regionName: 'Etimesgut, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 28800000).toISOString(),
  },
  {
    id: 'ad_11',
    ownerId: 'user_13',
    ownerName: 'Tuğba Ş.',
    ownerLevel: 5,
    title: 'Ahşap Meyve Kasaları (Dekoratif)',
    description: 'Kitaplık veya saksılık yapılabilecek zımparalanmış ham ahşap kasalar.',
    category: 'AHSAP',
    mediaUrl: 'https://images.pexels.com/photos/5879/wooden-boxes-vintage-wood-old.jpg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.8970,
      longitude: 32.7040,
      radiusInMeters: 500,
      regionName: 'Ümitköy, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 36000000).toISOString(),
  },
  {
    id: 'ad_12',
    ownerId: 'user_14',
    ownerName: 'Elif N.',
    ownerLevel: 4,
    title: 'Küçülen Çocuk Kıyafetleri (4-6 Yaş)',
    description: 'Temiz ve lekesiz; kazak, pantolon ve iki tişörtten oluşan mevsimlik çocuk kıyafeti paketi.',
    category: 'DIGER',
    mediaUrl: 'https://images.pexels.com/photos/7055870/pexels-photo-7055870.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9348,
      longitude: 32.8597,
      radiusInMeters: 350,
      regionName: 'Kavaklıdere, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 4200000).toISOString(),
  },
  {
    id: 'ad_13',
    ownerId: 'user_15',
    ownerName: 'Derya P.',
    ownerLevel: 3,
    title: 'Renkli Kumaş ve Keçe Parçaları',
    description: 'Dikiş, kukla ve okul projelerinde kullanılabilecek farklı desen ve boylarda kumaş parçaları.',
    category: 'HOBI',
    mediaUrl: 'https://images.pexels.com/photos/18359551/pexels-photo-18359551.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.8875,
      longitude: 32.8562,
      radiusInMeters: 450,
      regionName: 'Dikmen, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 7800000).toISOString(),
  },
  {
    id: 'ad_14',
    ownerId: 'user_16',
    ownerName: 'Berk E.',
    ownerLevel: 2,
    title: 'Kullanılabilir Saksılar ve Fide Kapları',
    description: 'Balkon düzenlemesinden kalan farklı boylarda sağlam saksılar; temizlenip kullanıma hazır.',
    category: 'HOBI',
    mediaUrl: 'https://images.pexels.com/photos/37397141/pexels-photo-37397141.jpeg?auto=compress&cs=tinysrgb&w=900',
    mediaType: 'photo',
    approximateLocation: {
      latitude: 39.9784,
      longitude: 32.8731,
      radiusInMeters: 500,
      regionName: 'Aydınlıkevler, Ankara',
    },
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 13200000).toISOString(),
  },
];

const INITIAL_NEED_ADS: NeedAd[] = [
  {
    id: 'need_1',
    requesterId: 'user_5',
    requesterName: 'Merve G.',
    title: 'Sanat projesi için eski gazete/karton',
    description: 'Okul projesi kapsamında kağıt hamuru yapmak için çok miktarda gazete ve kartona ihtiyacım var.',
    category: 'KARTON',
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'need_2',
    requesterId: 'user_6',
    requesterName: 'Bora T.',
    title: 'Çiçek saksısı için boş kavanozlar',
    description: 'Sukulent yetiştirmek için cam kavanoz veya plastik kap arıyorum.',
    category: 'CAM',
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 5000000).toISOString(),
  },
  {
    id: 'need_3',
    requesterId: 'user_14',
    requesterName: 'Gizem R.',
    title: 'Ev taşıma için 20 adet orta boy koli',
    description: 'Önümüzdeki hafta Çankaya içinde elden teslim alabilirim.',
    category: 'KOLI',
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 12000000).toISOString(),
  },
  {
    id: 'need_4',
    requesterId: 'user_15',
    requesterName: 'Kaan Ö.',
    title: 'Maket yapımı için atık kontrplak / ahşap',
    description: 'Mimarlık staj projesi için ince parça ahşap veya sunta arıyorum.',
    category: 'AHSAP',
    status: 'ACTIVE',
    createdAt: new Date(Date.now() - 18000000).toISOString(),
  },
];

const INITIAL_TRANSACTIONS: Transaction[] = [
  {
    id: 'tx_1',
    adId: 'ad_6',
    adTitle: 'Şeffaf Ambalaj Baloncuklu Naylon',
    category: 'DIGER',
    donorId: 'user_10',
    donorName: 'Deniz A.',
    receiverId: 'demo-user',
    receiverName: 'Demo Kullanıcı',
    status: 'AGREED',
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 36000000).toISOString(),
  },
  {
    id: 'tx_2',
    adId: 'ad_1',
    adTitle: '15 Adet Temiz Taşınma Kolisi',
    category: 'KOLI',
    donorId: 'user_2',
    donorName: 'Ahmet Y.',
    receiverId: 'demo-user',
    receiverName: 'Demo Kullanıcı',
    status: 'REQUESTED',
    createdAt: new Date(Date.now() - 18000000).toISOString(),
    updatedAt: new Date(Date.now() - 18000000).toISOString(),
  },
  {
    id: 'tx_3',
    adId: 'ad_3',
    adTitle: 'Ahşap Palet Parçaları & Tahtalar',
    category: 'AHSAP',
    donorId: 'user_4',
    donorName: 'Can M.',
    receiverId: 'demo-user',
    receiverName: 'Demo Kullanıcı',
    status: 'AGREED',
    createdAt: new Date(Date.now() - 43200000).toISOString(),
    updatedAt: new Date(Date.now() - 14400000).toISOString(),
  },
  {
    id: 'tx_4',
    adId: 'ad_9',
    adTitle: '10 Adet Cam Kavanoz (1 Litrelik)',
    category: 'CAM',
    donorId: 'demo-user',
    donorName: 'Demo Kullanıcı',
    receiverId: 'user_3',
    receiverName: 'Selin K.',
    status: 'DELIVERED',
    createdAt: new Date(Date.now() - 172800000).toISOString(),
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
  },
  {
    id: 'tx_5',
    adId: 'ad_8',
    adTitle: '12 Adet Taşınma Kolisi (Temiz & Katlanmış)',
    category: 'KOLI',
    donorId: 'demo-user',
    donorName: 'Demo Kullanıcı',
    receiverId: 'user_5',
    receiverName: 'Merve G.',
    status: 'DELIVERED',
    createdAt: new Date(Date.now() - 345600000).toISOString(),
    updatedAt: new Date(Date.now() - 259200000).toISOString(),
  },
  {
    id: 'tx_6',
    adId: 'ad_5',
    adTitle: 'Hobi / Seramik Çamuru Artıkları',
    category: 'HOBI',
    donorId: 'user_9',
    donorName: 'Murat D.',
    receiverId: 'demo-user',
    receiverName: 'Demo Kullanıcı',
    status: 'DELIVERED',
    createdAt: new Date(Date.now() - 518400000).toISOString(),
    updatedAt: new Date(Date.now() - 432000000).toISOString(),
  },
  {
    id: 'tx_7',
    adId: 'ad_6',
    adTitle: 'Şeffaf Ambalaj Baloncuklu Naylon',
    category: 'DIGER',
    donorId: 'user_10',
    donorName: 'Deniz A.',
    receiverId: 'demo-user',
    receiverName: 'Demo Kullanıcı',
    status: 'CANCELLED',
    createdAt: new Date(Date.now() - 604800000).toISOString(),
    updatedAt: new Date(Date.now() - 518400000).toISOString(),
  },
];

export const DEMO_MODE = true;

export const DEMO_USER: User = {
  id: 'demo-user',
  phoneNumber: '+90 555 123 45 67',
  name: 'Doğa Dostu',
  ecoCoinBalance: 320,
  level: 3,
  xp: 45,
  badges: INITIAL_BADGES,
  sharedCount: 8,
  receivedCount: 3,
  stats: {
    co2SavedKg: 14.8,
    waterSavedLiters: 320,
    landfillDivertedKg: 12.0,
  },
};

export const AppStateProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(DEMO_MODE ? DEMO_USER : null);
  const [ads, setAds] = useState<Ad[]>(DEMO_MODE ? INITIAL_ADS : []);
  const [isLoadingAds, setIsLoadingAds] = useState(false);
  const [adsError, setAdsError] = useState<string | null>(null);
  const [isLoadingProfileStats, setIsLoadingProfileStats] = useState(false);
  const [profileStatsError, setProfileStatsError] = useState<string | null>(null);
  const [quests, setQuests] = useState<Quest[]>([]);
  const [coinEntries, setCoinEntries] = useState<CoinEntry[]>([]);
  const [needAds, setNeedAds] = useState<NeedAd[]>(DEMO_MODE ? INITIAL_NEED_ADS : []);
  const [transactions, setTransactions] = useState<Transaction[]>(DEMO_MODE ? INITIAL_TRANSACTIONS : []);
  const [transactionsLoading, setTransactionsLoading] = useState(false);
  const [transactionsError, setTransactionsError] = useState<string | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(DEMO_MODE);
  const [onboardingRequired, setOnboardingRequired] = useState(false);
  const [isBootstrapping, setIsBootstrapping] = useState(!DEMO_MODE);
  const [authError, setAuthError] = useState<string | null>(null);

  const applyIdentityUser = (identity: IdentityUser, newUser = false) => {
    setUser(prev => ({
      id: identity.id,
      phoneNumber: identity.phone,
      name: identity.displayName || identity.phone,
      ecoCoinBalance: prev?.ecoCoinBalance ?? 0,
      level: prev?.level ?? 1,
      xp: prev?.xp ?? 0,
      badges: prev?.badges ?? [],
      sharedCount: prev?.sharedCount ?? 0,
      receivedCount: prev?.receivedCount ?? 0,
      stats: prev?.stats ?? { co2SavedKg: 0, waterSavedLiters: 0, landfillDivertedKg: 0 },
    }));
    setIsAuthenticated(true);
    setOnboardingRequired(newUser || identity.displayName === 'Yeni Kullanıcı');
  };

  const mapListingToAd = (listing: ListingDto): Ad => ({
    id: listing.id,
    ownerId: listing.ownerId,
    ownerName: 'Yeniden kullanıcısı',
    ownerLevel: 1,
    title: listing.title,
    description: listing.description ?? '',
    // Catalog currently exposes categoryId but not its human-readable code.
    // Keep the listing visible and use the neutral bucket until that contract
    // is added; never invent a category from a UUID.
    category: toMaterialCategory(listing.categoryCode),
    mediaUrl: 'https://images.unsplash.com/photo-1530587191325-3db32d826c18?w=500',
    mediaType: 'photo',
    approximateLocation: {
      latitude: listing.approxLatitude,
      longitude: listing.approxLongitude,
      radiusInMeters: 400,
      regionName: 'Yaklaşık konum',
    },
    status: listing.status === 'PUBLISHED' ? 'ACTIVE' : listing.status === 'EXPIRED' ? 'CANCELLED' : 'DELIVERED',
    createdAt: listing.createdAt ?? listing.publishedAt ?? new Date().toISOString(),
  });

  const refreshAds = useCallback(async () => {
    if (!isAuthenticated || !user?.id || user.id === 'pending-profile' || user.id === 'demo-user') return;
    setIsLoadingAds(true);
    setAdsError(null);
    try {
      const listings = await catalogApi.list();
      setAds(listings.map(mapListingToAd));
    } catch (error) {
      setAds([]);
      setAdsError(error instanceof ApiError ? error.message : 'İlanlar yüklenemedi.');
    } finally {
      setIsLoadingAds(false);
    }
  }, [isAuthenticated, user?.id]);

  useEffect(() => {
    if (isAuthenticated) refreshAds().catch(() => undefined);
  }, [isAuthenticated, refreshAds]);

  const userId = user?.id;
  const refreshProfileStats = useCallback(async () => {
    if (!userId || userId === 'pending-profile' || userId === 'demo-user') return;
    setIsLoadingProfileStats(true);
    setProfileStatsError(null);
    try {
      const [wallet, badges, level] = await Promise.all([
        ecoCoinApi.wallet(userId),
        gamificationApi.badges(userId),
        gamificationApi.level(userId),
      ]);
      setUser(previous => previous && previous.id === userId ? {
        ...previous,
        ecoCoinBalance: wallet.balance,
        level: level.level,
        xp: level.totalPointsEarned % 100,
        badges: badges.map(mapBadge),
      } : previous);
      try {
        const entries = await ecoCoinApi.entries(userId, 0, 5);
        setCoinEntries(entries.content.map(mapCoinEntry));
      } catch {
        setCoinEntries([]);
      }
      try {
        setQuests((await gamificationApi.quests()).map(mapQuest));
      } catch {
        // Quest availability must not hide the wallet, badges, or level.
        setQuests([]);
      }
    } catch (error) {
      setProfileStatsError(error instanceof ApiError ? error.message : 'Profil istatistikleri yüklenemedi.');
    } finally {
      setIsLoadingProfileStats(false);
    }
  }, [userId]);

  useEffect(() => {
    if (isAuthenticated && userId) refreshProfileStats().catch(() => undefined);
  }, [isAuthenticated, userId, refreshProfileStats]);

  useEffect(() => {
    setRefreshHandler(async () => {
      const stored = await readRefreshToken();
      if (!stored) return false;
      try {
        const tokens = await refreshSession(stored);
        setAccessToken(tokens.accessToken);
        await saveRefreshToken(tokens.refreshToken);
        return true;
      } catch {
        setAccessToken(null);
        await clearRefreshToken();
        setUser(null);
        setIsAuthenticated(false);
        return false;
      }
    });
    const restoreSession = async () => {
      if (DEMO_MODE) {
        setIsBootstrapping(false);
        return;
      }
      const stored = await readRefreshToken();
      if (!stored) return;
      try {
        const tokens = await refreshSession(stored);
        setAccessToken(tokens.accessToken);
        await saveRefreshToken(tokens.refreshToken);
        applyIdentityUser(await getMe());
      } catch {
        setAccessToken(null);
        await clearRefreshToken();
      }
    };
    restoreSession().finally(() => setIsBootstrapping(false));
    return () => setRefreshHandler(null);
  }, []);

  const requestOtp = async (phone: string) => {
    setAuthError(null);
    try { return await requestOtpApi(toE164(phone)); }
    catch (error) { const message = error instanceof ApiError ? error.message : 'Kod gönderilemedi.'; setAuthError(message); throw error; }
  };

  const verifyOtp = async (phone: string, code: string, challengeId: string) => {
    setAuthError(null);
    let tokensAccepted = false;
    try {
      const tokens = await verifyOtpApi(toE164(phone), code, challengeId);
      tokensAccepted = true;
      setAccessToken(tokens.accessToken);
      await saveRefreshToken(tokens.refreshToken);
      try {
        applyIdentityUser(await getMe(), tokens.isNewUser);
      } catch {
        // OTP/session are valid already. A profile that still has the
        // backend default name needs onboarding; do not report this as an
        // invalid OTP (the user may have retried registration before).
        setUser({ id: 'pending-profile', phoneNumber: toE164(phone), name: 'Yeni Kullanıcı', ecoCoinBalance: 0, level: 1, xp: 0, badges: [], sharedCount: 0, receivedCount: 0, stats: { co2SavedKg: 0, waterSavedLiters: 0, landfillDivertedKg: 0 } });
        setIsAuthenticated(true);
        setOnboardingRequired(true);
        return;
      }
    } catch (error) {
      const message = tokensAccepted
        ? (error instanceof ApiError ? `Kod doğrulandı ancak profil alınamadı (${error.code}).` : 'Kod doğrulandı ancak profil alınamadı.')
        : (error instanceof ApiError ? error.message : 'Kod doğrulanamadı.');
      setAuthError(message);
      throw error;
    }
  };

  const completeOnboarding = async (displayName: string, avatarKey?: string) => {
    setAuthError(null);
    try {
      const identity = await updateMe({ displayName: displayName.trim(), avatarKey });
      applyIdentityUser(identity, false);
    } catch (error) {
      const message = error instanceof ApiError
        ? `Profil tamamlanamadı (${error.status}/${error.code}): ${error.message}`
        : `Profil tamamlanamadı: ${error instanceof Error ? error.message : 'Bilinmeyen hata'}`;
      console.error('[onboarding] profile update failed', error);
      setAuthError(message);
      throw error;
    }
  };

  const loginDemo = () => {
    setUser(DEMO_USER);
    setAds(INITIAL_ADS);
    setNeedAds(INITIAL_NEED_ADS);
    setTransactions(INITIAL_TRANSACTIONS);
    setIsAuthenticated(true);
    setOnboardingRequired(false);
    setIsBootstrapping(false);
  };

  // Load default user when authenticated
  const logout = async () => {
    if (user?.id === 'demo-user') {
      setUser(null);
      setIsAuthenticated(false);
      setOnboardingRequired(false);
      return;
    }
    const stored = await readRefreshToken();
    try { if (stored) await logoutSession(stored); } finally {
      setAccessToken(null);
      await clearRefreshToken();
      setUser(null);
      setIsAuthenticated(false);
      setOnboardingRequired(false);
    }
  };

  // 1. Yeni İlan Paylaş
  const localPostAd = (title: string, category: MaterialCategory, description: string, imagePath: string) => {
    if (!user) return;
    const newAd: Ad = {
      id: `ad_${Date.now()}`,
      ownerId: user.id,
      ownerName: user.name,
      ownerLevel: user.level,
      title: title || `${category} Malzemesi`,
      description: description || 'Detay belirtilmedi.',
      category,
      mediaUrl: imagePath || 'https://images.unsplash.com/photo-1530587191325-3db32d826c18?w=500',
      mediaType: 'photo',
      approximateLocation: {
        latitude: 39.9208 + (Math.random() - 0.5) * 0.02,
        longitude: 32.8541 + (Math.random() - 0.5) * 0.02,
        radiusInMeters: 450,
        regionName: 'Çankaya, Ankara',
      },
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    };

    setAds([newAd, ...ads]);

    // Give XP and stats for posting
    setUser(prev => {
      if (!prev) return null;
      let newXp = prev.xp + 15;
      let newLevel = prev.level;
      if (newXp >= 100) {
        newLevel += 1;
        newXp -= 100;
      }
      return {
        ...prev,
        xp: newXp,
        level: newLevel,
        sharedCount: prev.sharedCount + 1,
        ecoCoinBalance: prev.ecoCoinBalance + 10, // 10 coins for publishing
      };
    });
  };

  // 2. Kendi İlanını Sil
  const localDeleteAd = (adId: string) => {
    setAds(ads.filter(ad => ad.id !== adId));
  };

  // 3. İhtiyacım Var İlanı Aç
  const postNeedAd = (title: string, category: MaterialCategory, description: string) => {
    if (user?.id === 'demo-user') {
      const newNeed: NeedAd = {
        id: `need_${Date.now()}`,
        requesterId: user.id,
        requesterName: user.name,
        title,
        description,
        category,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      };
      setNeedAds(previous => [newNeed, ...previous]);
      return;
    }
    // The current backend contract has no needs-listing endpoint yet.
    // Do not create a local/demo record that would disappear on reload.
  };

  // 4. Haritadaki İlana İlgi Talebi Gönder
  const localSendInterest = (ad: Ad) => {
    if (!user) return null;
    const newTx: Transaction = {
      id: `tx_${Date.now()}`,
      adId: ad.id,
      adTitle: ad.title,
      category: ad.category,
      donorId: ad.ownerId,
      donorName: ad.ownerName,
      receiverId: user.id,
      receiverName: user.name,
      status: user.id === 'demo-user' ? 'AGREED' : 'REQUESTED',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    setTransactions(previous => [newTx, ...previous]);
    return newTx;
  };

  // 5. İşlemi Onayla (Teslim Aldım veya Teslim Ettim)
  const localConfirmAction = (transactionId: string, action: 'DONOR_CONFIRM' | 'RECEIVER_CONFIRM') => {
    setTransactions(prevTxs => {
      return prevTxs.map(tx => {
        if (tx.id === transactionId) {
          // Both parties simulated to agree: Mark DELIVERED instantly
          const isFinished = true; // Simulating successful handshake instantly
          if (isFinished) {
            // Update User Stats
            setUser(currentUser => {
              if (!currentUser) return null;
              
              // Increment XP and EcoCoins
              let newXp = currentUser.xp + 35;
              let newLevel = currentUser.level;
              if (newXp >= 100) {
                newLevel += 1;
                newXp -= 100;
              }

              // Environmental Stats calculation based on category
              let co2 = 0.5;
              let water = 0;
              let waste = 1.0;

              if (tx.category === 'CAM') {
                water = 50; // 50 liters saved per transaction
                co2 = 1.2;
              } else if (tx.category === 'KARTON' || tx.category === 'KOLI') {
                co2 = 2.5; // 2.5 kg CO2 saved
                waste = 3.0;
              }

              // Earn a new badge if level increases to 4
              const earnedBadges = [...currentUser.badges];
              if (newLevel >= 4 && !earnedBadges.some(b => b.id === 'b4')) {
                const badge4 = ALL_MOCK_BADGES.find(b => b.id === 'b4');
                if (badge4) {
                  earnedBadges.push({ ...badge4, earnedAt: new Date().toLocaleDateString('tr-TR') });
                }
              }

              return {
                ...currentUser,
                ecoCoinBalance: currentUser.ecoCoinBalance + 50, // 50 coins reward
                level: newLevel,
                xp: newXp,
                badges: earnedBadges,
                receivedCount: action === 'RECEIVER_CONFIRM' ? currentUser.receivedCount + 1 : currentUser.receivedCount,
                sharedCount: action === 'DONOR_CONFIRM' ? currentUser.sharedCount + 1 : currentUser.sharedCount,
                stats: {
                  co2SavedKg: Number((currentUser.stats.co2SavedKg + co2).toFixed(1)),
                  waterSavedLiters: currentUser.stats.waterSavedLiters + water,
                  landfillDivertedKg: Number((currentUser.stats.landfillDivertedKg + waste).toFixed(1)),
                }
              };
            });

            // Mark the corresponding ad as DELIVERED in main listings
            setAds(prevAds => {
              return prevAds.map(ad => {
                if (ad.id === tx.adId) {
                  return { ...ad, status: 'DELIVERED' };
                }
                return ad;
              });
            });

            return { ...tx, status: 'DELIVERED', updatedAt: new Date().toISOString() };
          }
        }
        return tx;
      });
    });
  };

  // Production API actions. Demo/local implementations above are retained only
  // as historical reference and are never exposed by the provider.
  const categoryIds: Record<MaterialCategory, string> = {
    KOLI: '11111111-1111-1111-1111-111111111102', KARTON: '11111111-1111-1111-1111-111111111103',
    CAM: '11111111-1111-1111-1111-111111111201', AHSAP: '11111111-1111-1111-1111-111111111301',
    HOBI: '11111111-1111-1111-1111-111111111401', DIGER: '11111111-1111-1111-1111-111111111501',
  };
  const postAd = async (title: string, category: MaterialCategory, description: string, imagePath: string) => {
    if (!user) return;
    if (user.id === 'demo-user') {
      localPostAd(title, category, description, imagePath);
      return;
    }
    const draft = await catalogApi.create({ ownerId: user.id, categoryId: categoryIds[category], title: title.trim(), description: description.trim(), quantityBand: 'SINGLE', condition: 'USABLE', latitude: 39.9208, longitude: 32.8541 });
    await catalogApi.publish(draft.id);
    await refreshAds();
    await refreshProfileStats();
  };
  const deleteAd = async (adId: string) => {
    if (!user) return;
    if (user.id === 'demo-user') {
      localDeleteAd(adId);
      return;
    }
    await catalogApi.withdraw(adId, user.id);
    await refreshAds();
  };
  const refreshTransactions = useCallback(async () => {
    if (!user || user.id === 'pending-profile' || user.id === 'demo-user') return;
    setTransactionsLoading(true);
    setTransactionsError(null);
    try {
      const ownListings = await catalogApi.mine(user.id);
      const requestGroups = await Promise.all(ownListings.map(listing => exchangeApi.requestsForListing(listing.id)));
      const listingById = new Map(ownListings.map(listing => [listing.id, listing]));
      const ownedTransactions = requestGroups.flat().map(dto => {
        const listing = listingById.get(dto.listingId);
        const status: Transaction['status'] = dto.status === 'PENDING' ? 'REQUESTED' : dto.status === 'ACCEPTED' ? 'AGREED' : 'CANCELLED';
        return {
          id: dto.id, adId: dto.listingId, adTitle: listing?.title ?? 'İlan',
          category: toMaterialCategory(listing?.categoryCode), donorId: dto.ownerId, donorName: user.name,
          receiverId: dto.requesterId, receiverName: 'Talep eden kullanıcı', status,
          createdAt: dto.createdAt ?? new Date().toISOString(), updatedAt: dto.createdAt ?? new Date().toISOString(),
        };
      });
      setTransactions(previous => {
        const requesterSide = previous.filter(item => item.receiverId === user.id && !ownedTransactions.some(owned => owned.id === item.id));
        return [...ownedTransactions, ...requesterSide];
      });
    } catch (error) {
      setTransactionsError(error instanceof ApiError ? error.message : 'İşlemler yüklenemedi.');
    } finally { setTransactionsLoading(false); }
  }, [user]);

  useEffect(() => {
    if (isAuthenticated && user?.id && user.id !== 'pending-profile' && user.id !== 'demo-user') refreshTransactions().catch(() => undefined);
  }, [isAuthenticated, user?.id, refreshTransactions]);

  const sendInterest = async (ad: Ad) => {
    if (!user) return null;
    if (user.id === 'demo-user') {
      return localSendInterest(ad);
    }
    const dto = await exchangeApi.createRequest(ad.id, { requesterId: user.id, ownerId: ad.ownerId, message: 'İlanınızla ilgileniyorum.' });
    const tx: Transaction = { id: dto.id, adId: dto.listingId, adTitle: ad.title, category: ad.category, donorId: dto.ownerId, donorName: ad.ownerName, receiverId: dto.requesterId, receiverName: user.name, status: dto.status === 'ACCEPTED' ? 'AGREED' : 'REQUESTED', createdAt: dto.createdAt ?? new Date().toISOString(), updatedAt: dto.createdAt ?? new Date().toISOString() };
    setTransactions(previous => [tx, ...previous.filter(item => item.id !== tx.id)]);
    return tx;
  };
  const acceptExchangeRequest = async (transactionId: string) => {
    if (!user) return;
    setTransactionsError(null);
    try {
      const handover = await exchangeApi.accept(transactionId, user.id);
      setTransactions(previous => previous.map(item => item.id === transactionId ? {
        ...item, status: 'AGREED', handoverId: handover.id, handoverStatus: handover.status,
        failedAttempts: handover.failedAttempts, expiresAt: handover.expiresAt, updatedAt: new Date().toISOString(),
      } : item));
      await refreshAds();
    } catch (error) {
      const message = error instanceof ApiError ? error.message : 'Talep kabul edilemedi.';
      setTransactionsError(message);
      throw error;
    }
  };
  const getHandoverCode = async (transactionId: string) => {
    if (!user) throw new Error('Oturum bulunamadı.');
    const transaction = transactions.find(item => item.id === transactionId);
    if (!transaction?.handoverId) throw new Error('Teslimat kaydı henüz oluşturulmadı.');
    try {
      const handover = await exchangeApi.code(transaction.handoverId, user.id);
      const code = handover.confirmationCode ?? '';
      setTransactions(previous => previous.map(item => item.id === transactionId ? { ...item, confirmationCode: code, handoverStatus: handover.status, failedAttempts: handover.failedAttempts, expiresAt: handover.expiresAt } : item));
      return code;
    } catch (error) {
      setTransactionsError(error instanceof ApiError ? error.message : 'Teslimat kodu alınamadı.');
      throw error;
    }
  };
  const confirmHandover = async (transactionId: string, code: string) => {
    if (!user) return;
    const transaction = transactions.find(item => item.id === transactionId);
    if (!transaction?.handoverId) throw new Error('Teslimat kaydı henüz oluşturulmadı.');
    try {
      const handover = await exchangeApi.confirm(transaction.handoverId, { providerId: user.id, code });
      setTransactions(previous => previous.map(item => item.id === transactionId ? { ...item, status: handover.status === 'CONFIRMED' ? 'DELIVERED' : item.status, handoverStatus: handover.status, failedAttempts: handover.failedAttempts, updatedAt: new Date().toISOString() } : item));
      await Promise.all([refreshAds(), refreshProfileStats()]);
    } catch (error) {
      setTransactionsError(error instanceof ApiError ? error.message : 'Teslimat onaylanamadı.');
      throw error;
    }
  };
  const confirmAction = async (transactionId: string, action: 'DONOR_CONFIRM' | 'RECEIVER_CONFIRM') => {
    if (user?.id === 'demo-user') {
      localConfirmAction(transactionId, action);
      return;
    }
    // Confirmation requires the handover id and one-time code returned by the
    // accept/code endpoints. The agreement screen only exposes this action once
    // a handover has been created; refresh server state after that operation.
    await refreshProfileStats();
    await refreshAds();
  };

  return (
    <AppStateContext.Provider value={{
      user,
      ads,
      isLoadingAds,
      adsError,
      refreshAds,
      isLoadingProfileStats,
      profileStatsError,
      refreshProfileStats,
      quests,
      coinEntries,
      needAds,
      transactions,
      transactionsLoading,
      transactionsError,
      refreshTransactions,
      acceptExchangeRequest,
      getHandoverCode,
      confirmHandover,
      isAuthenticated,
      onboardingRequired,
      logout,
      isBootstrapping,
      authError,
      requestOtp,
      verifyOtp,
      completeOnboarding,
      postAd,
      deleteAd,
      postNeedAd,
      sendInterest,
      confirmAction,
      loginDemo,
    }}>
      {children}
    </AppStateContext.Provider>
  );
};

export const useAppState = () => {
  const context = useContext(AppStateContext);
  if (!context) {
    throw new Error('useAppState must be used within an AppStateProvider');
  }
  return context;
};
