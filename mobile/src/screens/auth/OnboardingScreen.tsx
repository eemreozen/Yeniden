import React, { useState } from 'react';
import { ActivityIndicator, Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { ArrowRight, Check, ShieldCheck, Sparkles } from 'lucide-react-native';
import { useAppState } from '../../context/AppStateContext';
import { theme } from '../../theme/theme';

const AVATARS = [
  { key: 'avatar-leaf.png', emoji: '🌿', label: 'Filiz' },
  { key: 'avatar-sun.png', emoji: '🌞', label: 'Güneş' },
  { key: 'avatar-plant.png', emoji: '🪴', label: 'Bahçe' },
  { key: 'avatar-earth.png', emoji: '🌍', label: 'Dünya' },
];

export const OnboardingScreen: React.FC = () => {
  const { user, completeOnboarding, authError } = useAppState();
  const [name, setName] = useState('');
  const [avatarKey, setAvatarKey] = useState(AVATARS[0].key);
  const [isSaving, setIsSaving] = useState(false);
  const [isFocused, setIsFocused] = useState(false);
  const nameIsValid = name.trim().length >= 2;

  const submit = async () => {
    if (name.trim().length < 2 || isSaving) return;
    setIsSaving(true);
    try { await completeOnboarding(name, avatarKey); } catch { /* rendered by context */ }
    finally { setIsSaving(false); }
  };

  return (
    <KeyboardAvoidingView style={styles.root} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={styles.topRow}>
          <View style={styles.logo}>
            <Image
              source={require('../../assets/yeniden_logo.png')}
              style={styles.logoImage}
              resizeMode="contain"
            />
          </View>
          <Text style={styles.step}>01 / 01</Text>
        </View>
        <View style={styles.progress}><View style={styles.progressActive} /><View style={styles.progressMuted} /><View style={styles.progressMuted} /></View>
        <View style={styles.heroIcon}><Sparkles size={26} color={theme.colors.accent} /></View>
        <Text style={styles.eyebrow}>HOŞ GELDİN {user?.phoneNumber ? '🌱' : ''}</Text>
        <Text style={styles.title}>Sana nasıl{`\n`}seslenelim?</Text>
        <Text style={styles.description}>Yeniden topluluğunda seni tanıyabilmemiz için küçük bir profil oluşturalım.</Text>

        <Text style={styles.label}>Görünen adın</Text>
        <View style={[styles.inputWrap, isFocused && styles.inputWrapFocused]}><TextInput value={name} onChangeText={setName} placeholder="Örn. Doğa Dostu" placeholderTextColor={theme.colors.inkFaint} style={styles.input} maxLength={80} autoCapitalize="words" returnKeyType="done" onFocus={() => setIsFocused(true)} onBlur={() => setIsFocused(false)} onSubmitEditing={submit} /><Text style={styles.counter}>{name.length}/80</Text></View>
        <Text style={[styles.helper, name.length > 0 && !nameIsValid && styles.helperError]}>{name.length > 0 && !nameIsValid ? 'En az 2 karakter kullanmalısın.' : 'Bu ad diğer kullanıcıların profilinde görünür.'}</Text>

        <Text style={styles.label}>Avatarını seç</Text>
        <View style={styles.avatarRow}>
          {AVATARS.map(avatar => {
            const selected = avatar.key === avatarKey;
            return <Pressable key={avatar.key} onPress={() => setAvatarKey(avatar.key)} style={styles.avatarOption}><View style={[styles.avatar, selected && styles.avatarSelected]}><Text style={styles.avatarEmoji}>{avatar.emoji}</Text>{selected && <View style={styles.check}><Check size={12} color={theme.colors.paper} strokeWidth={3} /></View>}</View><Text style={[styles.avatarLabel, selected && styles.avatarLabelSelected]}>{avatar.label}</Text></Pressable>;
          })}
        </View>

        <View style={styles.benefits}><View style={styles.benefit}><Text style={styles.benefitIcon}>✦</Text><Text style={styles.benefitText}>Yerel topluluk</Text></View><View style={styles.benefit}><Text style={styles.benefitIcon}>↗</Text><Text style={styles.benefitText}>Paylaş & kazan</Text></View><View style={styles.benefit}><Text style={styles.benefitIcon}>♡</Text><Text style={styles.benefitText}>İyilik bırak</Text></View></View>

        <View style={styles.trustCard}><ShieldCheck size={20} color={theme.colors.primary} /><View style={styles.trustCopy}><Text style={styles.trustTitle}>Güvenli başlangıç</Text><Text style={styles.trustText}>Telefonun doğrulandı. Profil bilgilerin istediğin zaman güncellenebilir.</Text></View></View>
        {authError && <Text style={styles.error}>{authError}</Text>}
        <Pressable onPress={submit} disabled={name.trim().length < 2 || isSaving} style={[styles.button, (name.trim().length < 2 || isSaving) && styles.buttonDisabled]}>{isSaving ? <ActivityIndicator color={theme.colors.ink} /> : <><Text style={styles.buttonText}>Topluluğa katıl</Text><ArrowRight size={19} color={theme.colors.ink} /></>}</Pressable>
        <Text style={styles.footnote}>Bu bilgiler yalnızca profilinde görünen addır.</Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: theme.colors.authBg }, content: { flexGrow: 1, padding: 28, paddingTop: 48 },
  topRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }, logo: { width: 38, height: 38, borderRadius: 10, overflow: 'hidden', alignItems: 'center', justifyContent: 'center' }, logoImage: { width: 38, height: 38, borderRadius: 10 }, step: { color: 'rgba(248,250,246,0.45)', fontSize: 12, letterSpacing: 1 },
  progress: { flexDirection: 'row', gap: 6, marginTop: 34 }, progressActive: { width: 30, height: 4, borderRadius: 4, backgroundColor: theme.colors.accent }, progressMuted: { width: 8, height: 4, borderRadius: 4, backgroundColor: 'rgba(248,250,246,0.2)' }, heroIcon: { marginTop: 28, marginBottom: 22, width: 54, height: 54, borderRadius: 18, backgroundColor: 'rgba(231,180,74,0.14)', alignItems: 'center', justifyContent: 'center' }, eyebrow: { color: theme.colors.accent, fontSize: 12, fontWeight: '800', letterSpacing: 1.6 }, title: { color: theme.colors.paper, fontFamily: 'Georgia', fontSize: 43, lineHeight: 46, marginTop: 10 }, description: { color: 'rgba(248,250,246,0.62)', fontSize: 15, lineHeight: 23, marginTop: 16, maxWidth: 330 },
  label: { color: 'rgba(248,250,246,0.78)', fontSize: 13, fontWeight: '700', marginTop: 30, marginBottom: 9 }, inputWrap: { height: 56, borderRadius: 16, backgroundColor: 'rgba(248,250,246,0.1)', borderWidth: 1, borderColor: 'rgba(248,250,246,0.18)', flexDirection: 'row', alignItems: 'center' }, inputWrapFocused: { borderColor: theme.colors.accent, backgroundColor: 'rgba(248,250,246,0.14)' }, input: { flex: 1, height: 56, borderRadius: 16, paddingHorizontal: 18, color: theme.colors.paper, fontSize: 16 }, counter: { color: 'rgba(248,250,246,0.35)', fontSize: 11, marginRight: 15 }, helper: { color: 'rgba(248,250,246,0.42)', fontSize: 11, marginTop: 7 }, helperError: { color: '#ffb4ab' },
  avatarRow: { flexDirection: 'row', justifyContent: 'space-between', paddingRight: 18 }, avatarOption: { alignItems: 'center', gap: 6 }, avatar: { width: 60, height: 60, borderRadius: 22, backgroundColor: 'rgba(248,250,246,0.1)', alignItems: 'center', justifyContent: 'center', borderWidth: 2, borderColor: 'transparent' }, avatarSelected: { borderColor: theme.colors.accent, backgroundColor: 'rgba(231,180,74,0.15)' }, avatarEmoji: { fontSize: 29 }, avatarLabel: { color: 'rgba(248,250,246,0.45)', fontSize: 11 }, avatarLabelSelected: { color: theme.colors.accent, fontWeight: '700' }, check: { position: 'absolute', right: -3, top: -4, width: 19, height: 19, borderRadius: 10, backgroundColor: theme.colors.primary, alignItems: 'center', justifyContent: 'center' },
  benefits: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 24, paddingVertical: 12, borderTopWidth: 1, borderBottomWidth: 1, borderColor: 'rgba(248,250,246,0.1)' }, benefit: { flexDirection: 'row', alignItems: 'center', gap: 5 }, benefitIcon: { color: theme.colors.accent, fontSize: 14 }, benefitText: { color: 'rgba(248,250,246,0.5)', fontSize: 10 }, trustCard: { flexDirection: 'row', gap: 12, padding: 15, borderRadius: 16, backgroundColor: 'rgba(142,181,151,0.12)', marginTop: 24 }, trustCopy: { flex: 1 }, trustTitle: { color: theme.colors.paper, fontWeight: '700', fontSize: 13 }, trustText: { color: 'rgba(248,250,246,0.58)', fontSize: 12, lineHeight: 17, marginTop: 3 }, error: { color: '#ffb4ab', fontSize: 13, marginTop: 14 }, button: { height: 58, borderRadius: 17, backgroundColor: theme.colors.accent, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 10, marginTop: 24 }, buttonDisabled: { opacity: 0.45 }, buttonText: { color: theme.colors.ink, fontSize: 16, fontWeight: '800' }, footnote: { color: 'rgba(248,250,246,0.38)', textAlign: 'center', fontSize: 11, marginTop: 17 },
});
