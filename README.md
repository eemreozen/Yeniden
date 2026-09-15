# 🌿 YENİDEN — Akıllı Sıfır Atık & Döngüsel Ekonomi Platformu

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-green.svg)](https://spring.io/projects/spring-boot)
[![React Native](https://img.shields.io/badge/React%20Native-0.74-61DAFB.svg)](https://reactnative.dev/)
[![Expo](https://img.shields.io/badge/Expo-51-black.svg)](https://expo.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue.svg)](https://www.typescriptlang.org/)
[![Docker Compose](https://img.shields.io/badge/Docker%20Compose-10%20Containers-blue.svg)](https://www.docker.com/)

**YENİDEN**, şehir sakinlerinin evlerindeki atık, koli, cam, ahşap veya kullanmadıkları malzemeleri mahalle ölçeğinde paylaşmalarını, geri dönüşüme kazandırmalarını ve bu katkıları karşılığında dijital ödüller (**EcoCoin**, **XP**, **Rozetler**) kazanmalarını sağlayan uçtan uca akıllı bir döngüsel ekonomi platformudur.

Bu depo, platformun **mikroservis mimarisine sahip backend sistemini** ve **React Native (Expo) mobil uygulamasını** tek bir monorepo altında birleştirmektedir.

---

## 📁 Depo Yapısı (Monorepo Architecture)

```text
Yeniden/
├── backend/                       # Java 21 & Spring Boot Mikroservisleri
│   ├── api-gateway/               # [Port 8080] Tüm dış isteklerin yönlendirildiği Ters Proxy
│   ├── identity-service/          # [Port 8081] Kullanıcı kaydı, OTP doğrulama & Güven Skoru
│   ├── catalog-service/           # [Port 8082] Atık ilanları, kategoriler & PostGIS yakınlık sorguları
│   ├── exchange-service/          # [Port 8083] İlan rezervasyonu & 6 haneli güvenli teslimat kodu doğrulama
│   ├── ecocoin-service/           # [Port 8084] Çift Kayıtlı Defter (Double-Entry Ledger) & EcoCoin cüzdanı
│   ├── gamification-service/      # [Port 8085] XP, Seviye atlama formülü & Rozet kazanım motoru
│   ├── wasteai-service/           # [Port 8086] Yapay zeka ile görsel atık sınıflandırma servisi
│   ├── yeniden-common/            # Ortak DTO, Event ve yardımcı kütüphaneler
│   ├── docker/                    # PostgreSQL init scriptleri ve Docker yapılandırmaları
│   ├── docker-compose.yml         # 10 konteynırlı tam orkestrasyon dosyası
│   └── pom.xml                    # Maven parent konfigürasyonu
│
├── mobile/                        # React Native / Expo Mobil Uygulaması
│   ├── src/
│   │   ├── screens/               # Auth, Onboarding, Home, İlan Verme, AI Tarama, Teslimat, Profil
│   │   ├── context/               # Global state (AppStateContext, kullanıcı ve ilan durumu)
│   │   ├── services/              # Mikroservis API istemcileri (Gateway entegrasyonu)
│   │   └── theme/                 # Renk paleti, tipografi ve tasarım sistemi
│   ├── android/                   # Android native proje dosyaları
│   ├── ios/                       # iOS native proje dosyaları
│   └── package.json               # Mobil bağımlılıklar ve scriptler
│
├── docs/                          # Detaylı mimari, ER diyagramları ve API sözleşmeleri
└── README.md                      # Platform ana dokümantasyonu (bu dosya)
```

---

## ⚡ Hızlı Başlangıç (Quick Start)

### 1. Backend Mikroservislerini Çalıştırma

Gereksinimler: **Java 21**, **Docker** ve **Docker Compose** (Docker VM için en az 4 GB RAM önerilir).

```bash
# 1. Backend dizinine geçin
cd backend

# 2. Çalıştırılabilir JAR'ları derleyin
./mvnw -DskipTests package

# 3. Güvenli OTP HMAC anahtarı oluşturun (ortam değişkeni olarak atanmalıdır)
export OTP_HMAC_SECRET="$(openssl rand -hex 32)"

# 4. Tüm altyapıyı (PostgreSQL PostGIS, Redis 7, RabbitMQ ve 7 Mikroservis) başlatın
docker compose up --build -d

# 5. Servislerin durumunu kontrol edin
docker compose ps
```

Birim (unit) testlerini çalıştırmak için:
```bash
cd backend
./mvnw test
```

### 2. Mobil Uygulamayı Çalıştırma

Gereksinimler: **Node.js (v18+)**, **npm** veya **yarn**, **Expo Go** (fiziksel cihaz için) veya **Android Studio / Xcode Emulator**.

```bash
# 1. Mobil dizinine geçin
cd mobile

# 2. Bağımlılıkları yükleyin
npm install

# 3. Expo geliştirici sunucusunu başlatın
npx expo start
```

- Metro arayüzünden `a` tuşuna basarak **Android Emulator**'de, `i` tuşuna basarak **iOS Simulator**'de veya Expo Go uygulamasıyla QR kodu okutarak **fiziksel cihazınızda** test edebilirsiniz.

---

## 🔌 Mikroservisler ve Ağ Port Haritası

Tüm mobil ve web istemcisi istekleri **API Gateway (Port 8080)** üzerinden yönlendirilir:

| Servis Adı | Port | Açıklama |
| :--- | :---: | :--- |
| 🚪 **`api-gateway`** | **8080** | Tek giriş noktası (Reverse Proxy). İstemci isteklerini mikroservislere yönlendirir. |
| 👤 **`identity-service`** | **8081** | Kullanıcı kaydı/girişi, SMS/Redis OTP doğrulama, Kullanıcı Güven Puanı. |
| 📦 **`catalog-service`** | **8082** | Atık/malzeme ilanları, kategoriler, PostGIS coğrafi yarıçap sorguları. |
| 🤝 **`exchange-service`** | **8083** | Rezervasyon talepleri, 6 haneli teslimat kodları, RabbitMQ `HandoverConfirmedEvent`. |
| 💰 **`ecocoin-service`** | **8084** | Çift Kayıtlı Defter cüzdanı, işlem geçmişi, günlük 100 coin tavan kontrolü. |
| 🏆 **`gamification-service`** | **8085** | Kullanıcı XP takibi, seviye atlama formülü (`(XP/100)+1`), dinamik rozetler. |
| 🤖 **`wasteai-service`** | **8086** | Fotoğraftan malzeme tanıma, durum tespiti ve kategori eşleme. |

### Altyapı Bileşenleri
- **PostgreSQL 16 + PostGIS:** `localhost:5432` (`yeniden_db` veritabanı, 5 yalıtılmış şema)
- **Redis 7 Alpine:** `localhost:6379` (OTP doğrulama & TTL önbelleği)
- **RabbitMQ 3 Management:** `localhost:5672` (AMQP Broker) / `localhost:15672` (Yönetim Paneli)

---

## 📱 Mobil Uygulama Özellikleri

- **Modern & Akıcı Arayüz:** Doğal renk paleti, koyu yeşil ve altın sarısı aksanlar ile çevre dostu tasarım.
- **AI Destekli Akıllı Tarama:** Kameradan çekilen ürün fotoğrafını anında analiz eder, malzeme türünü (Cam, Koli, Ahşap vb.) ve durumunu belirleyip ilan formunu otomatik doldurur.
- **Konum Bazlı İlan Keşfi:** Yakındaki atıkları ve ihtiyaçları mahalle/mesafe bazında listeleme ve harita görünümü.
- **Çift Taraflı Güvenli Teslimat:** Fiziksel buluşmada üretilen 6 haneli tek kullanımlık teslimat kodu ile doğrulama.
- **Oyunlaştırma & Cüzdan:** Kazanılan EcoCoin bakiyesi, işlem dökümleri, kazanılan başarı rozetleri ve seviye ilerleme çubuğu.

---

## 🏗️ Temel Mimari Prensipler

1. **Şema İzolasyonu:** Her mikroservis PostgreSQL üzerinde kendi bağımsız şemasına (`identity`, `catalog`, `exchange`, `ecocoin`, `gamification`) sahiptir.
2. **Olay Güdümlü Mimari (AMQP):** Teslimat başarıyla tamamlandığında `exchange-service` RabbitMQ'ya olay bırakır; `ecocoin-service` cüzdana para aktarırken `gamification-service` eşzamanlı olarak XP ve rozet günceller.
3. **Çift Kayıtlı Defter (Double-Entry Ledger):** Bakiye manipülasyonunu engellemek için tüm EcoCoin hareketleri Borç (-) ve Alacak (+) kayıtları olarak tutulur; `Net Toplam = 0` matematiksel garantisi sağlanır.
4. **Dayanıklı İstemci Mimarisi:** Mobil uygulama çevrimdışı (offline) ve backend hazır olmadığında dahi kesintisiz çalışabilecek hibrit mock/canlı servis altyapısıyla tasarlanmıştır.

---

## 📚 Detaylı Dokümantasyon

Mimari kararlar, veri modelleri ve akış diyagramları için `docs/` dizinini inceleyebilirsiniz:

- 🚀 [**Geliştirici Kılavuzu (PROJECT_GUIDE.md)**](docs/PROJECT_GUIDE.md)
- 🗄️ [**Veritabanı Mimarisi & ER Şeması (DATABASE_ARCHITECTURE.md)**](docs/DATABASE_ARCHITECTURE.md)
- 🗺️ [**Mimari Kararlar ve Doküman Haritası (00-overview.md)**](docs/00-overview.md)
- 📋 [**Mobil Entegrasyon Planı (mobile/INTEGRATION_PLAN.md)**](mobile/INTEGRATION_PLAN.md)
