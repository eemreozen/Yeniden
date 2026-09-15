import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  StyleSheet,
  Animated,
  Easing,
  Platform,
  Dimensions,
  ScrollView,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  Sparkles,
  ShieldCheck,
  RotateCcw,
  ArrowRight,
  X,
  Leaf,
  Droplets,
  CheckCircle2,
  Cpu,
  Layers,
  Zap,
} from 'lucide-react-native';
import { theme } from '../../theme/theme';
import { MaterialCategory } from '../../context/AppStateContext';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

// ---------------------------------------------------------------------------
// Types & Mock Data
// ---------------------------------------------------------------------------
export interface AiAnalysisResult {
  detectedObject: string;
  category: MaterialCategory;
  confidence: number;
  condition: string;
  material: string;
  damageStatus: string;
  cleanliness: string;
  co2SavedKg: number;
  waterSavedLiters: number;
  ecoCoinReward: number;
  suggestedTitle: string;
  suggestedDescription: string;
  tags: string[];
}

export const MOCK_GLASS_CUP_RESULT: AiAnalysisResult = {
  detectedObject: 'Cam Bardak',
  category: 'CAM',
  confidence: 98.7,
  condition: 'İyi',
  material: 'Şeffaf Soda-Kireç Camı (%100 Geri Dönüştürülebilir)',
  damageStatus: 'Kırık, çatlak veya çizik tespit edilmedi',
  cleanliness: 'Temiz & Parlak',
  co2SavedKg: 0.35,
  waterSavedLiters: 1.2,
  ecoCoinReward: 15,
  suggestedTitle: 'Temiz Cam Su Bardağı',
  suggestedDescription:
    'Yapay zeka servisiyle analiz edildi: Durumu iyi, çatlak veya kırık bulunmayan şeffaf cam bardak. Hijyenik ve hemen yeniden kullanıma uygundur.',
  tags: ['#cam', '#bardak', '#mutfak', '#sıfıratık', '#yeniden'],
};

export const MOCK_CUP_IMAGE =
  'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&q=80';

// ---------------------------------------------------------------------------
// Scanning Stages Configuration
// ---------------------------------------------------------------------------
interface ScanStage {
  threshold: number;
  title: string;
  badge: string;
  sub: string;
  log: string;
}

const SCAN_STAGES: ScanStage[] = [
  {
    threshold: 20,
    title: 'Görsel yapay zeka servisine yükleniyor...',
    badge: 'NEURAL NET INITIALIZING',
    sub: 'Atık-Vision AI Core v2.4 (Edge/Cloud Hibrit)',
    log: '> Görsel tensör matrisine dönüştürüldü [256x256]',
  },
  {
    threshold: 45,
    title: 'Piksel matrisi taranıyor & nesne sınırları belirleniyor...',
    badge: 'OBJECT LOCALIZATION',
    sub: 'YOLO-V8 Vision Derin Öğrenme Katmanı',
    log: '> Nesne sınır kutusu (bounding box) kilitlendi [%98.7]',
  },
  {
    threshold: 70,
    title: 'Materyal analizi: Şeffaf cam yüzeyi doğrulanıyor...',
    badge: 'SPECTRAL COMPOSITION',
    sub: 'Kırılma İndisi: 1.52 (Soda-Kireç Camı Doğrulandı)',
    log: '> Materyal tipi: %100 Geri Dönüştürülebilir Cam',
  },
  {
    threshold: 90,
    title: 'Kondisyon kontrolü: Çatlak, kırık ve leke taranıyor...',
    badge: 'DEFECT & INTEGRITY CHECK',
    sub: 'Kusur tespiti: 0 hata / Pürüzsüz yüzey',
    log: '> Durum analizi: İYİ (Kusursuz kondisyon)',
  },
  {
    threshold: 100,
    title: 'Eko-değer ve yeniden kullanım skoru hesaplanıyor...',
    badge: 'ECO-IMPACT CALCULATION',
    sub: 'Döngüsel Ekonomi Veritabanı Eşleşmesi',
    log: '> Karbon ve su tasarrufu hesaplandı (+15 Eco-Coin)',
  },
];

interface AiScanScreenProps {
  imageUri: string | null;
  onApply: (data: AiAnalysisResult, imageUri: string) => void;
  onCancel: () => void;
}

