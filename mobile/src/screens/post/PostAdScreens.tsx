import React, { useRef, useState } from 'react';
import {
  Alert,
  Image,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  StatusBar,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { Camera as CameraIcon, RefreshCw, RotateCcw, SwitchCamera, X, Check, Package, Sparkles } from 'lucide-react-native';
import { theme } from '../../theme/theme';
import { useAppState, MaterialCategory } from '../../context/AppStateContext';
import { AiScanScreen, MOCK_CUP_IMAGE } from './AiScanScreen';

const CATEGORIES: { value: MaterialCategory; label: string }[] = [
  { value: 'KOLI', label: 'Koli' },
  { value: 'KARTON', label: 'Karton' },
  { value: 'CAM', label: 'Cam' },
  { value: 'AHSAP', label: 'Ahşap' },
  { value: 'HOBI', label: 'Hobi' },
  { value: 'DIGER', label: 'Diğer' },
];

// ---------------------------------------------------------------------------
// Give Ad Flow
// ---------------------------------------------------------------------------
type GiveStep = 'camera' | 'scan' | 'form' | 'success';

const GiveAdFlow: React.FC<{ onClose: () => void }> = ({ onClose }) => {
  const { postAd, ads, user } = useAppState();
  const [permission, requestPermission] = useCameraPermissions();
  const cameraRef = useRef<CameraView | null>(null);
  const [step, setStep] = useState<GiveStep>('camera');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<MaterialCategory>('KOLI');
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  const [isCapturing, setIsCapturing] = useState(false);
  const [cameraFacing, setCameraFacing] = useState<'back' | 'front'>('back');
  const [capturedUri, setCapturedUri] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isAiScanned, setIsAiScanned] = useState(false);

  // My active ads for bottom section
  const myAds = ads.filter(a => a.ownerId === user?.id || a.ownerName === 'Doğa Dostu');

  const openCamera = async () => {
    let granted = permission?.granted ?? false;
    if (!granted) {
      const result = await requestPermission();
      granted = result.granted;
    }
    if (!granted) {
      Alert.alert('Kamera izni gerekli', 'İlan fotoğrafı çekebilmek için kamera iznine izin vermelisiniz.');
      return;
    }
    setCapturedUri(null);
    setIsCameraOpen(true);
  };

  const handleCapture = async () => {
    if (!cameraRef.current || isCapturing) return;
    setIsCapturing(true);
    try {
      const photo = await cameraRef.current.takePictureAsync({ quality: 0.82, skipProcessing: false });
      if (photo?.uri) setCapturedUri(photo.uri);
    } catch {
      Alert.alert('Fotoğraf çekilemedi', 'Kamerayı tekrar açıp yeniden deneyin.');
    } finally {
      setIsCapturing(false);
    }
  };

  const useCapturedPhoto = () => {
    if (!capturedUri) return;
    setIsCameraOpen(false);
    setStep('scan');
  };

  const handleSubmit = async () => {
    if (!title.trim()) return;
    setIsSubmitting(true);
    try {
      await postAd(title, category, description, capturedUri ?? '');
      setIsSubmitting(false);
      setStep('success');
    } catch {
      setIsSubmitting(false);
      Alert.alert('İlan oluşturulamadı', 'Sunucuya bağlanırken bir hata oluştu. Lütfen tekrar deneyin.');
    }
  };

  // ── Photo upload & details step (clean alternate flow) ──
  if (step === 'camera' && isCameraOpen) {
    if (capturedUri) {
      return (
        <View style={styles.cameraStage}>
          <Image source={{ uri: capturedUri }} style={styles.cameraPreview} resizeMode="cover" />
          <View style={styles.cameraTopBar}>
            <TouchableOpacity
              style={styles.cameraIconButton}
              onPress={() => { setCapturedUri(null); setIsCameraOpen(false); }}
              activeOpacity={0.75}
              accessibilityLabel="Kamerayı kapat"
            >
              <X size={24} color={theme.colors.paper} />
            </TouchableOpacity>
            <View style={styles.previewBadge}><Text style={styles.previewBadgeText}>Fotoğraf önizleme</Text></View>
          </View>
          <View style={styles.previewActions}>
            <TouchableOpacity style={styles.retakeButton} onPress={() => setCapturedUri(null)} activeOpacity={0.8}>
              <RotateCcw size={20} color={theme.colors.paper} />
              <Text style={styles.retakeText}>Yeniden Çek</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.usePhotoButton} onPress={useCapturedPhoto} activeOpacity={0.85}>
              <Check size={21} color={theme.colors.paper} />
              <Text style={styles.usePhotoText}>Fotoğrafı Kullan</Text>
            </TouchableOpacity>
          </View>
        </View>
      );
    }

    return (
      <View style={styles.cameraStage}>
        <CameraView ref={cameraRef} style={StyleSheet.absoluteFill} facing={cameraFacing} active />
        <View style={styles.cameraTopBar}>
          <TouchableOpacity
            style={styles.cameraIconButton}
            onPress={() => setIsCameraOpen(false)}
            activeOpacity={0.75}
            accessibilityLabel="Kamerayı kapat"
          >
            <X size={24} color={theme.colors.paper} />
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.cameraIconButton}
            onPress={() => setCameraFacing(current => current === 'back' ? 'front' : 'back')}
            activeOpacity={0.75}
            accessibilityLabel="Kamerayı çevir"
          >
            <SwitchCamera size={24} color={theme.colors.paper} />
          </TouchableOpacity>
        </View>
        <View style={styles.captureControls}>
          <Text style={styles.cameraHint}>Malzemeyi çerçevenin ortasına yerleştirin</Text>
          <TouchableOpacity
            style={[styles.shutterOuter, isCapturing && styles.shutterDisabled]}
            onPress={handleCapture}
            disabled={isCapturing}
            activeOpacity={0.8}
            accessibilityLabel="Fotoğraf çek"
          >
            {isCapturing ? <ActivityIndicator color={theme.colors.primary} /> : <View style={styles.shutterInner} />}
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  if (step === 'camera') {
    return (
      <ScrollView
        style={styles.flowContainer}
        contentContainerStyle={styles.cameraScrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Photo upload dropzone card */}
        <TouchableOpacity
          style={styles.uploadCard}
          onPress={openCamera}
          activeOpacity={0.82}
        >
          <View style={styles.uploadIconBadge}>
            <CameraIcon size={28} color={theme.colors.primary} />
          </View>
          <Text style={styles.uploadCardTitle}>Fotoğraf Ekle veya Çek</Text>
          <Text style={styles.uploadCardSub}>
            Net ve aydınlık bir kare eklemek ilanının hızlıca fark edilmesini sağlar.
          </Text>
          <View style={styles.uploadActionBtn}>
            <CameraIcon size={17} color={theme.colors.paper} />
            <Text style={styles.uploadActionBtnText}>Kamerayı Aç</Text>
          </View>
        </TouchableOpacity>

        {/* AI Scanner Showcase Card */}
        <TouchableOpacity
          style={styles.aiScanCard}
          onPress={() => {
            setCapturedUri(MOCK_CUP_IMAGE);
            setIsAiScanned(true);
            setStep('scan');
          }}
          activeOpacity={0.88}
        >
          <View style={styles.aiCardTop}>
            <View style={styles.aiIconBadge}>
              <Sparkles size={20} color={theme.colors.accent} />
            </View>
            <View style={styles.aiPill}>
              <Text style={styles.aiPillText}>AI DESTEKLİ TARAMA</Text>
            </View>
          </View>
          <Text style={styles.aiCardTitle}>Akıllı Nesne & Durum Taraması</Text>
          <Text style={styles.aiCardSub}>
            Yapay zeka görseli tarasın; malzeme türünü, kondisyonunu ve çevresel etkisini saniyeler içinde analiz etsin.
          </Text>
          <View style={styles.aiScanBtn}>
            <Sparkles size={16} color="#241905" />
            <Text style={styles.aiScanBtnText}>Örnek Cam Bardak ile AI Taramayı Başlat</Text>
          </View>
        </TouchableOpacity>

        {/* AKTİF İLANLARIM Bottom Section */}
        <View style={styles.myAdsSection}>
          <View style={styles.myAdsHeader}>
            <Text style={styles.myAdsTitle}>AKTİF İLANLARIM</Text>
            <TouchableOpacity activeOpacity={0.7}>
              <Text style={styles.myAdsMore}>Tümü</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.myAdsList}>
            {myAds.map((item, idx) => (
              <View key={item.id} style={styles.myAdCard}>
                <View style={styles.myAdIconBadge}>
                  <Package size={20} color={theme.colors.primary} />
                </View>
                <View style={styles.myAdInfo}>
                  <Text style={styles.myAdTitle} numberOfLines={1}>{item.title}</Text>
                  <Text style={styles.myAdSub}>
                    {idx === 0 ? '2 saat önce paylaşıldı' : idx === 1 ? 'Dün paylaşıldı' : 'Geçen hafta paylaşıldı'}
                  </Text>
                </View>
                <View style={[styles.statusPill, item.status === 'ACTIVE' ? styles.statusActive : styles.statusDelivered]}>
                  <Text style={[styles.statusPillText, item.status === 'ACTIVE' ? styles.statusActiveText : styles.statusDeliveredText]}>
                    {item.status === 'ACTIVE' ? 'Aktif' : 'Teslim edildi'}
                  </Text>
                </View>
              </View>
            ))}
          </View>
        </View>
      </ScrollView>
    );
  }

  // ── AI Scan Step ──
  if (step === 'scan') {
    return (
      <AiScanScreen
        imageUri={capturedUri}
        onCancel={() => setStep('camera')}
        onApply={(result, appliedUri) => {
          setTitle(result.suggestedTitle);
          setCategory(result.category);
          setDescription(result.suggestedDescription);
          setCapturedUri(appliedUri);
          setIsAiScanned(true);
          setStep('form');
        }}
      />
    );
  }

  // ── Form step ──
  if (step === 'form') {
    return (
      <KeyboardAvoidingView
        style={styles.flowContainer}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.formContent}
        >
          {/* Photo confirmed */}
          {capturedUri && (
            <View style={styles.photoPreviewCard}>
              <Image source={{ uri: capturedUri }} style={styles.formPhotoPreview} resizeMode="cover" />
              <View style={styles.photoReadyRow}>
                <View style={styles.photoReadyCopy}>
                  <Check size={18} color={theme.colors.primary} />
                  <Text style={styles.photoConfirmText}>Fotoğraf hazır</Text>
                </View>
                <TouchableOpacity
                  style={styles.changePhotoButton}
                  onPress={() => { setStep('camera'); setCapturedUri(null); setIsCameraOpen(true); }}
                  activeOpacity={0.75}
                >
                  <RefreshCw size={16} color={theme.colors.primary} />
                  <Text style={styles.changePhotoText}>Değiştir</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}

          {/* AI Verified Banner if scanned */}
          {isAiScanned && (
            <View style={styles.aiFormBanner}>
              <Sparkles size={18} color={theme.colors.accent} />
              <View style={styles.aiFormBannerTextWrap}>
                <Text style={styles.aiFormBannerTitle}>Yapay Zeka Doğrulandı ✓</Text>
                <Text style={styles.aiFormBannerSub}>
                  Tespit: Cam Bardak • Durum: İyi • Kategori: Cam (CAM)
                </Text>
              </View>
            </View>
          )}

          <Text style={styles.formLabel}>İlan Başlığı</Text>
          <TextInput
            style={styles.textField}
            value={title}
            onChangeText={setTitle}
            placeholder="Örneğin: 15 adet koli"
            placeholderTextColor={theme.colors.inkFaint}
            selectionColor={theme.colors.primary}
            maxLength={80}
          />

          <Text style={styles.formLabel}>Kategori</Text>
          <View style={styles.chipRow}>
            {CATEGORIES.map(cat => (
              <TouchableOpacity
                key={cat.value}
                style={[styles.catChip, category === cat.value && styles.catChipActive]}
                onPress={() => setCategory(cat.value)}
                activeOpacity={0.7}
              >
                <Text style={[styles.catChipText, category === cat.value && styles.catChipTextActive]}>
                  {cat.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <Text style={styles.formLabel}>Açıklama</Text>
          <TextInput
            style={[styles.textField, styles.textArea]}
            value={description}
            onChangeText={setDescription}
            placeholder="Malzeme durumu, miktar, teslim koşulları..."
            placeholderTextColor={theme.colors.inkFaint}
            multiline
            numberOfLines={4}
            textAlignVertical="top"
            selectionColor={theme.colors.primary}
            maxLength={300}
          />

          <TouchableOpacity
            style={[styles.submitBtn, (!title.trim() || isSubmitting) && styles.submitBtnDisabled]}
            onPress={handleSubmit}
            activeOpacity={0.85}
            disabled={!title.trim() || isSubmitting}
          >
            {isSubmitting
              ? <ActivityIndicator size="small" color={theme.colors.paper} />
              : <Text style={styles.submitBtnText}>İlanı Yayınla</Text>
            }
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }

  // ── Success step ──
  return (
    <View style={[styles.flowContainer, styles.successContainer]}>
      <View style={styles.successIcon}>
        <Text style={styles.successIconText}>✓</Text>
      </View>
      <Text style={styles.successTitle}>İlan Yayınlandı</Text>
      <Text style={styles.successSub}>
        "{title}" ilanı aktif. İlgilenen kullanıcılar sizinle iletişime geçebilir.
      </Text>
      <View style={styles.rewardRow}>
        <View style={styles.rewardBadge}>
          <Text style={styles.rewardBadgeText}>+10 Eco-Coin</Text>
        </View>
        <View style={styles.rewardBadge}>
          <Text style={styles.rewardBadgeText}>+15 XP</Text>
        </View>
      </View>
      <TouchableOpacity style={styles.submitBtn} onPress={onClose} activeOpacity={0.85}>
        <Text style={styles.submitBtnText}>Tamam</Text>
      </TouchableOpacity>
    </View>
  );
};

// ---------------------------------------------------------------------------
// Need Ad Flow
// ---------------------------------------------------------------------------
const NeedAdFlow: React.FC<{ onClose: () => void }> = ({ onClose }) => {
  const { postNeedAd } = useAppState();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<MaterialCategory>('KARTON');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const handleSubmit = () => {
    if (!title.trim()) return;
    setIsSubmitting(true);
    setTimeout(() => {
      postNeedAd(title, category, description);
      setIsSubmitting(false);
      setDone(true);
    }, 1000);
  };

  if (done) {
    return (
      <View style={[styles.flowContainer, styles.successContainer]}>
        <View style={[styles.successIcon, { backgroundColor: theme.colors.primaryTint, borderColor: theme.colors.primary }]}>
          <Text style={[styles.successIconText, { color: theme.colors.primary }]}>✓</Text>
        </View>
        <Text style={styles.successTitle}>Talep Yayınlandı</Text>
        <Text style={styles.successSub}>
          "{title}" talebiniz aktif. Size uyan ilanlar çıktığında bildirim alacaksınız.
        </Text>
        <TouchableOpacity style={[styles.submitBtn, { backgroundColor: theme.colors.primary }]} onPress={onClose} activeOpacity={0.85}>
          <Text style={styles.submitBtnText}>Tamam</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={styles.flowContainer}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={styles.formContent}
      >
        <Text style={styles.formLabel}>Ne arıyorsunuz?</Text>
        <TextInput
          style={styles.textField}
          value={title}
          onChangeText={setTitle}
          placeholder="Örneğin: Cam kavanoz"
          placeholderTextColor={theme.colors.inkFaint}
          selectionColor={theme.colors.primary}
          maxLength={80}
        />

        <Text style={styles.formLabel}>Kategori</Text>
        <View style={styles.chipRow}>
          {CATEGORIES.map(cat => (
            <TouchableOpacity
              key={cat.value}
              style={[styles.catChip, category === cat.value && styles.catChipActive]}
              onPress={() => setCategory(cat.value)}
              activeOpacity={0.7}
            >
              <Text style={[styles.catChipText, category === cat.value && styles.catChipTextActive]}>
                {cat.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.formLabel}>Ek Bilgi</Text>
        <TextInput
          style={[styles.textField, styles.textArea]}
          value={description}
          onChangeText={setDescription}
          placeholder="Miktar, amaç, teslim alabileceğim bölge..."
          placeholderTextColor={theme.colors.inkFaint}
          multiline
          numberOfLines={4}
          textAlignVertical="top"
          selectionColor={theme.colors.primary}
          maxLength={300}
        />

        <TouchableOpacity
          style={[styles.submitBtn, { backgroundColor: theme.colors.primary }, (!title.trim() || isSubmitting) && styles.submitBtnDisabled]}
          onPress={handleSubmit}
          activeOpacity={0.85}
          disabled={!title.trim() || isSubmitting}
        >
          {isSubmitting
            ? <ActivityIndicator size="small" color={theme.colors.paper} />
            : <Text style={styles.submitBtnText}>Talep Yayınla</Text>
          }
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

// ---------------------------------------------------------------------------
// PostAdFlow entry point
// ---------------------------------------------------------------------------
export const PostAdFlow: React.FC<{ navigation: any; route: any }> = ({ navigation, route }) => {
  const insets = useSafeAreaInsets();
  const mode: 'give' | 'need' = route?.params?.mode ?? 'give';

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <StatusBar barStyle="dark-content" backgroundColor={theme.colors.bg} />

      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn} activeOpacity={0.7}>
          <Text style={styles.backBtnIcon}>‹</Text>
        </TouchableOpacity>
        <View style={styles.wordmark}>
          <Text style={styles.wordmarkText}>{mode === 'give' ? 'İlan Ver' : 'Talep Oluştur'}</Text>
        </View>
        <View style={styles.headerSpacer} />
      </View>

      {mode === 'give'
        ? <GiveAdFlow onClose={() => navigation.goBack()} />
        : <NeedAdFlow onClose={() => navigation.goBack()} />
      }
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 18,
    height: 52,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.line,
  },
  backBtn: {
    width: 48, height: 48, borderRadius: theme.radius.sm,
    borderWidth: 1, borderColor: theme.colors.lineStrong,
    backgroundColor: theme.colors.surface,
    alignItems: 'center', justifyContent: 'center',
  },
  backBtnIcon: { fontSize: 20, color: theme.colors.ink, lineHeight: 22 },
  wordmark: { alignItems: 'center' },
  wordmarkText: {
    fontFamily: 'Georgia',
    fontSize: theme.font.size.lg,
    fontWeight: '500',
    color: theme.colors.ink,
    letterSpacing: -0.2,
  },

  flowContainer: { flex: 1 },
  headerSpacer: { width: 48 },
  cameraScrollContent: {
    paddingHorizontal: 20,
    paddingTop: 10,
    paddingBottom: 40,
  },

  // Camera
  cameraStage: {
    flex: 1,
    backgroundColor: '#0B120E',
    overflow: 'hidden',
  },
  cameraPreview: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
    width: '100%',
    height: '100%',
  },
  cameraTopBar: {
    position: 'absolute',
    top: 16,
    left: 16,
    right: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  cameraIconButton: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: 'rgba(0,0,0,0.48)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  previewBadge: {
    minHeight: 40,
    paddingHorizontal: 14,
    borderRadius: theme.radius.full,
    backgroundColor: 'rgba(0,0,0,0.48)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  previewBadgeText: {
    color: theme.colors.paper,
    fontSize: theme.font.size.xs,
    fontWeight: '700',
  },
  captureControls: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 30,
    alignItems: 'center',
    gap: 16,
  },
  cameraHint: {
    color: theme.colors.paper,
    fontSize: theme.font.size.sm,
    fontWeight: '600',
    backgroundColor: 'rgba(0,0,0,0.48)',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: theme.radius.full,
  },
  shutterOuter: {
    width: 82,
    height: 82,
    borderRadius: 41,
    borderWidth: 5,
    borderColor: theme.colors.paper,
    backgroundColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  shutterDisabled: { opacity: 0.6 },
  shutterInner: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: theme.colors.paper,
  },
  previewActions: {
    position: 'absolute',
    left: 18,
    right: 18,
    bottom: 28,
    flexDirection: 'row',
    gap: 12,
  },
  retakeButton: {
    flex: 1,
    minHeight: 52,
    borderRadius: theme.radius.lg,
    backgroundColor: 'rgba(0,0,0,0.58)',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  retakeText: {
    color: theme.colors.paper,
    fontSize: 13,
    fontWeight: '700',
  },
  usePhotoButton: {
    flex: 1,
    minHeight: 52,
    borderRadius: theme.radius.lg,
    backgroundColor: theme.colors.primary,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  usePhotoText: {
    color: theme.colors.paper,
    fontSize: 13,
    fontWeight: '700',
  },

  // Upload Card
  uploadCard: {
    backgroundColor: theme.colors.surface,
    borderRadius: 22,
    borderWidth: 1.5,
    borderColor: theme.colors.lineStrong,
    borderStyle: 'dashed',
    padding: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 20,
  },
  uploadIconBadge: {
    width: 52,
    height: 52,
    borderRadius: 16,
    backgroundColor: theme.colors.primaryTint,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  uploadCardTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: theme.colors.ink,
    marginBottom: 4,
  },
  uploadCardSub: {
    fontSize: 12,
    color: theme.colors.inkFaint,
    textAlign: 'center',
    lineHeight: 18,
    maxWidth: 240,
    marginBottom: 16,
  },
  uploadActionBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: theme.colors.primary,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: theme.radius.full,
    minHeight: 48,
  },
  uploadActionBtnText: {
    fontSize: 12.5,
    fontWeight: '700',
    color: theme.colors.paper,
  },

  // AKTİF İLANLARIM section
  myAdsSection: {
    paddingHorizontal: 20,
    marginTop: 4,
  },
  myAdsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  myAdsTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: theme.colors.inkSoft,
    letterSpacing: 0.6,
  },
  myAdsMore: {
    fontSize: 12,
    fontWeight: '600',
    color: theme.colors.ink,
  },
  emptyMyAds: {
    padding: 16,
    alignItems: 'center',
  },
  emptyMyAdsText: {
    fontSize: 12,
    color: theme.colors.inkFaint,
  },
  myAdsList: {
    gap: 8,
  },
  myAdCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: theme.colors.line,
    padding: 12,
    gap: 12,
  },
  myAdIconBadge: {
    width: 40,
    height: 40,
    borderRadius: 12,
    backgroundColor: theme.colors.primaryTint,
    alignItems: 'center',
    justifyContent: 'center',
  },
  myAdInfo: {
    flex: 1,
  },
  myAdTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: theme.colors.ink,
    marginBottom: 2,
  },
  myAdSub: {
    fontSize: 11,
    color: theme.colors.inkFaint,
  },
  statusPill: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: theme.radius.full,
  },
  statusPillText: {
    fontSize: 10.5,
  },
  statusActive: {
    backgroundColor: '#F1E3C4',
  },
  statusActiveText: {
    fontSize: 10.5,
    fontWeight: '700',
    color: '#7A5A16',
  },
  statusDelivered: {
    backgroundColor: theme.colors.surfaceAlt,
  },
  statusDeliveredText: {
    fontSize: 10.5,
    fontWeight: '600',
    color: theme.colors.inkFaint,
  },

  // Form
  formContent: {
    padding: 22,
    paddingBottom: 48,
  },
  photoConfirmRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: theme.colors.primaryTint,
    borderRadius: theme.radius.sm,
    paddingHorizontal: 14,
    paddingVertical: 10,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: 'rgba(30,58,43,0.2)',
  },
  photoConfirmText: {
    fontSize: theme.font.size.sm,
    fontWeight: '600',
    color: theme.colors.primary,
  },
  photoPreviewCard: {
    backgroundColor: theme.colors.surface,
    borderRadius: theme.radius.lg,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: theme.colors.line,
    marginBottom: 20,
  },
  formPhotoPreview: {
    width: '100%',
    height: 210,
  },
  photoReadyRow: {
    minHeight: 56,
    paddingHorizontal: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  photoReadyCopy: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 7,
  },
  changePhotoButton: {
    minHeight: 48,
    paddingHorizontal: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
  },
  changePhotoText: {
    color: theme.colors.primary,
    fontSize: 12.5,
    fontWeight: '700',
  },
  photoConfirmCheck: {
    fontSize: 14,
    color: theme.colors.primary,
    fontWeight: '700',
  },
  formLabel: {
    fontSize: 12.5, fontWeight: '700',
    color: theme.colors.inkSoft,
    textTransform: 'uppercase', letterSpacing: 0.6,
    marginBottom: 8, marginTop: 16,
  },
  textField: {
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.lineStrong,
    borderRadius: theme.radius.sm,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: theme.font.size.base,
    color: theme.colors.ink,
    fontWeight: '400',
  },
  textArea: {
    height: 96,
    paddingTop: 12,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  catChip: {
    paddingHorizontal: 14, paddingVertical: 8,
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.surface,
    borderWidth: 1, borderColor: theme.colors.lineStrong,
  },
  catChipActive: {
    backgroundColor: theme.colors.primary,
    borderColor: theme.colors.primary,
  },
  catChipText: {
    fontSize: 11.5, fontWeight: '600',
    color: theme.colors.inkSoft,
  },
  catChipTextActive: {
    color: theme.colors.paper,
  },
  submitBtn: {
    backgroundColor: theme.colors.accent,
    borderRadius: theme.radius.sm,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 24,
    minHeight: 48,
    justifyContent: 'center',
  },
  submitBtnDisabled: { opacity: 0.45 },
  submitBtnText: {
    fontSize: theme.font.size.base,
    fontWeight: '700',
    color: '#241905',
    letterSpacing: 0.1,
  },

  // Success
  successContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 32,
    gap: 14,
  },
  successIcon: {
    width: 72, height: 72, borderRadius: 36,
    backgroundColor: theme.colors.accentTint,
    borderWidth: 2, borderColor: theme.colors.accent,
    alignItems: 'center', justifyContent: 'center',
  },
  successIconText: {
    fontSize: 28, fontWeight: '700',
    color: theme.colors.accent,
  },
  successTitle: {
    fontFamily: 'Georgia',
    fontSize: theme.font.size.xl,
    fontWeight: '500',
    color: theme.colors.ink,
    textAlign: 'center',
  },
  successSub: {
    fontSize: theme.font.size.sm,
    color: theme.colors.inkSoft,
    textAlign: 'center',
    lineHeight: 20,
  },
  rewardRow: { flexDirection: 'row', gap: 8, flexWrap: 'wrap', justifyContent: 'center' },
  rewardBadge: {
    backgroundColor: theme.colors.accentTint,
    borderRadius: theme.radius.full,
    paddingHorizontal: 12, paddingVertical: 6,
    borderWidth: 1, borderColor: theme.colors.accent,
  },
  rewardBadgeText: {
    fontSize: 12, fontWeight: '600', color: '#7A5A16',
  },

  // AI Showcase Card
  aiScanCard: {
    backgroundColor: '#1E3A2B',
    borderRadius: 22,
    padding: 20,
    marginBottom: 24,
    borderWidth: 1.5,
    borderColor: 'rgba(185,138,46,0.4)',
    shadowColor: '#16241B',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.12,
    shadowRadius: 16,
    elevation: 4,
  },
  aiCardTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  aiIconBadge: {
    width: 42,
    height: 42,
    borderRadius: 14,
    backgroundColor: 'rgba(185,138,46,0.2)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  aiPill: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: theme.radius.full,
    backgroundColor: 'rgba(185,138,46,0.25)',
    borderWidth: 1,
    borderColor: 'rgba(185,138,46,0.5)',
  },
  aiPillText: {
    fontSize: 10,
    fontWeight: '800',
    color: theme.colors.accent,
    letterSpacing: 0.8,
  },
  aiCardTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: '#F8FAF6',
    marginBottom: 6,
    fontFamily: Platform.OS === 'ios' ? 'Georgia' : 'serif',
  },
  aiCardSub: {
    fontSize: 12.5,
    color: 'rgba(248,250,246,0.72)',
    lineHeight: 18,
    marginBottom: 16,
  },
  aiScanBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: theme.colors.accent,
    paddingVertical: 13,
    paddingHorizontal: 16,
    borderRadius: theme.radius.sm,
    minHeight: 46,
  },
  aiScanBtnText: {
    fontSize: 13,
    fontWeight: '800',
    color: '#241905',
  },

  // AI Form Banner
  aiFormBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(185,138,46,0.14)',
    borderWidth: 1,
    borderColor: 'rgba(185,138,46,0.35)',
    borderRadius: theme.radius.md,
    padding: 14,
    marginBottom: 16,
    gap: 12,
  },
  aiFormBannerTextWrap: {
    flex: 1,
  },
  aiFormBannerTitle: {
    fontSize: 12.5,
    fontWeight: '700',
    color: '#7A5A16',
    marginBottom: 2,
  },
  aiFormBannerSub: {
    fontSize: 11.5,
    color: theme.colors.inkSoft,
    lineHeight: 16,
  },
});
