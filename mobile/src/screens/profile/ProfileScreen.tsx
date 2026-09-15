import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  StatusBar,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Sprout, Droplet, Package, Trophy, Award, Lock, LogOut } from 'lucide-react-native';
import { theme } from '../../theme/theme';
import { useAppState } from '../../context/AppStateContext';

const getBadgeIcon = (name: string, color: string) => {
  switch (name) {
    case 'sprout': return <Sprout size={20} color={color} />;
    case 'droplet': return <Droplet size={20} color={color} />;
    case 'package': return <Package size={20} color={color} />;
    case 'trophy': return <Trophy size={20} color={color} />;
    case 'award': return <Award size={20} color={color} />;
    default: return <Award size={20} color={color} />;
  }
};

export const ProfileScreen: React.FC<{ navigation: any }> = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { user, quests, coinEntries, logout, isLoadingProfileStats, profileStatsError, refreshProfileStats } = useAppState();

  const xpPct = Math.min(100, (user?.xp ?? 0));

  const BADGES = user?.badges ?? [];
  const ALL_BADGES = [
    ...BADGES,
    { id: 'locked1', title: 'Yeşil Kahraman', description: '50 kg CO2 tasarrufu', iconName: 'trophy', earnedAt: '' },
    { id: 'locked2', title: 'Usta Dağıtıcı', description: '20 işlem tamamla', iconName: 'award', earnedAt: '' },
  ];

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <StatusBar barStyle="dark-content" backgroundColor={theme.colors.bg} />

      {/* Top bar */}
      <View style={styles.topBar}>
        <Text style={styles.topBarTitle}>Profil</Text>
        <TouchableOpacity onPress={logout} style={styles.logoutLink} activeOpacity={0.7}>
          <LogOut size={16} color={theme.colors.danger} />
          <Text style={styles.logoutLinkText}>Çıkış</Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: insets.bottom + 24 }]}
      >
        {profileStatsError && (
          <View style={styles.statsErrorBanner}>
            {isLoadingProfileStats && <ActivityIndicator size="small" color={theme.colors.primary} />}
            <Text style={styles.statsErrorText}>{profileStatsError}</Text>
            <TouchableOpacity onPress={refreshProfileStats} disabled={isLoadingProfileStats}>
              <Text style={styles.statsRetryText}>Tekrar dene</Text>
            </TouchableOpacity>
          </View>
        )}
        {/* Avatar + name */}
        <View style={styles.profileHead}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {user?.name ? user.name.charAt(0).toUpperCase() : 'M'}
            </Text>
          </View>
          <Text style={styles.profileName}>{user?.name ?? 'Kullanıcı'}</Text>
          <View style={styles.levelTag}>
            <Text style={styles.levelTagText}>Seviye {user?.level ?? 1}</Text>
          </View>
        </View>

        {/* XP bar */}
        <View style={styles.xpSection}>
          <View style={styles.xpRow}>
            <Text style={styles.xpLabel}>Deneyim Puanı</Text>
            <Text style={styles.xpValue}>{user?.xp ?? 0} / 100 XP</Text>
          </View>
          <View style={styles.xpTrack}>
            <View style={[styles.xpFill, { width: `${xpPct}%` }]} />
          </View>
        </View>

        {/* Stats grid */}
        <View style={styles.statGrid}>
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{user?.ecoCoinBalance ?? 0}</Text>
            <Text style={styles.statLabel}>Eco-Coin bakiyesi</Text>
          </View>
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{user?.sharedCount ?? 0}</Text>
            <Text style={styles.statLabel}>Paylaşım sayısı</Text>
          </View>
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{user?.receivedCount ?? 0}</Text>
            <Text style={styles.statLabel}>Alınan malzeme</Text>
          </View>
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{BADGES.filter(b => b.earnedAt).length}</Text>
            <Text style={styles.statLabel}>Kazanılan rozet</Text>
          </View>
        </View>

        {/* Section header */}
        <View style={styles.sectionLabel}>
          <Text style={styles.sectionLabelText}>Rozetler</Text>
        </View>

        {/* Badge row */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.badgeRow} contentContainerStyle={{ gap: 10, paddingHorizontal: 22 }}>
          {ALL_BADGES.map(badge => {
            const locked = !badge.earnedAt;
            return (
              <View key={badge.id} style={[styles.badge, locked && styles.badgeLocked]}>
                <View style={styles.badgeIconWrap}>
                  {locked ? <Lock size={18} color={theme.colors.inkFaint} /> : getBadgeIcon(badge.iconName, theme.colors.primary)}
                </View>
              </View>
            );
          })}
        </ScrollView>

        {quests.length > 0 && (
          <View style={styles.questsSection}>
            <View style={styles.sectionLabel}>
              <Text style={styles.sectionLabelText}>Bu ayın görevleri</Text>
            </View>
            {quests.map(quest => (
              <View key={quest.id} style={styles.questRow}>
                <View style={styles.questCopy}>
                  <Text style={styles.questTitle}>{quest.title}</Text>
                  <Text style={styles.questMeta}>{quest.targetValue} hedef · +{quest.rewardCoins} Eco-Coin</Text>
                </View>
              </View>
            ))}
          </View>
        )}

        {coinEntries.length > 0 && (
          <View style={styles.ledgerSection}>
            <View style={styles.sectionLabel}>
              <Text style={styles.sectionLabelText}>Son Eco-Coin hareketleri</Text>
            </View>
            {coinEntries.map(entry => (
              <View key={entry.id} style={styles.ledgerRow}>
                <Text style={styles.ledgerDate}>{new Date(entry.createdAt).toLocaleDateString('tr-TR')}</Text>
                <Text style={[styles.ledgerAmount, entry.amount < 0 && styles.ledgerAmountNegative]}>
                  {entry.amount > 0 ? '+' : ''}{entry.amount}
                </Text>
              </View>
            ))}
          </View>
        )}

        {/* Sustainability card */}
        <View style={styles.sustainCard}>
          <View style={styles.sustainTop}>
            <Text style={styles.sustainTitle}>Sürdürülebilirlik etkisi</Text>
            <Text style={styles.sustainPeriod}>Bu ay</Text>
          </View>
          <View style={styles.sustainStats}>
            <View style={styles.sustainStat}>
              <Text style={styles.sustainStatVal}>
                {((user?.stats?.co2SavedKg ?? 0)).toFixed(1)} kg
              </Text>
              <Text style={styles.sustainStatLabel}>CO₂ tasarrufu</Text>
            </View>
            <View style={styles.sustainDivider} />
            <View style={styles.sustainStat}>
              <Text style={styles.sustainStatVal}>
                {user?.stats?.waterSavedLiters ?? 0} L
              </Text>
              <Text style={styles.sustainStatLabel}>Su korundu</Text>
            </View>
            <View style={styles.sustainDivider} />
            <View style={styles.sustainStat}>
              <Text style={styles.sustainStatVal}>
                {user?.stats?.landfillDivertedKg ?? 0} kg
              </Text>
              <Text style={styles.sustainStatLabel}>Atık önlendi</Text>
            </View>
          </View>
          <View style={styles.progressTrack}>
            <View style={[styles.progressFill, { width: '64%' }]} />
          </View>
          <Text style={styles.sustainFoot}>
            {user?.stats?.landfillDivertedKg ?? 0} kg malzeme yeniden dolaşıma girdi
          </Text>
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: theme.colors.bg,
  },
  statsErrorBanner: {
    marginHorizontal: 22,
    marginTop: 12,
    padding: 10,
    borderRadius: 12,
    backgroundColor: '#FFF4E5',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  statsErrorText: { flex: 1, color: '#8A4B08' },
  statsRetryText: { color: theme.colors.primary, fontWeight: '700' },
  questsSection: { marginTop: 8 },
  questRow: {
    marginHorizontal: 22,
    marginBottom: 8,
    padding: 12,
    borderRadius: 12,
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.line,
  },
  questCopy: { flex: 1 },
  questTitle: { color: theme.colors.ink, fontWeight: '700' },
  questMeta: { marginTop: 4, color: theme.colors.inkFaint, fontSize: 12 },
  ledgerSection: { marginTop: 8 },
  ledgerRow: {
    marginHorizontal: 22,
    marginBottom: 6,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 10,
    backgroundColor: theme.colors.surface,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  ledgerDate: { color: theme.colors.inkFaint, fontSize: 12 },
  ledgerAmount: { color: theme.colors.primary, fontWeight: '800' },
  ledgerAmountNegative: { color: theme.colors.danger },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 22,
    height: 48,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.line,
  },
  topBarTitle: {
    fontFamily: 'Georgia',
    fontSize: theme.font.size.xl,
    fontWeight: '500',
    color: theme.colors.ink,
    letterSpacing: -0.3,
  },
  badgeIconWrap: {
    width: 32,
    height: 32,
    borderRadius: theme.radius.full,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoutLink: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: theme.radius.full,
    backgroundColor: 'rgba(198,40,40,0.08)',
  },
  logoutLinkText: {
    fontSize: theme.font.size.xs,
    color: theme.colors.danger,
    fontWeight: '700',
  },

  scrollContent: {
    paddingBottom: 32,
  },

  profileHead: {
    alignItems: 'center',
    paddingVertical: 24,
    paddingHorizontal: 22,
  },
  avatar: {
    width: 64, height: 64, borderRadius: theme.radius.full,
    backgroundColor: theme.colors.primary,
    alignItems: 'center', justifyContent: 'center',
    marginBottom: 10,
  },
  avatarText: {
    fontFamily: 'Georgia',
    fontSize: 26, fontWeight: '500',
    color: theme.colors.paper,
  },
  profileName: {
    fontSize: theme.font.size.md,
    fontWeight: '600',
    color: theme.colors.ink,
    marginBottom: 6,
  },
  levelTag: {
    backgroundColor: theme.colors.primaryTint,
    borderRadius: theme.radius.full,
    paddingHorizontal: 12,
    paddingVertical: 4,
  },
  levelTagText: {
    fontSize: theme.font.size.xs,
    fontWeight: '700',
    color: theme.colors.primary,
  },

  // XP
  xpSection: { paddingHorizontal: 22, marginBottom: 20 },
  xpRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  xpLabel: { fontSize: theme.font.size.sm, color: theme.colors.inkSoft, fontWeight: '600' },
  xpValue: { fontSize: theme.font.size.sm, color: theme.colors.primary, fontWeight: '700' },
  xpTrack: {
    height: 6, backgroundColor: theme.colors.surfaceAlt,
    borderRadius: theme.radius.full, overflow: 'hidden',
  },
  xpFill: {
    height: '100%', backgroundColor: theme.colors.accent, borderRadius: theme.radius.full,
  },

  // Stats grid
  statGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    paddingHorizontal: 22,
    marginBottom: 20,
  },
  statCard: {
    width: (((require('react-native')).Dimensions.get('window').width) - 44 - 10) / 2,
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.line,
    borderRadius: 16,
    padding: 14,
  },
  statValue: {
    fontFamily: 'Georgia',
    fontSize: 22, fontWeight: '500',
    color: theme.colors.ink,
    marginBottom: 4,
  },
  statLabel: { fontSize: theme.font.size.xs, color: theme.colors.inkFaint },

  // Section label
  sectionLabel: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 22,
    paddingBottom: 12,
  },
  sectionLabelText: {
    fontSize: 12.5, fontWeight: '700',
    color: theme.colors.inkSoft,
    textTransform: 'uppercase', letterSpacing: 1,
  },

  // Badge row
  badgeRow: { marginBottom: 20 },
  badge: {
    width: 46, height: 46, borderRadius: 14,
    backgroundColor: theme.colors.accentTint,
    alignItems: 'center', justifyContent: 'center',
  },
  badgeLocked: { backgroundColor: theme.colors.surfaceAlt },
  badgeIcon: { fontSize: 20, color: theme.colors.accent },
  badgeIconLocked: { color: theme.colors.inkFaint },

  // Sustainability card
  sustainCard: {
    marginHorizontal: 22,
    backgroundColor: theme.colors.primary,
    borderRadius: 18,
    padding: 18,
  },
  sustainTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'baseline',
    marginBottom: 16,
  },
  sustainTitle: {
    fontFamily: 'Georgia',
    fontSize: 15, fontWeight: '500',
    color: theme.colors.paper,
  },
  sustainPeriod: {
    fontSize: 11, color: 'rgba(248,250,246,0.6)',
  },
  sustainStats: {
    flexDirection: 'row',
    marginBottom: 14,
  },
  sustainStat: { flex: 1, alignItems: 'center' },
  sustainStatVal: {
    fontSize: 15, fontWeight: '700',
    color: theme.colors.paper,
    marginBottom: 2,
  },
  sustainStatLabel: { fontSize: 10, color: 'rgba(248,250,246,0.6)' },
  sustainDivider: {
    width: 1, backgroundColor: 'rgba(248,250,246,0.15)', marginVertical: 4,
  },
  progressTrack: {
    height: 6, backgroundColor: 'rgba(248,250,246,0.18)',
    borderRadius: theme.radius.full, overflow: 'hidden', marginBottom: 8,
  },
  progressFill: {
    height: '100%', backgroundColor: theme.colors.accent, borderRadius: theme.radius.full,
  },
  sustainFoot: { fontSize: 10.5, color: 'rgba(248,250,246,0.55)' },
});
