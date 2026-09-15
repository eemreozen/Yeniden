import React, { useMemo } from 'react';
import {
  ActivityIndicator,
  Image,
  Linking,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { WebView } from 'react-native-webview';
import {
  CalendarDays,
  Check,
  ChevronLeft,
  MapPin,
  Navigation,
} from 'lucide-react-native';
import { useAppState } from '../../context/AppStateContext';
import { theme } from '../../theme/theme';

const CATEGORY_LABELS: Record<string, string> = {
  KOLI: 'Koli', KARTON: 'Karton', CAM: 'Cam', AHSAP: 'Ahşap', HOBI: 'Hobi', DIGER: 'Diğer',
};

interface Props {
  transactionId: string;
  onBack: () => void;
}

export const AgreementDetailScreen: React.FC<Props> = ({ transactionId, onBack }) => {
  const insets = useSafeAreaInsets();
  const {
    user,
    ads,
    transactions,
    transactionsLoading,
    transactionsError,
    refreshTransactions,
    getHandoverCode,
    confirmHandover,
  } = useAppState();
  const [confirmationCode, setConfirmationCode] = React.useState('');
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const [actionError, setActionError] = React.useState<string | null>(null);
  const transaction = transactions.find(item => item.id === transactionId);
  const ad = ads.find(item => item.id === transaction?.adId);

  const latitude = ad?.approximateLocation.latitude ?? 39.9208;
  const longitude = ad?.approximateLocation.longitude ?? 32.8541;
  const region = ad?.approximateLocation.regionName ?? 'Çankaya, Ankara';
  const radius = ad?.approximateLocation.radiusInMeters ?? 450;

  const mapHtml = useMemo(() => `
    <!doctype html><html><head>
      <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no" />
      <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
      <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
      <style>html,body,#map{width:100%;height:100%;margin:0;background:#e7ece0}.leaflet-control-attribution{display:none}.pin{background:#1e3a2b;color:white;border:2px solid white;border-radius:18px;padding:6px 9px;font:700 11px sans-serif;white-space:nowrap}</style>
    </head><body><div id="map"></div><script>
      const map=L.map('map',{zoomControl:false,dragging:false,doubleClickZoom:false,scrollWheelZoom:false}).setView([${latitude},${longitude}],14);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19}).addTo(map);
      const icon=L.divIcon({className:'',html:'<div class="pin">${region.replace(/'/g, "\\'")}</div>',iconSize:[116,34],iconAnchor:[58,34]});
      L.marker([${latitude},${longitude}],{icon}).addTo(map);
    </script></body></html>
  `, [latitude, longitude, region]);

  if (!transaction) {
    return (
      <View style={[styles.root, { paddingTop: insets.top }]}>
        <View style={styles.header}>
          <TouchableOpacity style={styles.backButton} onPress={onBack}><ChevronLeft size={24} color={theme.colors.ink} /></TouchableOpacity>
          <Text style={styles.headerTitle}>Anlaşma Detayı</Text>
          <View style={styles.headerSpacer} />
        </View>
        <View style={styles.missingState}>
          {transactionsLoading ? <ActivityIndicator color={theme.colors.primary} /> : <Text style={styles.missingText}>{transactionsError ?? 'Anlaşma bilgisi bulunamadı.'}</Text>}
          {!transactionsLoading && !!transactionsError && (
            <TouchableOpacity style={styles.retryButton} onPress={refreshTransactions}><Text style={styles.retryText}>Tekrar Dene</Text></TouchableOpacity>
          )}
        </View>
      </View>
    );
  }

  const isDonor = transaction.donorId === user?.id;
  const isDelivered = transaction.status === 'DELIVERED';
  const imageUrl = ad?.mediaUrl ?? 'https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=900';
  const category = CATEGORY_LABELS[transaction.category] ?? transaction.category;

  const openLocation = () => Linking.openURL(`https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`);
  const handleGetCode = async () => {
    setIsSubmitting(true);
    setActionError(null);
    try {
      const code = await getHandoverCode(transaction.id);
      setConfirmationCode(code);
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Teslimat kodu alınamadı.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirm = async () => {
    if (!/^\d{6}$/.test(confirmationCode)) {
      setActionError('Alıcının gösterdiği 6 haneli teslimat kodunu girin.');
      return;
    }
    setIsSubmitting(true);
    setActionError(null);
    try {
      await confirmHandover(transaction.id, confirmationCode);
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Teslimat doğrulanamadı.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <StatusBar barStyle="dark-content" backgroundColor={theme.colors.bg} />
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={onBack} activeOpacity={0.75} accessibilityLabel="Geri dön">
          <ChevronLeft size={24} color={theme.colors.ink} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Anlaşma Detayı</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.successCard}>
          <View style={styles.successIcon}><Check size={27} color={theme.colors.paper} strokeWidth={2.6} /></View>
          <View style={styles.successCopy}>
            <Text style={styles.successTitle}>{isDelivered ? 'Teslimat Tamamlandı' : 'Anlaşma Yapıldı'}</Text>
            <Text style={styles.successLead}>{isDelivered ? 'Değişim başarıyla tamamlandı.' : 'Teslimat kodu oluşturuldu.'}</Text>
            <Text style={styles.successBody}>Bu eşyanın yeniden kullanıma kazandırılmasına katkı sağladığınız için teşekkürler!</Text>
          </View>
          <Text style={styles.leafDecoration}>❧</Text>
        </View>

        <View style={styles.itemCard}>
          <View style={styles.visualRow}>
            <View style={styles.imageWrap}>
              <Image source={{ uri: imageUrl }} style={styles.itemImage} resizeMode="cover" />
              <View style={styles.categoryOverlay}><Text style={styles.categoryOverlayText}>{category}</Text></View>
            </View>
            <View style={styles.mapWrap}>
              <WebView source={{ html: mapHtml }} style={styles.map} scrollEnabled={false} pointerEvents="none" />
            </View>
          </View>
          <Text style={styles.itemTitle}>{transaction.adTitle}</Text>
          <View style={styles.badgeRow}>
            <View style={styles.categoryBadge}><Text style={styles.categoryBadgeText}>{category}</Text></View>
            <View style={styles.freeBadge}><Text style={styles.freeBadgeText}>Ücretsiz</Text></View>
          </View>
          <View style={styles.locationRow}><MapPin size={18} color={theme.colors.primary} /><Text style={styles.locationText}>{region} (~{radius}m çapında)</Text></View>
          <View style={styles.divider} />
          <Text style={styles.description}>{ad?.description ?? 'Yeniden kullanıma uygun, temiz durumdaki malzeme için teslimat planlandı.'}</Text>
        </View>

        <View style={styles.deliveryCard}>
          <Text style={styles.sectionTitle}>Taraflar ve Teslimat Bilgisi</Text>
          <View style={styles.peopleRow}>
            <View style={styles.personBlock}>
              <View style={styles.personTop}>
                <View style={styles.avatar}><Text style={styles.avatarText}>{transaction.donorName.charAt(0)}</Text></View>
                <View style={styles.personCopy}><Text style={styles.personRole}>Bağışçı</Text><Text style={styles.personName}>{transaction.donorName}</Text><Text style={styles.personLevel}>Seviye {ad?.ownerLevel ?? 4} Bağışçı</Text></View>
              </View>
            </View>
            <View style={styles.verticalDivider} />
            <View style={styles.personBlock}>
              <View style={styles.personTop}>
                <View style={[styles.avatar, styles.receiverAvatar]}><Text style={styles.avatarText}>{transaction.receiverName.charAt(0)}</Text></View>
                <View style={styles.personCopy}><Text style={styles.personRole}>Alıcı</Text><Text style={styles.personName}>{transaction.receiverName}</Text><Text style={styles.personLevel}>Seviye {user?.level ?? 4} Kullanıcı</Text></View>
              </View>
            </View>
          </View>

          <View style={styles.divider} />
          <View style={styles.planRow}>
            <View style={styles.planBlock}><CalendarDays size={28} color={theme.colors.primary} /><View><Text style={styles.planLabel}>Kod Geçerlilik Süresi</Text><Text style={styles.planValue}>{transaction.expiresAt ? new Date(transaction.expiresAt).toLocaleDateString('tr-TR') : '72 saat'}</Text><Text style={styles.planSub}>{transaction.expiresAt ? new Date(transaction.expiresAt).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' }) : 'Kabulden itibaren'}</Text></View></View>
            <View style={styles.planBlock}><MapPin size={28} color={theme.colors.primary} /><View style={styles.planCopy}><Text style={styles.planLabel}>Teslimat Konumu</Text><Text style={styles.planValue}>{region}</Text><Text style={styles.planSub}>~{radius}m çapında</Text></View></View>
          </View>

          {!isDelivered && (
            <View style={styles.codePanel}>
              <Text style={styles.codeTitle}>{isDonor ? 'Teslimatı doğrula' : 'Teslimat kodunuz'}</Text>
              <Text style={styles.codeHelp}>{isDonor ? 'Alıcının uygulamasında görünen 6 haneli kodu girin.' : 'Teslim sırasında bu kodu bağışçıya gösterin.'}</Text>
              {isDonor ? (
                <TextInput
                  value={confirmationCode}
                  onChangeText={value => setConfirmationCode(value.replace(/\D/g, '').slice(0, 6))}
                  style={styles.codeInput}
                  keyboardType="number-pad"
                  maxLength={6}
                  placeholder="000000"
                  placeholderTextColor={theme.colors.inkFaint}
                  textContentType="oneTimeCode"
                />
              ) : confirmationCode ? (
                <Text style={styles.codeValue}>{confirmationCode}</Text>
              ) : null}
              {!!actionError && <Text style={styles.actionError}>{actionError}</Text>}
              <TouchableOpacity
                style={[styles.confirmButton, isSubmitting && styles.disabledButton]}
                onPress={isDonor ? handleConfirm : handleGetCode}
                activeOpacity={0.82}
                disabled={isSubmitting}
              >
                {isSubmitting ? <ActivityIndicator color={theme.colors.paper} /> : <Check size={22} color={theme.colors.paper} />}
                <Text style={styles.confirmText}>{isDonor ? 'Teslimatı Onayla' : confirmationCode ? 'Kodu Yenile' : 'Kodu Göster'}</Text>
              </TouchableOpacity>
            </View>
          )}

          <View style={styles.actionRow}>
            <TouchableOpacity style={styles.locationButton} onPress={openLocation} activeOpacity={0.82}><Navigation size={21} color={theme.colors.primary} /><Text style={styles.locationButtonText}>Konumu Aç</Text></TouchableOpacity>
          </View>
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: theme.colors.bg },
  header: { height: 58, paddingHorizontal: 18, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  backButton: { width: 48, height: 48, borderRadius: 14, backgroundColor: theme.colors.surface, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: theme.colors.lineStrong },
  headerTitle: { fontSize: theme.font.size.xl, fontWeight: '700', color: theme.colors.ink },
  headerSpacer: { width: 48 },
  content: { padding: 16, paddingTop: 8, gap: 14, paddingBottom: 24 },
  successCard: { minHeight: 104, borderRadius: theme.radius.lg, backgroundColor: theme.colors.surface, padding: 14, flexDirection: 'row', alignItems: 'center', overflow: 'hidden', ...theme.shadow.sm },
  successIcon: { width: 54, height: 54, borderRadius: 27, backgroundColor: theme.colors.primary, alignItems: 'center', justifyContent: 'center' },
  successCopy: { flex: 1, marginLeft: 12, paddingRight: 12 },
  successTitle: { fontSize: theme.font.size.lg, fontWeight: '800', color: theme.colors.primary },
  successLead: { marginTop: 1, fontSize: theme.font.size.sm, color: theme.colors.inkSoft },
  successBody: { marginTop: 4, fontSize: theme.font.size.xs, lineHeight: 15, color: theme.colors.inkFaint },
  leafDecoration: { position: 'absolute', right: 8, top: 22, fontSize: 48, color: theme.colors.primaryTint, opacity: 0.55 },
  itemCard: { borderRadius: theme.radius.xl, backgroundColor: theme.colors.surface, padding: 12, ...theme.shadow.sm },
  visualRow: { flexDirection: 'row', gap: 10 },
  imageWrap: { flex: 1, height: 174, borderRadius: theme.radius.md, overflow: 'hidden' },
  itemImage: { width: '100%', height: '100%' },
  categoryOverlay: { position: 'absolute', top: 10, left: 10, backgroundColor: 'rgba(248,250,246,0.92)', borderRadius: theme.radius.full, paddingHorizontal: 12, paddingVertical: 6 },
  categoryOverlayText: { color: theme.colors.primary, fontSize: theme.font.size.xs, fontWeight: '700' },
  mapWrap: { flex: 1, height: 174, borderRadius: theme.radius.md, overflow: 'hidden' },
  map: { flex: 1, backgroundColor: theme.colors.surfaceAlt },
  itemTitle: { marginTop: 14, fontSize: theme.font.size.lg, lineHeight: 23, fontWeight: '800', color: theme.colors.ink },
  badgeRow: { flexDirection: 'row', gap: 8, marginTop: 10 },
  categoryBadge: { borderRadius: theme.radius.full, backgroundColor: theme.colors.primaryTint, paddingHorizontal: 14, paddingVertical: 6 },
  categoryBadgeText: { color: theme.colors.primary, fontWeight: '700', fontSize: theme.font.size.xs },
  freeBadge: { borderRadius: theme.radius.full, backgroundColor: theme.colors.accentTint, paddingHorizontal: 14, paddingVertical: 6 },
  freeBadgeText: { color: '#7A5A16', fontWeight: '700', fontSize: theme.font.size.xs },
  locationRow: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 12 },
  locationText: { flex: 1, color: theme.colors.inkSoft, fontSize: theme.font.size.sm },
  divider: { height: 1, backgroundColor: theme.colors.line, marginVertical: 13 },
  description: { color: theme.colors.inkSoft, fontSize: theme.font.size.sm, lineHeight: 20 },
  deliveryCard: { borderRadius: theme.radius.xl, backgroundColor: theme.colors.surface, padding: 16, ...theme.shadow.sm },
  sectionTitle: { color: theme.colors.ink, fontSize: theme.font.size.md, fontWeight: '800', marginBottom: 16 },
  peopleRow: { flexDirection: 'row', alignItems: 'stretch' },
  personBlock: { flex: 1 },
  personTop: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  personCopy: { flex: 1 },
  avatar: { width: 48, height: 48, borderRadius: 24, backgroundColor: theme.colors.primary, alignItems: 'center', justifyContent: 'center' },
  receiverAvatar: { backgroundColor: theme.colors.primarySoft },
  avatarText: { color: theme.colors.paper, fontSize: theme.font.size.xl, fontWeight: '700' },
  personRole: { color: theme.colors.inkFaint, fontSize: theme.font.size.xs },
  personName: { color: theme.colors.ink, fontSize: theme.font.size.base, fontWeight: '800' },
  personLevel: { color: theme.colors.inkFaint, fontSize: 10 },
  verticalDivider: { width: 1, backgroundColor: theme.colors.line, marginHorizontal: 10 },
  planRow: { flexDirection: 'row', gap: 14 },
  planBlock: { flex: 1, flexDirection: 'row', alignItems: 'flex-start', gap: 9 },
  planCopy: { flex: 1 },
  planLabel: { color: theme.colors.inkFaint, fontSize: theme.font.size.xs },
  planValue: { color: theme.colors.ink, fontSize: theme.font.size.sm, fontWeight: '700', marginTop: 2 },
  planSub: { color: theme.colors.inkFaint, fontSize: theme.font.size.xs, marginTop: 2 },
  codePanel: { marginTop: 18, padding: 14, borderRadius: theme.radius.lg, backgroundColor: theme.colors.surfaceAlt },
  codeTitle: { color: theme.colors.ink, fontSize: theme.font.size.base, fontWeight: '800' },
  codeHelp: { color: theme.colors.inkSoft, fontSize: theme.font.size.xs, lineHeight: 18, marginTop: 4 },
  codeInput: { height: 54, borderRadius: theme.radius.md, borderWidth: 1, borderColor: theme.colors.lineStrong, backgroundColor: theme.colors.surface, color: theme.colors.ink, fontSize: 26, fontWeight: '800', letterSpacing: 8, textAlign: 'center', marginTop: 12 },
  codeValue: { color: theme.colors.primary, fontSize: 32, fontWeight: '900', letterSpacing: 10, textAlign: 'center', marginVertical: 14 },
  actionError: { color: '#B42318', fontSize: theme.font.size.xs, marginTop: 9 },
  disabledButton: { opacity: 0.55 },
  actionRow: { flexDirection: 'row', gap: 10, marginTop: 10 },
  confirmButton: { minHeight: 52, marginTop: 12, borderRadius: theme.radius.lg, backgroundColor: theme.colors.primary, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8 },
  confirmText: { color: theme.colors.paper, fontSize: theme.font.size.sm, fontWeight: '800' },
  locationButton: { flex: 1, minHeight: 52, borderRadius: theme.radius.lg, backgroundColor: theme.colors.primaryTint, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8 },
  locationButtonText: { color: theme.colors.primary, fontSize: theme.font.size.sm, fontWeight: '800' },
  missingState: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 32, gap: 12 },
  missingText: { textAlign: 'center', color: theme.colors.inkSoft },
  retryButton: { minHeight: 42, borderRadius: theme.radius.md, backgroundColor: theme.colors.primary, justifyContent: 'center', paddingHorizontal: 18 },
  retryText: { color: theme.colors.paper, fontSize: theme.font.size.xs, fontWeight: '800' },
});
