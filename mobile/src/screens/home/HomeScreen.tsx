import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  StatusBar,
  FlatList,
  ScrollView,
  Image,
  TextInput,
  Modal,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  Search,
  MapPin,
  Filter,
  Package,
  Sparkles,
  ChevronRight,
  X,
  User as UserIcon,
  Heart,
  HandHeart,
} from 'lucide-react-native';
import { theme } from '../../theme/theme';
import { useAppState, Ad, MaterialCategory } from '../../context/AppStateContext';
import { useTab } from '../../navigation/AppNavigator';

// ---------------------------------------------------------------------------
// Helpers & Data
// ---------------------------------------------------------------------------
const CATEGORIES: { id: string; label: string; key: MaterialCategory | 'ALL' }[] = [
  { id: 'all', label: 'Tümü', key: 'ALL' },
  { id: 'koli', label: 'Koli & Kutu', key: 'KOLI' },
  { id: 'karton', label: 'Karton', key: 'KARTON' },
  { id: 'cam', label: 'Cam Şişe/Kavanoz', key: 'CAM' },
  { id: 'ahsap', label: 'Ahşap & Palet', key: 'AHSAP' },
  { id: 'hobi', label: 'Hobi / DİY', key: 'HOBI' },
  { id: 'diger', label: 'Diğer Atıklar', key: 'DIGER' },
];

const CAT_LABELS: Record<string, string> = {
  KOLI: 'Koli', KARTON: 'Karton', CAM: 'Cam',
  AHSAP: 'Ahşap', HOBI: 'Hobi', DIGER: 'Diğer',
};

