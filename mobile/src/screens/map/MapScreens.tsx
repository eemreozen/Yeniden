import React, { useState, useMemo } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  TextInput,
  StatusBar,
  ScrollView,
  Dimensions,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { WebView } from 'react-native-webview';
import { Search, MapPin, X, Package, HandHeart, CheckCircle2 } from 'lucide-react-native';
import { theme } from '../../theme/theme';
import { useAppState, Ad } from '../../context/AppStateContext';
import { useTab } from '../../navigation/AppNavigator';

const { width } = Dimensions.get('window');

const CHIPS = ['Tümü', 'Koli', 'Cam', 'Karton', 'Ahşap', 'Hobi', 'Diğer'];

const CAT_LABELS: Record<string, string> = {
  KOLI: 'Koli', KARTON: 'Karton', CAM: 'Cam',
  AHSAP: 'Ahşap', HOBI: 'Hobi', DIGER: 'Diğer',
};

export const MapScreen: React.FC<{ navigation: any }> = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { ads, sendInterest, isLoadingAds, adsError, refreshAds } = useAppState();
  const { openAgreement } = useTab();

  const [activeChip, setActiveChip] = useState('Tümü');
  const [search, setSearch] = useState('');
  const [selectedAd, setSelectedAd] = useState<Ad | null>(null);
  const [requestedSuccess, setRequestedSuccess] = useState(false);

  // Filter ads based on category & search text
  const activeAds = ads.filter(a => a.status === 'ACTIVE');
  const filteredAds = activeAds.filter(ad => {
    const catLabel = CAT_LABELS[ad.category] ?? ad.category;
    const matchesChip = activeChip === 'Tümü' || catLabel === activeChip;
    const matchesSearch =
      ad.title.toLowerCase().includes(search.toLowerCase()) ||
      ad.approximateLocation.regionName.toLowerCase().includes(search.toLowerCase());
    return matchesChip && matchesSearch;
  });

  const handleInterest = async (ad: Ad) => {
    try {
      const transaction = await sendInterest(ad);
      setSelectedAd(null);
      if (transaction?.status === 'AGREED') { openAgreement(transaction.id); return; }
      setRequestedSuccess(true);
      setTimeout(() => setRequestedSuccess(false), 2500);
    } catch { setSelectedAd(null); }
  };

  // Generate Leaflet OpenStreetMap HTML content with interactive markers
  const leafletHtml = useMemo(() => {
    const markersData = filteredAds.map(ad => ({
      id: ad.id,
      title: ad.title.replace(/'/g, "\\'"),
      category: CAT_LABELS[ad.category] ?? ad.category,
      region: ad.approximateLocation.regionName.replace(/'/g, "\\'"),
      lat: ad.approximateLocation.latitude,
      lng: ad.approximateLocation.longitude,
    }));

    return `
      <!DOCTYPE html>
      <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
          html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #EFF3EA; }
          .custom-pin {
            background: #1E3A2B;
            color: #F8FAF6;
            padding: 5px 9px;
            border-radius: 14px;
            font-size: 11px;
            font-weight: 700;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            white-space: nowrap;
            box-shadow: 0 4px 12px rgba(22,36,27,0.3);
            border: 1.5px solid #F8FAF6;
            display: flex;
            align-items: center;
            gap: 4px;
          }
          .custom-pin::after {
            content: '';
            position: absolute;
            bottom: -6px;
            left: 50%;
            transform: translateX(-50%);
            border-width: 6px 5px 0 5px;
            border-style: solid;
            border-color: #1E3A2B transparent transparent transparent;
          }
          .leaflet-control-attribution { display: none !important; }
        </style>
      </head>
      <body>
        <div id="map"></div>
        <script>
          var map = L.map('map', { zoomControl: false }).setView([39.9334, 32.8597], 11);
          
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
          }).addTo(map);

          var markers = ${JSON.stringify(markersData)};

          markers.forEach(function(m) {
            var icon = L.divIcon({
              className: 'custom-div-icon',
              html: '<div class="custom-pin">📦 ' + m.category + '</div>',
              iconSize: [80, 24],
              iconAnchor: [40, 30]
            });

            var marker = L.marker([m.lat, m.lng], { icon: icon }).addTo(map);
            marker.on('click', function() {
              window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'MARKER_CLICK', id: m.id }));
            });
          });
        </script>
      </body>
      </html>
    `;
  }, [filteredAds]);

  const handleWebViewMessage = (event: any) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);
      if (data.type === 'MARKER_CLICK') {
        const found = ads.find(a => a.id === data.id);
        if (found) setSelectedAd(found);
      }
    } catch (e) {}
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <StatusBar barStyle="dark-content" backgroundColor={theme.colors.bg} />

      {/* Floating Header & Search */}
      <View style={styles.topControlContainer}>
        {adsError && (
          <View style={styles.adsErrorBanner}>
            {isLoadingAds && <ActivityIndicator size="small" color={theme.colors.primary} />}
            <Text style={styles.adsErrorText}>{adsError}</Text>
            <TouchableOpacity onPress={refreshAds} disabled={isLoadingAds}>
              <Text style={styles.adsRetryText}>Tekrar dene</Text>
            </TouchableOpacity>
          </View>
        )}
        <View style={styles.searchBar}>
          <Search size={18} color={theme.colors.inkFaint} />
          <TextInput
            style={styles.searchInput}
            value={search}
            onChangeText={setSearch}
            placeholder="Çankaya, Kızılay veya malzeme ara..."
            placeholderTextColor={theme.colors.inkFaint}
            selectionColor={theme.colors.primary}
            returnKeyType="search"
          />
          {search.length > 0 && (
            <TouchableOpacity onPress={() => setSearch('')} activeOpacity={0.7}>
              <X size={18} color={theme.colors.inkFaint} />
            </TouchableOpacity>
          )}
        </View>

        {/* Category Horizontal Filter Pills */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          style={styles.chipScroll}
          contentContainerStyle={styles.chipScrollContent}
        >
          {CHIPS.map(chip => (
            <TouchableOpacity
              key={chip}
              style={[styles.chip, activeChip === chip && styles.chipActive]}
              onPress={() => setActiveChip(chip)}
              activeOpacity={0.8}
            >
              <Text style={[styles.chipText, activeChip === chip && styles.chipTextActive]}>
                {chip}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* Interactive OpenStreetMap via Leaflet & WebView */}
      <View style={styles.mapContainer}>
        <WebView
          originWhitelist={['*']}
          source={{ html: leafletHtml }}
          style={styles.map}
          onMessage={handleWebViewMessage}
          javaScriptEnabled
          domStorageEnabled
        />

        {/* Toast Alert when Interest Request Sent */}
        {requestedSuccess && (
          <View style={styles.toastNotice}>
            <CheckCircle2 size={18} color={theme.colors.primary} />
            <Text style={styles.toastText}>İlgi talebiniz gönderildi!</Text>
          </View>
        )}

        {/* Selected Ad Bottom Sheet Card */}
        {selectedAd && (
          <View style={[styles.sheetCard, { paddingBottom: insets.bottom + 12 }]}>
            <TouchableOpacity
              style={styles.sheetCloseBtn}
              onPress={() => setSelectedAd(null)}
              activeOpacity={0.7}
            >
              <X size={18} color={theme.colors.inkSoft} />
            </TouchableOpacity>

            <View style={styles.sheetHandle} />

            <View style={styles.sheetHeaderRow}>
              <View style={styles.sheetCategoryTag}>
                <Text style={styles.sheetCategoryText}>
                  {CAT_LABELS[selectedAd.category] ?? selectedAd.category}
                </Text>
              </View>
              <View style={styles.sheetFreeTag}>
                <Text style={styles.sheetFreeText}>Ücretsiz</Text>
              </View>
            </View>

            <Text style={styles.sheetTitle}>{selectedAd.title}</Text>
            
            <View style={styles.sheetLocRow}>
              <MapPin size={14} color={theme.colors.primary} />
              <Text style={styles.sheetLocText}>
                {selectedAd.approximateLocation.regionName} (~{selectedAd.approximateLocation.radiusInMeters}m çapında)
              </Text>
            </View>

            <Text style={styles.sheetDesc} numberOfLines={2}>
              {selectedAd.description}
            </Text>

            <View style={styles.sheetFooterRow}>
              <View style={styles.sheetOwnerInfo}>
                <Text style={styles.sheetOwnerName}>{selectedAd.ownerName}</Text>
                <Text style={styles.sheetOwnerLevel}>Seviye {selectedAd.ownerLevel} Bağışçı</Text>
              </View>

              <TouchableOpacity
                style={styles.sheetCtaBtn}
                onPress={() => handleInterest(selectedAd)}
                activeOpacity={0.82}
              >
                <HandHeart size={16} color="#241905" />
                <Text style={styles.sheetCtaText}>İlgi Bildir</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      </View>
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

  // Search bar
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginHorizontal: 18,
    marginTop: 6,
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.lineStrong,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 12,
    ...theme.shadow.sm,
  },
  searchIcon: {
    fontSize: 14,
    color: theme.colors.inkFaint,
  },
  searchInput: {
    flex: 1,
    fontSize: theme.font.size.base,
    color: theme.colors.ink,
    padding: 0,
  },
  searchClear: {
    fontSize: 20,
    color: theme.colors.inkFaint,
    lineHeight: 22,
  },

  // Chips
  chipScroll: {},
  chipScrollContent: {
    paddingHorizontal: 18,
    paddingVertical: 10,
    gap: 7,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.lineStrong,
  },
  chipActive: {
    backgroundColor: theme.colors.primary,
    borderColor: theme.colors.primary,
  },
  chipText: {
    fontSize: 11.5,
    fontWeight: '600',
    color: theme.colors.inkSoft,
  },
  chipTextActive: {
    color: theme.colors.paper,
  },

  // Map Container
  topControlContainer: {
    paddingBottom: 4,
  },
  adsErrorBanner: {
    marginHorizontal: 16,
    marginBottom: 8,
    padding: 10,
    borderRadius: 12,
    backgroundColor: '#FFF4E5',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  adsErrorText: { flex: 1, color: '#8A4B08' },
  adsRetryText: { color: theme.colors.primary, fontWeight: '700' },
  mapContainer: {
    flex: 1,
    position: 'relative',
    overflow: 'hidden',
  },
  map: {
    width: '100%',
    height: '100%',
  },

  // Markers
  markerContainer: {
    alignItems: 'center',
  },
  markerBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: theme.colors.primary,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: theme.radius.full,
    borderWidth: 1.5,
    borderColor: theme.colors.paper,
    ...theme.shadow.sm,
  },
  markerText: {
    fontSize: 11,
    fontWeight: '700',
    color: theme.colors.paper,
  },
  markerArrow: {
    width: 0,
    height: 0,
    borderLeftWidth: 5,
    borderRightWidth: 5,
    borderTopWidth: 6,
    borderLeftColor: 'transparent',
    borderRightColor: 'transparent',
    borderTopColor: theme.colors.primary,
    marginTop: -1,
  },

  // Sheet Card
  sheetCard: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: theme.colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingTop: 12,
    paddingHorizontal: 20,
    ...theme.shadow.md,
  },
  sheetHandle: {
    width: 34,
    height: 4,
    backgroundColor: theme.colors.lineStrong,
    borderRadius: theme.radius.full,
    alignSelf: 'center',
    marginBottom: 12,
  },
  sheetCloseBtn: {
    position: 'absolute',
    top: 14,
    right: 18,
    width: 30,
    height: 30,
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.bg,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 10,
  },
  sheetHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
  },
  sheetCategoryTag: {
    backgroundColor: theme.colors.primaryTint,
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: theme.radius.full,
  },
  sheetCategoryText: {
    fontSize: 11,
    fontWeight: '700',
    color: theme.colors.primary,
  },
  sheetFreeTag: {
    backgroundColor: '#F1E3C4',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: theme.radius.full,
  },
  sheetFreeText: {
    fontSize: 10.5,
    fontWeight: '700',
    color: '#7A5A16',
  },
  sheetTitle: {
    fontFamily: 'Georgia',
    fontSize: 17,
    fontWeight: '600',
    color: theme.colors.ink,
    marginBottom: 6,
  },
  sheetLocRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    marginBottom: 8,
  },
  sheetLocText: {
    fontSize: 12,
    color: theme.colors.inkSoft,
    fontWeight: '500',
  },
  sheetDesc: {
    fontSize: 12.5,
    color: theme.colors.inkSoft,
    lineHeight: 18,
    marginBottom: 14,
  },
  sheetFooterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: theme.colors.line,
    paddingTop: 12,
  },
  sheetOwnerInfo: {},
  sheetOwnerName: {
    fontSize: 13,
    fontWeight: '600',
    color: theme.colors.ink,
  },
  sheetOwnerLevel: {
    fontSize: 11,
    color: theme.colors.inkFaint,
  },
  sheetCtaBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: theme.colors.accent,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 12,
  },
  sheetCtaText: {
    fontSize: 13,
    fontWeight: '700',
    color: '#241905',
  },

  // Toast
  toastNotice: {
    position: 'absolute',
    top: 16,
    alignSelf: 'center',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: theme.colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: theme.radius.full,
    borderWidth: 1,
    borderColor: theme.colors.lineStrong,
    ...theme.shadow.sm,
  },
  toastText: {
    fontSize: 12.5,
    fontWeight: '600',
    color: theme.colors.ink,
  },
});