export const AiScanScreen: React.FC<AiScanScreenProps> = ({
  imageUri,
  onApply,
  onCancel,
}) => {
  const insets = useSafeAreaInsets();
  const effectiveUri = imageUri || MOCK_CUP_IMAGE;

  // Scanning State
  const [progress, setProgress] = useState(0);
  const [currentStageIndex, setCurrentStageIndex] = useState(0);
  const [isScanningComplete, setIsScanningComplete] = useState(false);

  // Animated values
  const scanBeamY = useRef(new Animated.Value(0)).current;
  const pulseAnim = useRef(new Animated.Value(1)).current;
  const reticleAnim = useRef(new Animated.Value(0)).current;
  const boxAnim = useRef(new Animated.Value(0)).current;
  const resultFadeAnim = useRef(new Animated.Value(0)).current;

  // ── Laser Beam & Pulse Loop ──
  useEffect(() => {
    // Laser Beam looping up and down
    const beamAnimation = Animated.loop(
      Animated.sequence([
        Animated.timing(scanBeamY, {
          toValue: 1,
          duration: 1600,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(scanBeamY, {
          toValue: 0,
          duration: 1600,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ]),
    );
    beamAnimation.start();

    // Pulse loop for HUD indicators
    const pulseAnimation = Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, {
          toValue: 1.12,
          duration: 800,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(pulseAnim, {
          toValue: 1,
          duration: 800,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ]),
    );
    pulseAnimation.start();

    // Fade in reticle
    Animated.timing(reticleAnim, {
      toValue: 1,
      duration: 600,
      useNativeDriver: true,
    }).start();

    return () => {
      beamAnimation.stop();
      pulseAnimation.stop();
    };
  }, [scanBeamY, pulseAnim, reticleAnim]);

  // ── Simulated Scanning Progression ──
  const startScan = () => {
    setProgress(0);
    setCurrentStageIndex(0);
    setIsScanningComplete(false);
    boxAnim.setValue(0);
    resultFadeAnim.setValue(0);

    const totalDuration = 4600; // ms
    const intervalTime = 50;
    const totalSteps = totalDuration / intervalTime;
    let currentStep = 0;

    const timer = setInterval(() => {
      currentStep++;
      const currentPct = Math.min(100, Math.round((currentStep / totalSteps) * 100));
      setProgress(currentPct);

      // Update stage based on percentage
      if (currentPct >= 90) setCurrentStageIndex(4);
      else if (currentPct >= 70) setCurrentStageIndex(3);
      else if (currentPct >= 45) {
        setCurrentStageIndex(2);
        // Show bounding box
        Animated.timing(boxAnim, {
          toValue: 1,
          duration: 400,
          useNativeDriver: true,
        }).start();
      } else if (currentPct >= 20) {
        setCurrentStageIndex(1);
      } else {
        setCurrentStageIndex(0);
      }

      if (currentStep >= totalSteps) {
        clearInterval(timer);
        setTimeout(() => {
          setIsScanningComplete(true);
          Animated.timing(resultFadeAnim, {
            toValue: 1,
            duration: 500,
            useNativeDriver: true,
          }).start();
        }, 400);
      }
    }, intervalTime);

    return timer;
  };

  useEffect(() => {
    const timer = startScan();
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const currentStage = SCAN_STAGES[currentStageIndex];

  // Laser beam translation
  const beamTranslateY = scanBeamY.interpolate({
    inputRange: [0, 1],
    outputRange: [10, 240], // Matches viewfinder height
  });

  return (
    <View style={[styles.container, { paddingTop: insets.top, paddingBottom: insets.bottom }]}>
      {/* Top Header */}
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.closeBtn}
          onPress={onCancel}
          activeOpacity={0.75}
          accessibilityLabel="Taramayı İptal Et"
        >
          <X size={20} color="rgba(248,250,246,0.8)" />
        </TouchableOpacity>

        <View style={styles.headerCenter}>
          <View style={styles.liveIndicator}>
            <Animated.View style={[styles.liveDot, { transform: [{ scale: pulseAnim }] }]} />
            <Text style={styles.headerTitle}>AI VISION SCANNER</Text>
          </View>
          <Text style={styles.headerSubtitle}>Akıllı Nesne & Durum Tespiti</Text>
        </View>

        <View style={styles.versionBadge}>
          <Text style={styles.versionBadgeText}>v2.4</Text>
        </View>
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
        bounces={false}
      >
        {/* ── Viewport / Image Area ── */}
        <View style={styles.viewportWrapper}>
          <View style={styles.viewport}>
            <Image
              source={{ uri: effectiveUri }}
              style={styles.viewportImage}
              resizeMode="cover"
            />

            {/* Dark Tech Vignette Overlay */}
            <View style={styles.vignetteOverlay} />

            {/* Corner Reticles */}
            <Animated.View style={[styles.cornerReticles, { opacity: reticleAnim }]}>
              {/* Top-Left */}
              <View style={[styles.cornerBracket, styles.cornerTL]} />
              {/* Top-Right */}
              <View style={[styles.cornerBracket, styles.cornerTR]} />
              {/* Bottom-Left */}
              <View style={[styles.cornerBracket, styles.cornerBL]} />
              {/* Bottom-Right */}
              <View style={[styles.cornerBracket, styles.cornerBR]} />

              {/* Center Crosshair */}
              <View style={styles.crosshairH} />
              <View style={styles.crosshairV} />
            </Animated.View>

            {/* Detected Bounding Box (Appears at 45%+) */}
            <Animated.View
              style={[
                styles.detectedBox,
                {
                  opacity: boxAnim,
                  transform: [
                    {
                      scale: boxAnim.interpolate({
                        inputRange: [0, 1],
                        outputRange: [0.88, 1],
                      }),
                    },
                  ],
                },
              ]}
            >
              <View style={styles.boxTag}>
                <Sparkles size={11} color="#16241B" />
                <Text style={styles.boxTagText}>Cam Bardak (%98.7)</Text>
              </View>
            </Animated.View>

            {/* Scanning Laser Beam */}
            {!isScanningComplete && (
              <Animated.View
                style={[
                  styles.laserBeam,
                  {
                    transform: [{ translateY: beamTranslateY }],
                  },
                ]}
              >
                <View style={styles.laserLine} />
                <View style={styles.laserGlow} />
              </Animated.View>
            )}

            {/* Viewport Floating Badges */}
            <View style={styles.floatingTopTag}>
              <Cpu size={12} color="#10B981" />
              <Text style={styles.floatingTagText}>{currentStage.badge}</Text>
            </View>

            <View style={styles.floatingBottomTag}>
              <Text style={styles.floatingCoords}>KONDİSYON: KONTROL EDİLİYOR</Text>
            </View>
          </View>
        </View>

        {/* ── Scanning Progression HUD (When scanning) ── */}
        {!isScanningComplete ? (
          <View style={styles.hudSection}>
            {/* Progress Bar & Percentage */}
            <View style={styles.progressRow}>
              <View style={styles.progressTrack}>
                <View style={[styles.progressFill, { width: `${progress}%` }]} />
              </View>
              <Text style={styles.progressPercent}>%{progress}</Text>
            </View>

            {/* Dynamic Stage Info */}
            <View style={styles.stageCard}>
              <View style={styles.stageIconWrap}>
                <Layers size={22} color={theme.colors.accent} />
              </View>
              <View style={styles.stageTextWrap}>
                <Text style={styles.stageTitle}>{currentStage.title}</Text>
                <Text style={styles.stageSub}>{currentStage.sub}</Text>
              </View>
            </View>

            {/* Terminal Live Diagnostics */}
            <View style={styles.terminalBox}>
              <View style={styles.terminalHeader}>
                <View style={styles.terminalDotGreen} />
                <Text style={styles.terminalTitle}>CANLI ANALİZ TELEMETRİSİ</Text>
              </View>
              {SCAN_STAGES.slice(0, currentStageIndex + 1).map((stage, idx) => (
                <Text key={idx} style={styles.terminalLog}>
                  {stage.log}
                </Text>
              ))}
            </View>
          </View>
        ) : (
          /* ── Scan Completed: Mock Bardak Results Card ── */
          <Animated.View style={[styles.resultsCard, { opacity: resultFadeAnim }]}>
            {/* Verified Header */}
            <View style={styles.resultBanner}>
              <View style={styles.verifiedIconWrap}>
                <ShieldCheck size={28} color="#10B981" />
              </View>
              <View style={styles.bannerInfo}>
                <View style={styles.bannerBadgeRow}>
                  <View style={styles.badgeSuccess}>
                    <Text style={styles.badgeSuccessText}>AI Doğrulandı ✓</Text>
                  </View>
                  <View style={styles.conditionPill}>
                    <CheckCircle2 size={13} color="#065F46" />
                    <Text style={styles.conditionPillText}>Durum: İYİ</Text>
                  </View>
                </View>
                <Text style={styles.resultMainTitle}>{MOCK_GLASS_CUP_RESULT.detectedObject}</Text>
              </View>
            </View>

            {/* Detailed Parameters Grid */}
            <View style={styles.gridContainer}>
              <View style={styles.gridItem}>
                <Text style={styles.gridLabel}>KATEGORİ</Text>
                <Text style={styles.gridValue}>Cam (CAM)</Text>
              </View>

              <View style={styles.gridItem}>
                <Text style={styles.gridLabel}>DOĞRULUK</Text>
                <Text style={[styles.gridValue, styles.gridValueGreen]}>
                  %{MOCK_GLASS_CUP_RESULT.confidence}
                </Text>
              </View>

              <View style={styles.gridItem}>
                <Text style={styles.gridLabel}>MATERYAL</Text>
                <Text style={styles.gridValue}>Soda-Kireç Camı</Text>
              </View>

              <View style={styles.gridItem}>
                <Text style={styles.gridLabel}>HASAR DURUMU</Text>
                <Text style={[styles.gridValue, styles.gridValueGreen]}>0 Hasar / Kusursuz</Text>
              </View>
            </View>

            {/* Eco Impact & Reward Badges */}
            <View style={styles.impactBox}>
              <View style={styles.impactTitleRow}>
                <Sparkles size={16} color={theme.colors.accent} />
                <Text style={styles.impactTitle}>Tahmini Çevresel Tasarruf & Ödül</Text>
              </View>
              <View style={styles.impactStatsRow}>
                <View style={styles.impactStat}>
                  <Leaf size={16} color="#10B981" />
                  <Text style={styles.impactStatVal}>+{MOCK_GLASS_CUP_RESULT.co2SavedKg} kg</Text>
                  <Text style={styles.impactStatLabel}>CO₂ Tasarrufu</Text>
                </View>

                <View style={styles.impactDivider} />

                <View style={styles.impactStat}>
                  <Droplets size={16} color="#38BDF8" />
                  <Text style={styles.impactStatVal}>+{MOCK_GLASS_CUP_RESULT.waterSavedLiters} L</Text>
                  <Text style={styles.impactStatLabel}>Su Tasarrufu</Text>
                </View>

                <View style={styles.impactDivider} />

                <View style={styles.impactStat}>
                  <Zap size={16} color={theme.colors.accent} />
                  <Text style={[styles.impactStatVal, styles.impactStatValGold]}>
                    +{MOCK_GLASS_CUP_RESULT.ecoCoinReward}
                  </Text>
                  <Text style={styles.impactStatLabel}>Eco-Coin</Text>
                </View>
              </View>
            </View>

            {/* AI Note / Suggestion */}
            <View style={styles.aiNoteCard}>
              <Text style={styles.aiNoteTitle}>Yapay Zeka Açıklaması:</Text>
              <Text style={styles.aiNoteBody}>{MOCK_GLASS_CUP_RESULT.suggestedDescription}</Text>
            </View>

            {/* Action Buttons */}
            <View style={styles.actionButtons}>
              <TouchableOpacity
                style={styles.applyButton}
                onPress={() => onApply(MOCK_GLASS_CUP_RESULT, effectiveUri)}
                activeOpacity={0.85}
              >
                <Text style={styles.applyButtonText}>İlan Bilgilerine Aktar ve Devam Et</Text>
                <ArrowRight size={18} color="#241905" />
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.rescanButton}
                onPress={startScan}
                activeOpacity={0.8}
              >
                <RotateCcw size={16} color="rgba(248,250,246,0.7)" />
                <Text style={styles.rescanButtonText}>Yeniden Tara</Text>
              </TouchableOpacity>
            </View>
          </Animated.View>
        )}
      </ScrollView>
    </View>
  );
};

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0A120E', // Deep neural dark green
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.08)',
  },
  closeBtn: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255,255,255,0.08)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerCenter: {
    alignItems: 'center',
  },
  liveIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#10B981',
  },
  headerTitle: {
    fontSize: 13,
    fontWeight: '800',
    color: '#F8FAF6',
    letterSpacing: 1.2,
  },
  headerSubtitle: {
    fontSize: 11,
    color: 'rgba(248,250,246,0.5)',
    marginTop: 2,
  },
  versionBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    backgroundColor: 'rgba(16,185,129,0.14)',
    borderWidth: 1,
    borderColor: 'rgba(16,185,129,0.3)',
  },
  versionBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#10B981',
  },
  scrollContent: {
    paddingBottom: 40,
    alignItems: 'center',
  },

  // Viewport
  viewportWrapper: {
    width: SCREEN_WIDTH - 40,
    marginVertical: 16,
  },
  viewport: {
    width: '100%',
    height: 260,
    borderRadius: 24,
    overflow: 'hidden',
    backgroundColor: '#040805',
    borderWidth: 1.5,
    borderColor: 'rgba(16,185,129,0.35)',
    position: 'relative',
  },
  viewportImage: {
    width: '100%',
    height: '100%',
  },
  vignetteOverlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(10,18,14,0.3)',
  },

  // Reticles
  cornerReticles: {
    ...StyleSheet.absoluteFill,
  },
  cornerBracket: {
    position: 'absolute',
    width: 26,
    height: 26,
    borderColor: '#10B981',
  },
  cornerTL: {
    top: 14,
    left: 14,
    borderTopWidth: 3,
    borderLeftWidth: 3,
    borderTopLeftRadius: 6,
  },
  cornerTR: {
    top: 14,
    right: 14,
    borderTopWidth: 3,
    borderRightWidth: 3,
    borderTopRightRadius: 6,
  },
  cornerBL: {
    bottom: 14,
    left: 14,
    borderBottomWidth: 3,
    borderLeftWidth: 3,
    borderBottomLeftRadius: 6,
  },
  cornerBR: {
    bottom: 14,
    right: 14,
    borderBottomWidth: 3,
    borderRightWidth: 3,
    borderBottomRightRadius: 6,
  },
  crosshairH: {
    position: 'absolute',
    top: '50%',
    left: '42%',
    right: '42%',
    height: 1,
    backgroundColor: 'rgba(16,185,129,0.4)',
  },
  crosshairV: {
    position: 'absolute',
    left: '50%',
    top: '42%',
    bottom: '42%',
    width: 1,
    backgroundColor: 'rgba(16,185,129,0.4)',
  },

  // Detected box
  detectedBox: {
    position: 'absolute',
    top: 35,
    left: '20%',
    width: '60%',
    height: 180,
    borderWidth: 2,
    borderColor: '#E7B44A',
    borderRadius: 16,
    borderStyle: 'dashed',
    backgroundColor: 'rgba(231,180,74,0.1)',
  },
  boxTag: {
    position: 'absolute',
    top: -12,
    left: 12,
    backgroundColor: '#E7B44A',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  boxTagText: {
    fontSize: 10.5,
    fontWeight: '800',
    color: '#16241B',
  },

  // Laser beam
  laserBeam: {
    position: 'absolute',
    left: 0,
    right: 0,
    height: 12,
  },
  laserLine: {
    height: 2.5,
    backgroundColor: '#10B981',
    shadowColor: '#10B981',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.9,
    shadowRadius: 10,
    elevation: 8,
  },
  laserGlow: {
    height: 14,
    backgroundColor: 'rgba(16,185,129,0.18)',
  },

  // Floating viewport tags
  floatingTopTag: {
    position: 'absolute',
    top: 14,
    alignSelf: 'center',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(10,18,14,0.85)',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(16,185,129,0.3)',
  },
  floatingTagText: {
    fontSize: 9.5,
    fontWeight: '800',
    color: '#10B981',
    letterSpacing: 0.8,
  },
  floatingBottomTag: {
    position: 'absolute',
    bottom: 12,
    alignSelf: 'center',
    backgroundColor: 'rgba(10,18,14,0.75)',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  floatingCoords: {
    fontSize: 9,
    color: 'rgba(248,250,246,0.6)',
    letterSpacing: 0.5,
    fontWeight: '600',
  },

  // HUD Section
  hudSection: {
    width: SCREEN_WIDTH - 40,
    marginTop: 4,
  },
  progressRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 16,
  },
  progressTrack: {
    flex: 1,
    height: 8,
    borderRadius: 4,
    backgroundColor: 'rgba(255,255,255,0.08)',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#10B981',
    borderRadius: 4,
  },
  progressPercent: {
    fontSize: 14,
    fontWeight: '800',
    color: '#10B981',
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    minWidth: 44,
    textAlign: 'right',
  },
  stageCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.05)',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.09)',
    marginBottom: 14,
    gap: 14,
  },
  stageIconWrap: {
    width: 44,
    height: 44,
    borderRadius: 14,
    backgroundColor: 'rgba(185,138,46,0.18)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  stageTextWrap: {
    flex: 1,
  },
  stageTitle: {
    fontSize: 13.5,
    fontWeight: '700',
    color: '#F8FAF6',
    marginBottom: 3,
  },
  stageSub: {
    fontSize: 11.5,
    color: 'rgba(248,250,246,0.5)',
  },

  // Terminal box
  terminalBox: {
    backgroundColor: '#050906',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  terminalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
    paddingBottom: 6,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.06)',
  },
  terminalDotGreen: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#10B981',
  },
  terminalTitle: {
    fontSize: 10,
    fontWeight: '700',
    color: 'rgba(248,250,246,0.45)',
    letterSpacing: 0.8,
  },
  terminalLog: {
    fontSize: 11,
    color: '#10B981',
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    lineHeight: 18,
  },

  // Results Card
  resultsCard: {
    width: SCREEN_WIDTH - 40,
    backgroundColor: 'rgba(255,255,255,0.05)',
    borderRadius: 24,
    padding: 18,
    borderWidth: 1.5,
    borderColor: 'rgba(16,185,129,0.4)',
  },
  resultBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    marginBottom: 16,
    paddingBottom: 14,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.08)',
  },
  verifiedIconWrap: {
    width: 52,
    height: 52,
    borderRadius: 18,
    backgroundColor: 'rgba(16,185,129,0.15)',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
    borderColor: 'rgba(16,185,129,0.35)',
  },
  bannerInfo: {
    flex: 1,
  },
  bannerBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 4,
  },
  badgeSuccess: {
    backgroundColor: 'rgba(16,185,129,0.2)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  badgeSuccessText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#10B981',
  },
  conditionPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: '#D1FAE5',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  conditionPillText: {
    fontSize: 10.5,
    fontWeight: '800',
    color: '#065F46',
  },
  resultMainTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#F8FAF6',
    letterSpacing: -0.2,
  },

  // Grid
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 16,
  },
  gridItem: {
    width: (SCREEN_WIDTH - 40 - 36 - 10) / 2,
    backgroundColor: 'rgba(255,255,255,0.04)',
    borderRadius: 12,
    padding: 10,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  gridLabel: {
    fontSize: 9.5,
    fontWeight: '700',
    color: 'rgba(248,250,246,0.45)',
    letterSpacing: 0.6,
    marginBottom: 3,
  },
  gridValue: {
    fontSize: 12,
    fontWeight: '700',
    color: '#F8FAF6',
  },
  gridValueGreen: {
    color: '#10B981',
  },

  // Impact Box
  impactBox: {
    backgroundColor: 'rgba(185,138,46,0.12)',
    borderRadius: 16,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(231,180,74,0.3)',
    marginBottom: 16,
  },
  impactTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 10,
  },
  impactTitle: {
    fontSize: 11.5,
    fontWeight: '700',
    color: '#E7B44A',
  },
  impactStatsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
  },
  impactStat: {
    alignItems: 'center',
    gap: 3,
  },
  impactStatVal: {
    fontSize: 14,
    fontWeight: '800',
    color: '#F8FAF6',
  },
  impactStatValGold: {
    color: theme.colors.accent,
  },
  impactStatLabel: {
    fontSize: 10,
    color: 'rgba(248,250,246,0.6)',
  },
  impactDivider: {
    width: 1,
    height: 28,
    backgroundColor: 'rgba(255,255,255,0.1)',
  },

  // AI Note
  aiNoteCard: {
    backgroundColor: 'rgba(255,255,255,0.03)',
    borderRadius: 14,
    padding: 12,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  aiNoteTitle: {
    fontSize: 11,
    fontWeight: '700',
    color: 'rgba(248,250,246,0.6)',
    marginBottom: 4,
  },
  aiNoteBody: {
    fontSize: 12,
    color: 'rgba(248,250,246,0.85)',
    lineHeight: 18,
  },

  // Actions
  actionButtons: {
    gap: 10,
  },
  applyButton: {
    backgroundColor: theme.colors.accent,
    borderRadius: 14,
    paddingVertical: 14,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  applyButtonText: {
    fontSize: 14,
    fontWeight: '800',
    color: '#241905',
  },
  rescanButton: {
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: 'rgba(255,255,255,0.06)',
    borderRadius: 12,
  },
  rescanButtonText: {
    fontSize: 12.5,
    fontWeight: '600',
    color: 'rgba(248,250,246,0.7)',
  },
});
