import React, { useState, createContext, useContext } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Home, Compass, Repeat, User as UserIcon, Plus } from 'lucide-react-native';
import { useAppState } from '../context/AppStateContext';
import { theme } from '../theme/theme';

// Screens
import { AuthScreen } from '../screens/auth/AuthScreens';
import { OnboardingScreen } from '../screens/auth/OnboardingScreen';
import { HomeScreen } from '../screens/home/HomeScreen';
import { MapScreen } from '../screens/map/MapScreens';
import { ProfileScreen } from '../screens/profile/ProfileScreen';
import { TransactionsScreen } from '../screens/transactions/TransactionsScreen';
import { AgreementDetailScreen } from '../screens/transactions/AgreementDetailScreen';
import { PostAdFlow } from '../screens/post/PostAdScreens';

// ---------------------------------------------------------------------------
// Tab Context — allows child screens to switch tabs without prop drilling
// ---------------------------------------------------------------------------
export type AppTab = 'home' | 'map' | 'transactions' | 'profile';

interface TabContextType {
  activeTab: AppTab;
  setActiveTab: (tab: AppTab) => void;
  openAgreement: (transactionId: string) => void;
  closeAgreement: () => void;
}

export const TabContext = createContext<TabContextType>({
  activeTab: 'home',
  setActiveTab: () => {},
  openAgreement: () => {},
  closeAgreement: () => {},
});

export const useTab = () => useContext(TabContext);

// ---------------------------------------------------------------------------
// Custom Tab Bar with Elevated Center FAB
// ---------------------------------------------------------------------------
interface TabBarProps {
  activeTab: AppTab;
  onTabPress: (tab: AppTab) => void;
  onPostPress: () => void;
}

const TabBar: React.FC<TabBarProps> = ({ activeTab, onTabPress, onPostPress }) => {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.tabBar, { paddingBottom: Math.max(insets.bottom, 8) }]}>
      {/* Home */}
      <TouchableOpacity
        style={styles.tabItem}
        onPress={() => onTabPress('home')}
        activeOpacity={0.7}
      >
        <Home color={activeTab === 'home' ? theme.colors.primary : theme.colors.inkFaint} size={22} />
        <Text style={[styles.tabLabel, activeTab === 'home' && styles.tabLabelActive]}>Ana Ekran</Text>
      </TouchableOpacity>

      {/* Map / Keşfet */}
      <TouchableOpacity
        style={styles.tabItem}
        onPress={() => onTabPress('map')}
        activeOpacity={0.7}
      >
        <Compass color={activeTab === 'map' ? theme.colors.primary : theme.colors.inkFaint} size={22} />
        <Text style={[styles.tabLabel, activeTab === 'map' && styles.tabLabelActive]}>Keşfet</Text>
      </TouchableOpacity>

      {/* CENTER FAB: İlan Paylaş */}
      <TouchableOpacity
        style={styles.centerFabWrap}
        onPress={onPostPress}
        activeOpacity={0.88}
      >
        <View style={styles.centerFab}>
          <Plus size={24} color={theme.colors.paper} strokeWidth={2.8} />
        </View>
        <Text style={styles.centerFabLabel}>İlan Ver</Text>
      </TouchableOpacity>

      {/* Transactions */}
      <TouchableOpacity
        style={styles.tabItem}
        onPress={() => onTabPress('transactions')}
        activeOpacity={0.7}
      >
        <Repeat color={activeTab === 'transactions' ? theme.colors.primary : theme.colors.inkFaint} size={22} />
        <Text style={[styles.tabLabel, activeTab === 'transactions' && styles.tabLabelActive]}>İşlemler</Text>
      </TouchableOpacity>

      {/* Profile */}
      <TouchableOpacity
        style={styles.tabItem}
        onPress={() => onTabPress('profile')}
        activeOpacity={0.7}
      >
        <UserIcon color={activeTab === 'profile' ? theme.colors.primary : theme.colors.inkFaint} size={22} />
        <Text style={[styles.tabLabel, activeTab === 'profile' && styles.tabLabelActive]}>Profil</Text>
      </TouchableOpacity>
    </View>
  );
};

