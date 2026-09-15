import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  StatusBar,
  RefreshControl,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { CheckCircle2, RefreshCw } from 'lucide-react-native';
import { theme } from '../../theme/theme';
import { useAppState, Transaction } from '../../context/AppStateContext';
import { useTab } from '../../navigation/AppNavigator';

const STATUS_LABELS: Record<string, string> = {
  REQUESTED: 'Bekliyor',
  AGREED: 'Anlaşıldı',
  DELIVERED: 'Tamamlandı',
  CANCELLED: 'İptal',
};

const StatusPill: React.FC<{ status: Transaction['status'] }> = ({ status }) => {
  const styles = pillStyles[status] ?? pillStyles.REQUESTED;
  return (
    <View style={styles.wrap}>
      <Text style={styles.text}>{STATUS_LABELS[status] ?? status}</Text>
    </View>
  );
};

const TransactionSeparator = () => <View style={styles.transactionSeparator} />;

const pillStyles: Record<string, any> = {
  AGREED: StyleSheet.create({
    wrap: { backgroundColor: '#F1E3C4', borderRadius: 999, paddingHorizontal: 10, paddingVertical: 3, borderWidth: 1, borderColor: '#B98A2E' },
    text: { fontSize: 10, fontWeight: '700', color: '#7A5A16' },
  }),
  REQUESTED: StyleSheet.create({
    wrap: { backgroundColor: '#DCE7DC', borderRadius: 999, paddingHorizontal: 10, paddingVertical: 3, borderWidth: 1, borderColor: '#1E3A2B' },
    text: { fontSize: 10, fontWeight: '700', color: '#1E3A2B' },
  }),
  DELIVERED: StyleSheet.create({
    wrap: { backgroundColor: '#E7ECE0', borderRadius: 999, paddingHorizontal: 10, paddingVertical: 3, borderWidth: 1, borderColor: 'rgba(22,36,27,0.16)' },
    text: { fontSize: 10, fontWeight: '700', color: '#8A9A8C' },
  }),
  CANCELLED: StyleSheet.create({
    wrap: { backgroundColor: 'rgba(198,40,40,0.08)', borderRadius: 999, paddingHorizontal: 10, paddingVertical: 3, borderWidth: 1, borderColor: 'rgba(198,40,40,0.2)' },
    text: { fontSize: 10, fontWeight: '700', color: '#C62828' },
  }),
};

