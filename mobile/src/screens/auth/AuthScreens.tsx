import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  StatusBar,
  ActivityIndicator,
  ScrollView,
  Image,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Phone, ChevronLeft } from 'lucide-react-native';
import { theme } from '../../theme/theme';
import { useAppState } from '../../context/AppStateContext';

// ---------------------------------------------------------------------------
// App Logo component
// ---------------------------------------------------------------------------
const LeafMark: React.FC = () => (
  <View style={styles.authMark}>
    <Image
      source={require('../../assets/yeniden_logo.png')}
      style={styles.authLogoImage}
      resizeMode="cover"
    />
  </View>
);

// ---------------------------------------------------------------------------
// AuthScreen
// ---------------------------------------------------------------------------
export const AuthScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const { requestOtp, verifyOtp, authError, loginDemo } = useAppState();

  const [step, setStep] = useState<'phone' | 'otp'>('phone');
  const [rawPhone, setRawPhone] = useState('');
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [isLoading, setIsLoading] = useState(false);
  const [challengeId, setChallengeId] = useState<string | null>(null);
  const [retryAfter, setRetryAfter] = useState(0);

  const otpRefs = useRef<(TextInput | null)[]>([]);

  useEffect(() => {
    if (retryAfter <= 0) return;
    const timer = setInterval(() => setRetryAfter(value => Math.max(0, value - 1)), 1000);
    return () => clearInterval(timer);
  }, [retryAfter]);

  // ── Phone formatting ──
  const formatPhone = (raw: string) => {
    const d = raw.slice(0, 10);
    let f = '';
    if (d.length > 0) f = d.slice(0, 3);
    if (d.length > 3) f += ' ' + d.slice(3, 6);
    if (d.length > 6) f += ' ' + d.slice(6, 8);
    if (d.length > 8) f += ' ' + d.slice(8, 10);
    return f;
  };

  const handlePhoneInput = (text: string) => {
    const digits = text.replace(/\D/g, '').slice(0, 10);
    setRawPhone(digits);
  };

  const handleSend = async () => {
    if (rawPhone.length < 10) return;
    setIsLoading(true);
    try {
      const challenge = await requestOtp(rawPhone);
      setChallengeId(challenge.challengeId);
      setRetryAfter(challenge.retryAfterSeconds);
      setIsLoading(false);
      setStep('otp');
      setTimeout(() => otpRefs.current[0]?.focus(), 80);
    } catch {
      setIsLoading(false);
      // A resend can be rejected by the cooldown while the previous
      // challenge is still valid. Return to the OTP cells so the user can
      // enter the code already received instead of getting stuck on phone.
      if (challengeId) {
        setStep('otp');
        setTimeout(() => otpRefs.current[0]?.focus(), 80);
      }
    }
  };

  // ── OTP logic ──
  const handleOtpChange = (value: string, index: number) => {
    const digits = value.replace(/\D/g, '');
    if (!digits) {
      const next = [...otp];
      next[index] = '';
      setOtp(next);
      return;
    }

    // Android can deliver a pasted OTP as one onChangeText event. Fill from
    // the active cell instead of keeping only the last character.
    const next = [...otp];
    digits.slice(0, 6 - index).split('').forEach((digit, offset) => {
      next[index + offset] = digit;
    });
    setOtp(next);
    const nextIndex = Math.min(5, index + digits.length);
    otpRefs.current[nextIndex]?.focus();
  };

  const handleOtpKey = (key: string, index: number) => {
    if (key === 'Backspace') {
      if (otp[index]) {
        const next = [...otp];
        next[index] = '';
        setOtp(next);
      } else if (index > 0) {
        const next = [...otp];
        next[index - 1] = '';
        setOtp(next);
        otpRefs.current[index - 1]?.focus();
      }
    }
  };

  const handleVerify = async () => {
    const code = otp.join('');
    if (code.length < 6 || !challengeId) return;
    setIsLoading(true);
    try { await verifyOtp(rawPhone, code, challengeId); } catch { /* error is rendered below */ }
    setIsLoading(false);
  };

  const handleResend = async () => {
    if (retryAfter > 0) return;
    setOtp(['', '', '', '', '', '']);
    await handleSend();
  };

  const formattedPhone = formatPhone(rawPhone);
  const phoneDisplay = rawPhone.slice(0, 3) + ' ' + rawPhone.slice(3, 6) + ' ' + rawPhone.slice(6, 8) + ' ' + rawPhone.slice(8, 10);

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <StatusBar barStyle="light-content" backgroundColor={theme.colors.authBg} />

      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView
          contentContainerStyle={[styles.scrollContent, { paddingBottom: insets.bottom + 32 }]}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          {/* Brand block */}
          <View style={styles.brandBlock}>
            <LeafMark />
            <Text style={styles.heading}>
              {'İhtiyacın varsa ara,\nfazlan varsa paylaş'}
            </Text>
            <Text style={styles.lead}>
              Telefon numaranla katıl, çevrendeki malzeme paylaşım ağını keşfet.
            </Text>
          </View>

          {/* Phone step */}
          {step === 'phone' && (
            <View style={styles.panel}>
              <View style={styles.phoneField}>
                <Phone size={18} color="rgba(248,250,246,0.6)" />
                <Text style={styles.phoneCode}>+90</Text>
                <TextInput
                  style={styles.phoneInput}
                  value={formattedPhone}
                  onChangeText={handlePhoneInput}
                  placeholder="5xx xxx xx xx"
                  placeholderTextColor="rgba(248,250,246,0.4)"
                  keyboardType="number-pad"
                  selectionColor={theme.colors.accent}
                  returnKeyType="done"
                  onSubmitEditing={handleSend}
                />
              </View>
              {authError && <Text style={styles.errorText}>{authError}</Text>}
              <TouchableOpacity
                style={[styles.ctaBtn, (rawPhone.length < 10 || isLoading) && styles.ctaBtnDisabled]}
                onPress={handleSend}
                activeOpacity={0.82}
                disabled={rawPhone.length < 10 || isLoading}
              >
                {isLoading
                  ? <ActivityIndicator size="small" color="#241905" />
                  : <Text style={styles.ctaBtnText}>Doğrulama kodu gönder</Text>
                }
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.demoBtn}
                onPress={loginDemo}
                activeOpacity={0.82}
              >
                <Text style={styles.demoBtnText}>Demo Modu ile Giriş Yap 🚀</Text>
              </TouchableOpacity>
            </View>
          )}

          {/* OTP step */}
          {step === 'otp' && (
            <View style={styles.panel}>
              {/* Back row */}
              <TouchableOpacity
                style={styles.backRow}
                onPress={() => { setStep('phone'); setOtp(['', '', '', '', '', '']); }}
                activeOpacity={0.7}
              >
                <ChevronLeft size={20} color="rgba(248,250,246,0.65)" />
                <Text style={styles.backText}>Geri</Text>
              </TouchableOpacity>

              {/* Description */}
              <Text style={styles.otpDesc}>
                +90 <Text style={styles.otpDescPhone}>{phoneDisplay}</Text> adresine gönderilen 6 haneli kodu girin.
              </Text>
              {authError && <Text style={styles.errorText}>{authError}</Text>}

              {/* OTP inputs */}
              <View style={styles.otpRow}>
                {otp.map((digit, i) => (
                  <TextInput
                    key={i}
                    ref={ref => { otpRefs.current[i] = ref; }}
                    style={[styles.otpBox, digit ? styles.otpBoxFilled : null]}
                    value={digit}
                    onChangeText={v => handleOtpChange(v, i)}
                    onKeyPress={({ nativeEvent }) => handleOtpKey(nativeEvent.key, i)}
                    keyboardType="number-pad"
                    // Keep room for Android's one-event paste; the handler
                    // distributes the sanitized digits across all six cells.
                    maxLength={6}
                    selectTextOnFocus
                    textContentType={i === 0 ? 'oneTimeCode' : 'none'}
                    selectionColor={theme.colors.accent}
                    textAlign="center"
                  />
                ))}
              </View>

              <TouchableOpacity
                style={[styles.ctaBtn, (otp.join('').length < 6 || isLoading) && styles.ctaBtnDisabled]}
                onPress={handleVerify}
                activeOpacity={0.82}
                disabled={otp.join('').length < 6 || isLoading}
              >
                {isLoading
                  ? <ActivityIndicator size="small" color="#241905" />
                  : <Text style={styles.ctaBtnText}>Doğrula ve devam et</Text>
                }
              </TouchableOpacity>

              <TouchableOpacity onPress={handleResend} activeOpacity={0.7} style={styles.resendRow}>
                <Text style={styles.resendText}>Kod gelmedi mi? </Text>
                <Text style={styles.resendLink}>{retryAfter > 0 ? `${retryAfter} sn sonra tekrar gönder` : 'Tekrar gönder'}</Text>
              </TouchableOpacity>
            </View>
          )}

          {/* Legal */}
          <Text style={styles.legal}>
            Devam ederek Kullanım Koşulları'nı ve Gizlilik Politikası'nı kabul etmiş olursun.
          </Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
};

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------
const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: theme.colors.authBg,
  },
  flex: { flex: 1 },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 28,
    paddingTop: 48,
    justifyContent: 'center',
    alignItems: 'center',
  },

  // Brand
  brandBlock: {
    alignItems: 'center',
    marginBottom: 36,
    marginTop: 20,
  },
  authMark: {
    width: 72,
    height: 72,
    borderRadius: 20,
    backgroundColor: '#F7F8F4',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 22,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 10,
    elevation: 4,
  },
  authLogoImage: {
    width: 72,
    height: 72,
    borderRadius: 20,
  },
  heading: {
    fontFamily: Platform.OS === 'ios' ? 'Georgia' : 'serif',
    fontSize: theme.font.size['2xl'],
    fontWeight: '500',
    color: theme.colors.authPaper,
    textAlign: 'center',
    lineHeight: 34,
    letterSpacing: -0.3,
    marginBottom: 10,
  },
  lead: {
    fontSize: theme.font.size.sm,
    color: 'rgba(248,250,246,0.68)',
    textAlign: 'center',
    lineHeight: 20,
    maxWidth: 260,
  },

  // Panel
  panel: {
    width: '100%',
    marginBottom: 8,
  },

  // Phone field
  phoneField: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: 'rgba(248,250,246,0.08)',
    borderWidth: 1,
    borderColor: 'rgba(248,250,246,0.20)',
    borderRadius: theme.radius.sm,
    paddingHorizontal: 16,
    paddingVertical: 14,
    marginBottom: 14,
  },
  phoneCode: {
    fontSize: 13,
    color: 'rgba(248,250,246,0.65)',
    fontWeight: '500',
  },
  phoneInput: {
    flex: 1,
    fontSize: theme.font.size.base,
    color: theme.colors.authPaper,
    fontWeight: '500',
    letterSpacing: 0.8,
    padding: 0,
  },

  // CTA button
  ctaBtn: {
    width: '100%',
    backgroundColor: theme.colors.accent,
    borderRadius: theme.radius.sm,
    paddingVertical: 14,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 48,
  },
  ctaBtnDisabled: {
    opacity: 0.45,
  },
  ctaBtnText: {
    fontSize: theme.font.size.base,
    fontWeight: '700',
    color: '#241905',
    letterSpacing: 0.1,
  },
  demoBtn: {
    width: '100%',
    borderRadius: theme.radius.sm,
    borderWidth: 1,
    borderColor: 'rgba(248,250,246,0.28)',
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 44,
    marginTop: 10,
  },
  demoBtnText: {
    fontSize: theme.font.size.sm,
    fontWeight: '600',
    color: 'rgba(248,250,246,0.82)',
  },

  // OTP
  backRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 14,
    gap: 4,
  },
  backText: {
    fontSize: 12,
    color: 'rgba(248,250,246,0.55)',
    fontWeight: '500',
  },
  otpDesc: {
    fontSize: 12,
    color: 'rgba(248,250,246,0.55)',
    lineHeight: 18,
    marginBottom: 16,
  },
  otpDescPhone: {
    color: 'rgba(248,250,246,0.85)',
    fontWeight: '600',
  },
  otpRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 7,
    marginBottom: 18,
  },
  otpBox: {
    width: 42,
    height: 52,
    borderRadius: 12,
    backgroundColor: 'rgba(248,250,246,0.08)',
    borderWidth: 1.5,
    borderColor: 'rgba(248,250,246,0.22)',
    fontSize: 20,
    fontFamily: Platform.OS === 'ios' ? 'Georgia' : 'serif',
    fontWeight: '500',
    color: theme.colors.authPaper,
    textAlign: 'center',
  },
  otpBoxFilled: {
    borderColor: theme.colors.accent,
    backgroundColor: 'rgba(185,138,46,0.18)',
  },
  resendRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: 12,
  },
  resendText: {
    fontSize: 11.5,
    color: 'rgba(248,250,246,0.45)',
  },
  resendLink: {
    fontSize: 11.5,
    color: theme.colors.accent,
    fontWeight: '600',
    textDecorationLine: 'underline',
  },

  // Legal
  legal: {
    fontSize: 11,
    color: 'rgba(248,250,246,0.38)',
    textAlign: 'center',
    lineHeight: 16,
    marginTop: 20,
    maxWidth: 260,
  },
  errorText: {
    color: '#ffb4ab',
    textAlign: 'center',
    marginBottom: 12,
  },
});