// ---------------------------------------------------------------------------
// HomeScreen
// ---------------------------------------------------------------------------
export const HomeScreen: React.FC<{ navigation: any }> = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { user, ads, deleteAd, sendInterest, isLoadingAds, adsError, refreshAds } = useAppState();
  const { setActiveTab, openAgreement } = useTab();

  const [selectedCat, setSelectedCat] = useState<MaterialCategory | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [favoriteIds, setFavoriteIds] = useState<string[]>([]);
  const [selectedAdModal, setSelectedAdModal] = useState<Ad | null>(null);
  const [requestSuccessModal, setRequestSuccessModal] = useState(false);

  // Filtered ads list for feed
  const activeAds = ads.filter(ad => ad.status === 'ACTIVE');
  const filteredAds = activeAds.filter(ad => {
    const matchesCat = selectedCat === 'ALL' || ad.category === selectedCat;
    const matchesSearch =
      ad.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      ad.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      ad.approximateLocation.regionName.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

  const toggleFavorite = (id: string) => {
    setFavoriteIds(prev =>
      prev.includes(id) ? prev.filter(item => item !== id) : [...prev, id]
    );
  };

  const handleSendInterest = async (ad: Ad) => {
    try {
      const transaction = await sendInterest(ad);
      setSelectedAdModal(null);
      if (transaction?.status === 'AGREED') { openAgreement(transaction.id); return; }
      setRequestSuccessModal(true);
    } catch { setSelectedAdModal(null); }
  };

  // Render individual item in Letgo feed grid
  const renderFeedCard = ({ item }: { item: Ad }) => {
    const isFav = favoriteIds.includes(item.id);

    return (
      <TouchableOpacity
        style={styles.feedCard}
        activeOpacity={0.88}
        onPress={() => setSelectedAdModal(item)}
      >
        {/* Card Image */}
        <View style={styles.cardImageContainer}>
          <Image
            source={{ uri: item.mediaUrl }}
            style={styles.cardImage}
            resizeMode="cover"
          />
          {/* Top category badge */}
          <View style={styles.catBadge}>
            <Text style={styles.catBadgeText}>{CAT_LABELS[item.category] ?? item.category}</Text>
          </View>
          {/* Favorite button */}
          <TouchableOpacity
            style={styles.favBtn}
            activeOpacity={0.7}
            onPress={() => toggleFavorite(item.id)}
          >
            <Heart
              size={18}
              color={isFav ? '#E53935' : '#57685B'}
              fill={isFav ? '#E53935' : 'transparent'}
            />
          </TouchableOpacity>
        </View>

        {/* Card Info */}
        <View style={styles.cardBody}>
          <Text style={styles.cardTitle} numberOfLines={2}>
            {item.title}
          </Text>

          <View style={styles.cardLocationRow}>
            <MapPin size={13} color={theme.colors.inkFaint} />
            <Text style={styles.cardLocationText} numberOfLines={1}>
              {item.approximateLocation.regionName}
            </Text>
          </View>

          <View style={styles.cardFooterRow}>
            <Text style={styles.freeBadge}>Ücretsiz / Bağış</Text>
            <Text style={styles.ownerText}>{item.ownerName}</Text>
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <StatusBar barStyle="dark-content" backgroundColor={theme.colors.bg} />

      {/* Header bar */}
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <View style={styles.logoBadge}>
            <Image
              source={require('../../assets/yeniden_logo.png')}
              style={styles.headerLogoImage}
              resizeMode="contain"
            />
          </View>
          <View>
            <Text style={styles.brandTitle}>Yeniden</Text>
            <Text style={styles.brandSubtitle}>Ankara yeniden kullanım ağı</Text>
          </View>
        </View>

        <TouchableOpacity
          style={styles.profileFab}
          onPress={() => setActiveTab('profile')}
          activeOpacity={0.8}
        >
          <UserIcon size={18} color={theme.colors.primary} />
        </TouchableOpacity>
      </View>

      {/* Search Bar with Filter Button */}
      <View style={styles.searchSection}>
        <View style={styles.searchBar}>
          <Search size={18} color={theme.colors.inkFaint} />
          <TextInput
            style={styles.searchInput}
            value={searchQuery}
            onChangeText={setSearchQuery}
            placeholder="Malzeme, koli, kavanoz veya bölge ara..."
            placeholderTextColor={theme.colors.inkFaint}
            selectionColor={theme.colors.primary}
          />
          {searchQuery.length > 0 && (
            <TouchableOpacity onPress={() => setSearchQuery('')} activeOpacity={0.7} style={styles.searchClearBtn}>
              <X size={18} color={theme.colors.inkFaint} />
            </TouchableOpacity>
          )}
          <TouchableOpacity style={styles.filterBtn} activeOpacity={0.8}>
            <Filter size={16} color={theme.colors.primary} />
          </TouchableOpacity>
        </View>
      </View>

      {/* Category Pills Slider */}
      <View style={styles.categoryWrap}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.categoryScroll}
        >
          {CATEGORIES.map(cat => {
            const isSelected = selectedCat === cat.key;
            return (
              <TouchableOpacity
                key={cat.id}
                style={[styles.catChip, isSelected && styles.catChipActive]}
                onPress={() => setSelectedCat(cat.key)}
                activeOpacity={0.78}
              >
                <Text style={[styles.catChipText, isSelected && styles.catChipTextActive]}>
                  {cat.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      {/* Prominent High-Card for Talep Oluştur (Under Categories) */}
      <View style={styles.needCardSection}>
        <TouchableOpacity
          style={styles.needCardHero}
          onPress={() => navigation.navigate('PostAdFlow', { mode: 'need' })}
          activeOpacity={0.88}
        >
          <View style={styles.needCardLeft}>
            <View style={styles.needIconBadge}>
              <HandHeart size={24} color={theme.colors.primary} />
            </View>
            <View style={styles.needCardCopy}>
              <Text style={styles.needCardTitle}>Aradığını Bulamadın mı?</Text>
              <Text style={styles.needCardSub}>
                Bölgendeki komşularından malzeme istemek için hemen bir talep ilanı oluştur.
              </Text>
            </View>
          </View>
          <View style={styles.needCardCta}>
            <Text style={styles.needCardCtaText}>Talep Aç</Text>
            <ChevronRight size={16} color={theme.colors.primary} />
          </View>
        </TouchableOpacity>
      </View>

      {/* Main Feed Grid */}
      <FlatList
        data={filteredAds}
        keyExtractor={item => item.id}
        renderItem={renderFeedCard}
        numColumns={2}
        columnWrapperStyle={styles.feedColumnWrapper}
        contentContainerStyle={styles.feedListContent}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={isLoadingAds} onRefresh={refreshAds} tintColor={theme.colors.primary} />}
        ListHeaderComponent={
          adsError ? (
            <View style={styles.adsErrorBanner}>
              <Text style={styles.adsErrorText}>{adsError}</Text>
              <TouchableOpacity onPress={refreshAds} disabled={isLoadingAds}>
                {isLoadingAds ? <ActivityIndicator size="small" color={theme.colors.primary} /> : <Text style={styles.adsRetryText}>Tekrar dene</Text>}
              </TouchableOpacity>
            </View>
          ) : undefined
        }
        ListEmptyComponent={
          <View style={styles.emptyFeed}>
            <Package size={42} color={theme.colors.inkFaint} />
            <Text style={styles.emptyFeedTitle}>İlan Bulunamadı</Text>
            <Text style={styles.emptyFeedSub}>
              Aramanıza uygun ilan henüz eklenmedi. Hemen yeni bir ilan verebilirsiniz!
            </Text>
          </View>
        }
      />

      {/* Ad Detail Modal */}
      <Modal
        visible={selectedAdModal !== null}
        animationType="slide"
        transparent
        onRequestClose={() => setSelectedAdModal(null)}
      >
        {selectedAdModal && (
          <View style={styles.modalOverlay}>
            <View style={[styles.modalSheet, { paddingBottom: insets.bottom + 16 }]}>
              <View style={styles.modalHandle} />
              
              <View style={styles.modalHeader}>
                <Text style={styles.modalCategoryTitle}>
                  {CAT_LABELS[selectedAdModal.category] ?? selectedAdModal.category}
                </Text>
                <TouchableOpacity
                  onPress={() => setSelectedAdModal(null)}
                  style={styles.modalCloseBtn}
                  activeOpacity={0.7}
                >
                  <X size={20} color={theme.colors.inkSoft} />
                </TouchableOpacity>
              </View>

              <Image
                source={{ uri: selectedAdModal.mediaUrl }}
                style={styles.modalImage}
                resizeMode="cover"
              />

              <Text style={styles.modalTitle}>{selectedAdModal.title}</Text>
              
              <View style={styles.modalLocRow}>
                <MapPin size={15} color={theme.colors.primary} />
                <Text style={styles.modalLocText}>
                  {selectedAdModal.approximateLocation.regionName}
                </Text>
              </View>

              <Text style={styles.modalDesc}>{selectedAdModal.description}</Text>

              <View style={styles.modalOwnerInfo}>
                <View style={styles.modalAvatar}>
                  <Text style={styles.modalAvatarText}>
                    {selectedAdModal.ownerName.charAt(0)}
                  </Text>
                </View>
                <View>
                  <Text style={styles.modalOwnerName}>{selectedAdModal.ownerName}</Text>
                  <Text style={styles.modalOwnerLevel}>Seviye {selectedAdModal.ownerLevel} Bağışçı</Text>
                </View>
              </View>

              {selectedAdModal.ownerId !== user?.id ? (
                <TouchableOpacity
                  style={styles.requestCtaBtn}
                  onPress={() => handleSendInterest(selectedAdModal)}
                  activeOpacity={0.82}
                >
                  <HandHeart size={20} color="#241905" />
                  <Text style={styles.requestCtaText}>İlgi Bildir / İste</Text>
                </TouchableOpacity>
              ) : (
                <TouchableOpacity
                  style={styles.deleteAdBtn}
                  onPress={() => {
                    deleteAd(selectedAdModal.id).catch(() => undefined);
                    setSelectedAdModal(null);
                  }}
                  activeOpacity={0.8}
                >
                  <Text style={styles.deleteAdText}>Bu İlanı Kaldır</Text>
                </TouchableOpacity>
              )}
            </View>
          </View>
        )}
      </Modal>

      {/* Success Modal */}
      <Modal
        visible={requestSuccessModal}
        animationType="fade"
        transparent
        onRequestClose={() => setRequestSuccessModal(false)}
      >
        <View style={styles.modalOverlayCenter}>
          <View style={styles.successDialog}>
            <Sparkles size={36} color={theme.colors.primary} />
            <Text style={styles.successTitle}>Talep Gönderildi!</Text>
            <Text style={styles.successSub}>
              İlan sahibine ilgi talebin iletildi. 'İşlemler' sekmesinden takip edebilirsin.
            </Text>
            <TouchableOpacity
              style={styles.successOkBtn}
              onPress={() => setRequestSuccessModal(false)}
              activeOpacity={0.8}
            >
              <Text style={styles.successOkText}>Tamam</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </View>
  );
};

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------
const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: theme.colors.bg,
  },

  // Header
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  logoBadge: {
    width: 38,
    height: 38,
    borderRadius: 10,
    backgroundColor: 'transparent',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  headerLogoImage: {
    width: 38,
    height: 38,
    borderRadius: 10,
  },
  brandTitle: {
    fontFamily: 'Georgia',
    fontSize: 19,
    fontWeight: '600',
    color: theme.colors.ink,
    letterSpacing: -0.3,
  },
  brandSubtitle: {
    fontSize: 11,
    color: theme.colors.inkFaint,
    fontWeight: '500',
  },
  profileFab: {
    width: 38,
    height: 38,
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.lineStrong,
    alignItems: 'center',
    justifyContent: 'center',
  },

  // Search Section
  searchSection: {
    paddingHorizontal: 20,
    marginBottom: 10,
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.surface,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: theme.colors.line,
    paddingLeft: 14,
    paddingRight: 6,
    paddingVertical: 6,
    gap: 8,
  },
  searchInput: {
    flex: 1,
    fontSize: theme.font.size.sm,
    color: theme.colors.ink,
    padding: 0,
  },
  searchClearBtn: {
    marginRight: 6,
  },
  filterBtn: {
    width: 34,
    height: 34,
    borderRadius: 10,
    backgroundColor: theme.colors.primaryTint,
    alignItems: 'center',
    justifyContent: 'center',
  },

  // Categories
  categoryWrap: {
    marginBottom: 12,
  },
  categoryScroll: {
    paddingHorizontal: 20,
    gap: 8,
  },
  catChip: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.line,
  },
  catChipActive: {
    backgroundColor: theme.colors.primaryTint,
    borderColor: theme.colors.primary,
  },
  catChipText: {
    fontSize: 12,
    fontWeight: '600',
    color: theme.colors.inkSoft,
  },
  catChipTextActive: {
    color: theme.colors.primary,
    fontWeight: '700',
  },

  // High Card: Talep Oluştur
  needCardSection: {
    paddingHorizontal: 20,
    marginBottom: 14,
  },
  needCardHero: {
    backgroundColor: theme.colors.surface,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: theme.colors.lineStrong,
    padding: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  needCardLeft: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginRight: 8,
  },
  needIconBadge: {
    width: 44,
    height: 44,
    borderRadius: 14,
    backgroundColor: theme.colors.primaryTint,
    alignItems: 'center',
    justifyContent: 'center',
  },
  needCardCopy: {
    flex: 1,
  },
  needCardTitle: {
    fontSize: 13.5,
    fontWeight: '700',
    color: theme.colors.ink,
    marginBottom: 2,
  },
  needCardSub: {
    fontSize: 11,
    color: theme.colors.inkFaint,
    lineHeight: 15,
  },
  needCardCta: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.primaryTint,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: theme.radius.full,
    gap: 2,
  },
  needCardCtaText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: theme.colors.primary,
  },

  // Feed Grid
  feedListContent: {
    paddingHorizontal: 20,
    paddingBottom: 24,
  },
  feedColumnWrapper: {
    justifyContent: 'space-between',
    marginBottom: 14,
  },
  feedCard: {
    width: '48%',
    backgroundColor: theme.colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: theme.colors.line,
    overflow: 'hidden',
  },
  cardImageContainer: {
    width: '100%',
    height: 135,
    backgroundColor: theme.colors.surfaceAlt,
    position: 'relative',
  },
  cardImage: {
    width: '100%',
    height: '100%',
  },
  catBadge: {
    position: 'absolute',
    top: 8,
    left: 8,
    backgroundColor: 'rgba(22,36,27,0.75)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: theme.radius.full,
  },
  catBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: theme.colors.paper,
  },
  favBtn: {
    position: 'absolute',
    top: 8,
    right: 8,
    width: 30,
    height: 30,
    borderRadius: theme.radius.full,
    backgroundColor: 'rgba(255,255,255,0.85)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardBody: {
    padding: 10,
  },
  cardTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: theme.colors.ink,
    lineHeight: 17,
    marginBottom: 6,
    height: 34,
  },
  cardLocationRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginBottom: 8,
  },
  cardLocationText: {
    fontSize: 11,
    color: theme.colors.inkFaint,
  },
  cardFooterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: theme.colors.line,
    paddingTop: 6,
  },
  freeBadge: {
    fontSize: 10.5,
    fontWeight: '700',
    color: theme.colors.primary,
  },
  ownerText: {
    fontSize: 10.5,
    color: theme.colors.inkFaint,
  },

  emptyFeed: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 48,
    paddingHorizontal: 32,
  },
  adsErrorBanner: {
    marginBottom: 12,
    padding: 12,
    borderRadius: 12,
    backgroundColor: '#FFF4E5',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  adsErrorText: { flex: 1, color: '#8A4B08', marginRight: 10 },
  adsRetryText: { color: theme.colors.primary, fontWeight: '700' },
  emptyFeedTitle: {
    fontSize: theme.font.size.base,
    fontWeight: '700',
    color: theme.colors.ink,
    marginTop: 12,
  },
  emptyFeedSub: {
    fontSize: 12,
    color: theme.colors.inkFaint,
    textAlign: 'center',
    marginTop: 4,
    lineHeight: 18,
  },

  // Modal Sheet
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(22,36,27,0.5)',
    justifyContent: 'flex-end',
  },
  modalSheet: {
    backgroundColor: theme.colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingTop: 12,
    paddingHorizontal: 22,
    maxHeight: '88%',
  },
  modalHandle: {
    width: 36,
    height: 4,
    backgroundColor: theme.colors.lineStrong,
    borderRadius: theme.radius.full,
    alignSelf: 'center',
    marginBottom: 12,
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  modalCategoryTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: theme.colors.primary,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  modalCloseBtn: {
    width: 32,
    height: 32,
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.bg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalImage: {
    width: '100%',
    height: 180,
    borderRadius: 16,
    marginBottom: 14,
  },
  modalTitle: {
    fontFamily: 'Georgia',
    fontSize: 18,
    fontWeight: '600',
    color: theme.colors.ink,
    marginBottom: 6,
  },
  modalLocRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 12,
  },
  modalLocText: {
    fontSize: 12.5,
    fontWeight: '500',
    color: theme.colors.inkSoft,
  },
  modalDesc: {
    fontSize: 13,
    color: theme.colors.inkSoft,
    lineHeight: 20,
    marginBottom: 16,
  },
  modalOwnerInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: theme.colors.bg,
    padding: 10,
    borderRadius: 12,
    marginBottom: 16,
  },
  modalAvatar: {
    width: 34,
    height: 34,
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.primaryTint,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalAvatarText: {
    fontSize: 14,
    fontWeight: '700',
    color: theme.colors.primary,
  },
  modalOwnerName: {
    fontSize: 13,
    fontWeight: '600',
    color: theme.colors.ink,
  },
  modalOwnerLevel: {
    fontSize: 11,
    color: theme.colors.inkFaint,
  },
  requestCtaBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: theme.colors.accent,
    paddingVertical: 14,
    borderRadius: 14,
  },
  requestCtaText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#241905',
  },
  deleteAdBtn: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(198,40,40,0.08)',
    paddingVertical: 14,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: 'rgba(198,40,40,0.2)',
  },
  deleteAdText: {
    fontSize: 14,
    fontWeight: '700',
    color: theme.colors.danger,
  },

  // Success Dialog
  modalOverlayCenter: {
    flex: 1,
    backgroundColor: 'rgba(22,36,27,0.5)',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  successDialog: {
    width: '100%',
    backgroundColor: theme.colors.surface,
    borderRadius: 24,
    padding: 24,
    alignItems: 'center',
  },
  successTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: theme.colors.ink,
    marginTop: 12,
    marginBottom: 6,
  },
  successSub: {
    fontSize: 13,
    color: theme.colors.inkSoft,
    textAlign: 'center',
    lineHeight: 19,
    marginBottom: 20,
  },
  successOkBtn: {
    width: '100%',
    backgroundColor: theme.colors.primary,
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  successOkText: {
    fontSize: 14,
    fontWeight: '700',
    color: theme.colors.paper,
  },
});