// ---------------------------------------------------------------------------
// TransactionCard
// ---------------------------------------------------------------------------
const TxCard: React.FC<{
  item: Transaction;
  userId: string | undefined;
  onOpenDetail: (tx: Transaction) => void;
  onAccept: (tx: Transaction) => void;
  isAccepting: boolean;
}> = ({ item, userId, onOpenDetail, onAccept, isAccepting }) => {
  const isDonor = item.donorId === userId;
  const canAccept = isDonor && item.status === 'REQUESTED';
  const canOpenDetail = item.status === 'AGREED' || item.status === 'DELIVERED';

  return (
    <View style={styles.txCard}>
      <View style={styles.txCardHeader}>
        <Text style={styles.txCardTitle} numberOfLines={2}>{item.adTitle}</Text>
        <StatusPill status={item.status} />
      </View>
      <Text style={styles.txMeta}>
        {isDonor ? `Talep eden: ${item.receiverName}` : `Donör: ${item.donorName}`}
      </Text>

      <View style={styles.txActions}>
        {canAccept && (
          <TouchableOpacity
            style={styles.txPrimaryBtn}
            onPress={() => onAccept(item)}
            activeOpacity={0.8}
            disabled={isAccepting}
          >
            {isAccepting
              ? <ActivityIndicator size="small" color={theme.colors.paper} />
              : <Text style={styles.txPrimaryBtnText}>Talebi Kabul Et</Text>}
          </TouchableOpacity>
        )}
        {canOpenDetail && (
          <TouchableOpacity style={styles.txGhostBtn} onPress={() => onOpenDetail(item)} activeOpacity={0.7}>
            <CheckCircle2 size={15} color={theme.colors.primary} />
            <Text style={styles.txGhostBtnText}>Anlaşma Detayı</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
};

// ---------------------------------------------------------------------------
// TransactionsScreen
// ---------------------------------------------------------------------------
export const TransactionsScreen: React.FC<{ navigation: any }> = () => {
  const insets = useSafeAreaInsets();
  const {
    user,
    transactions,
    transactionsLoading,
    transactionsError,
    refreshTransactions,
    acceptExchangeRequest,
  } = useAppState();
  const { openAgreement } = useTab();
  const [tab, setTab] = useState<'aktif' | 'gecmis'>('aktif');
  const [acceptingId, setAcceptingId] = useState<string | null>(null);

  const active = transactions.filter(t => t.status === 'REQUESTED' || t.status === 'AGREED');
  const past = transactions.filter(t => t.status === 'DELIVERED' || t.status === 'CANCELLED');
  const list = tab === 'aktif' ? active : past;

  const handleAccept = async (transaction: Transaction) => {
    setAcceptingId(transaction.id);
    try {
      await acceptExchangeRequest(transaction.id);
      openAgreement(transaction.id);
    } catch (error) {
      Alert.alert('Talep kabul edilemedi', error instanceof Error ? error.message : 'Lütfen tekrar deneyin.');
    } finally {
      setAcceptingId(null);
    }
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <StatusBar barStyle="dark-content" backgroundColor={theme.colors.bg} />

      {/* Top bar */}
      <View style={styles.topBar}>
        <Text style={styles.topBarTitle}>İşlemler</Text>
      </View>

      {/* Tabs */}
      <View style={styles.tabs}>
        <TouchableOpacity
          style={[styles.tab, tab === 'aktif' && styles.tabActive]}
          onPress={() => setTab('aktif')}
          activeOpacity={0.7}
        >
          <Text style={[styles.tabText, tab === 'aktif' && styles.tabTextActive]}>
            Aktif {active.length > 0 ? `(${active.length})` : ''}
          </Text>
          {tab === 'aktif' && <View style={styles.tabIndicator} />}
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, tab === 'gecmis' && styles.tabActive]}
          onPress={() => setTab('gecmis')}
          activeOpacity={0.7}
        >
          <Text style={[styles.tabText, tab === 'gecmis' && styles.tabTextActive]}>
            Geçmiş
          </Text>
          {tab === 'gecmis' && <View style={styles.tabIndicator} />}
        </TouchableOpacity>
      </View>

      {/* List */}
      <FlatList
        data={list}
        keyExtractor={i => i.id}
        renderItem={({ item }) => (
          <TxCard
            item={item}
            userId={user?.id}
            onOpenDetail={tx => openAgreement(tx.id)}
            onAccept={handleAccept}
            isAccepting={acceptingId === item.id}
          />
        )}
        refreshControl={<RefreshControl refreshing={transactionsLoading} onRefresh={refreshTransactions} tintColor={theme.colors.primary} />}
        contentContainerStyle={[
          styles.listContent,
          { paddingBottom: insets.bottom + 16 },
          list.length === 0 && styles.listEmpty,
        ]}
        ListEmptyComponent={transactionsLoading ? (
          <View style={styles.stateBox}><ActivityIndicator color={theme.colors.primary} /><Text style={styles.emptyText}>İşlemler yükleniyor…</Text></View>
        ) : transactionsError ? (
          <View style={styles.stateBox}>
            <Text style={styles.errorText}>{transactionsError}</Text>
            <TouchableOpacity style={styles.retryButton} onPress={refreshTransactions}><RefreshCw size={16} color={theme.colors.paper} /><Text style={styles.retryText}>Tekrar Dene</Text></TouchableOpacity>
          </View>
        ) : (
          <View style={styles.stateBox}><CheckCircle2 size={28} color={theme.colors.inkFaint} /><Text style={styles.emptyText}>{tab === 'aktif' ? 'Henüz aktif bir değişim talebiniz yok.' : 'Tamamlanmış işlem bulunmuyor.'}</Text></View>
        )}
        ItemSeparatorComponent={TransactionSeparator}
        showsVerticalScrollIndicator={false}
      />

    </View>
  );
};

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------
const styles = StyleSheet.create({
  transactionSeparator: { height: 10 },
  root: {
    flex: 1,
    backgroundColor: theme.colors.bg,
  },
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

  // Tabs
  tabs: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.line,
  },
  tab: {
    flex: 1,
    height: 46,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  tabActive: {},
  tabText: {
    fontSize: theme.font.size.sm,
    fontWeight: '600',
    color: theme.colors.inkFaint,
  },
  tabTextActive: {
    color: theme.colors.ink,
  },
  tabIndicator: {
    position: 'absolute',
    bottom: -1,
    left: '20%',
    right: '20%',
    height: 2,
    backgroundColor: theme.colors.primary,
    borderRadius: theme.radius.full,
  },

  // List
  listContent: {
    padding: 16,
    paddingHorizontal: 22,
  },
  listEmpty: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: theme.font.size.sm,
    color: theme.colors.inkFaint,
    textAlign: 'center',
    paddingVertical: 40,
  },
  stateBox: { alignItems: 'center', gap: 10, paddingHorizontal: 24 },
  errorText: { color: '#B42318', fontSize: theme.font.size.sm, textAlign: 'center' },
  retryButton: { minHeight: 40, borderRadius: 12, backgroundColor: theme.colors.primary, paddingHorizontal: 16, flexDirection: 'row', alignItems: 'center', gap: 7 },
  retryText: { color: theme.colors.paper, fontSize: theme.font.size.xs, fontWeight: '700' },

  // Transaction card
  txCard: {
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.line,
    borderRadius: 16,
    padding: 14,
  },
  txCardHeader: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    marginBottom: 8,
    gap: 8,
  },
  txCardTitle: {
    flex: 1,
    fontSize: theme.font.size.base,
    fontWeight: '600',
    color: theme.colors.ink,
    lineHeight: 20,
  },
  txMeta: {
    fontSize: theme.font.size.xs,
    color: theme.colors.inkFaint,
    marginBottom: 10,
  },
  txActions: {
    flexDirection: 'row',
    gap: 8,
  },
  txPrimaryBtn: {
    flex: 1.3,
    backgroundColor: theme.colors.primary,
    borderRadius: 12,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  txPrimaryBtnText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: theme.colors.paper,
  },
  txGhostBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: theme.colors.surfaceAlt,
    borderRadius: 12,
    height: 40,
  },
  txGhostBtnText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: theme.colors.primary,
  },

});