// ---------------------------------------------------------------------------
// Main Screen — hosts custom tab navigation
// ---------------------------------------------------------------------------
const MainScreen = ({ navigation }: any) => {
  const [activeTab, setActiveTab] = useState<AppTab>('home');
  const [agreementId, setAgreementId] = useState<string | null>(null);

  const openAgreement = (transactionId: string) => {
    setActiveTab('transactions');
    setAgreementId(transactionId);
  };

  const closeAgreement = () => setAgreementId(null);

  const renderTab = () => {
    if (agreementId) {
      return <AgreementDetailScreen transactionId={agreementId} onBack={closeAgreement} />;
    }
    switch (activeTab) {
      case 'home':
        return <HomeScreen navigation={navigation} />;
      case 'map':
        return <MapScreen navigation={navigation} />;
      case 'transactions':
        return <TransactionsScreen navigation={navigation} />;
      case 'profile':
        return <ProfileScreen navigation={navigation} />;
    }
  };

  return (
    <TabContext.Provider value={{ activeTab, setActiveTab, openAgreement, closeAgreement }}>
      <View style={styles.mainContainer}>
        <View style={styles.tabContent}>{renderTab()}</View>
        <TabBar
          activeTab={activeTab}
          onTabPress={tab => { closeAgreement(); setActiveTab(tab); }}
          onPostPress={() => navigation.navigate('PostAdFlow', { mode: 'give' })}
        />
      </View>
    </TabContext.Provider>
  );
};

// ---------------------------------------------------------------------------
// Root Stack Navigator
// ---------------------------------------------------------------------------
const Stack = createNativeStackNavigator();

export const AppNavigator = () => {
  const { isAuthenticated, onboardingRequired, isBootstrapping } = useAppState();

  if (isBootstrapping) {
    return <View style={styles.bootstrap}><Text style={styles.bootstrapText}>Yükleniyor…</Text></View>;
  }

  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: theme.colors.bg },
        animation: Platform.OS === 'android' ? 'fade_from_bottom' : 'default',
      }}
    >
      {!isAuthenticated && (
        <Stack.Screen name="Auth" component={AuthScreen} />
      )}
      {isAuthenticated && onboardingRequired && (
        <Stack.Screen name="Onboarding" component={OnboardingScreen} />
      )}
      {isAuthenticated && !onboardingRequired && (
        <Stack.Screen name="Main" component={MainScreen} />
      )}
      {isAuthenticated && !onboardingRequired && (
        <Stack.Screen
          name="PostAdFlow"
          component={PostAdFlow}
          options={{ presentation: 'fullScreenModal' }}
        />
      )}
    </Stack.Navigator>
  );
};

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------
const styles = StyleSheet.create({
  bootstrap: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: theme.colors.bg },
  bootstrapText: { color: theme.colors.ink, fontSize: 16 },
  mainContainer: {
    flex: 1,
    backgroundColor: theme.colors.bg,
  },
  tabContent: {
    flex: 1,
  },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: theme.colors.surface,
    borderTopWidth: 1,
    borderTopColor: theme.colors.line,
    paddingTop: 8,
    alignItems: 'flex-end',
  },
  tabItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 3,
    paddingBottom: 2,
  },
  tabLabel: {
    fontSize: 10.5,
    color: theme.colors.inkFaint,
    fontWeight: '500',
    letterSpacing: 0.1,
  },
  tabLabelActive: {
    color: theme.colors.primary,
    fontWeight: '700',
  },

  // Elevated Center FAB
  centerFabWrap: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: -22,
  },
  centerFab: {
    width: 48,
    height: 48,
    borderRadius: theme.radius.full,
    backgroundColor: theme.colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    borderColor: theme.colors.surface,
    shadowColor: theme.colors.accent,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.35,
    shadowRadius: 6,
    elevation: 6,
  },
  centerFabLabel: {
    fontSize: 10.5,
    fontWeight: '700',
    color: theme.colors.primary,
    marginTop: 2,
  },
});
