# Mobil Ekranlar Bazında Backend API İstekleri Sözleşmesi

Bu doküman, GreenTrack mobil uygulamasındaki tüm ekranların (`auth`, `home`, `map`, `post`, `transactions`, `profile`, `rewards`, `recycling`) ihtiyaç duyduğu REST API uçlarını, istek parametrelerini, yanıt formatlarını ve ekrandaki tetiklenme senaryolarını detaylandırır.

**Taban URL:** `/api/v1`  
**Ortak Standartlar:** Tüm istek/yanıt gövdeleri JSON formatındadır. Kimlik doğrulama `Authorization: Bearer <accessToken>` başlığı ile yapılır. Hata gövdeleri RFC 7807 formatındadır.

---

## 1. Giriş ve Doğrulama Ekranı (`src/screens/auth/AuthScreens.tsx`)

Kullanıcının telefon numarası ile kayıt olduğu ve SMS OTP kodu ile giriş yaptığı ekrandır.

### `POST /auth/otp/request`
* **Açıklama:** Kullanıcının girdiği telefon numarasına 6 haneli SMS doğrulama kodu gönderir.
* **Tetiklenme Anı:** "Doğrulama kodu gönder" butonuna basıldığında.
* **İstek Gövdesi:**
  ```json
  { "phone": "+905551234567" }
  ```
* **Yanıt (200 OK):**
  ```json
  { "message": "OTP gönderildi", "expiresInSeconds": 180 }
  ```

### `POST /auth/otp/verify`
* **Açıklama:** Kullanıcının girdiği 6 haneli OTP kodunu doğrular, JWT access token ve rotating refresh token üretir.
* **Tetiklenme Anı:** 6 haneli kod girilip "Doğrula ve devam et" butonuna basıldığında.
* **İstek Gövdesi:**
  ```json
  { "phone": "+905551234567", "code": "482910" }
  ```
* **Yanıt (200 OK):**
  ```json
  {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "d8e8fca2-...",
    "expiresIn": 900,
    "isNewUser": false,
    "user": {
      "id": "c3a1b4e2-a6e2-47bb-b352-82c57a79f041",
      "phoneNumber": "+905551234567",
      "displayName": "Doğa Dostu",
      "neighborhoodId": "n_kadikoy"
    }
  }
  ```

### `POST /auth/refresh`
* **Açıklama:** 15 dakikalık access token süresi dolduğunda arka planda yeni token çifti alır.
* **Tetiklenme Anı:** API istemcisinde `401 Unauthorized` hatası alındığında interceptor tarafından otomatik çağrılır.
* **İstek Gövdesi:**
  ```json
  { "refreshToken": "d8e8fca2-..." }
  ```
* **Yanıt (200 OK):** Yeni `{ accessToken, refreshToken, expiresIn }`.

### `POST /auth/logout`
* **Açıklama:** Oturumu sonlandırır ve veritabanındaki refresh token'ı geçersiz kılar.
* **Tetiklenme Anı:** Profil ekranından "Çıkış Yap" tıklandığında.
* **Yanıt (204 No Content)**

---

## 2. Ana Sayfa & İlan Akışı (`src/screens/home/HomeScreen.tsx`)

Yakındaki ilanların listelendiği, arama/kategori filtrelerinin uygulandığı ve ilan detay modalının açıldığı ana akış ekranıdır.

### `GET /categories`
* **Açıklama:** Üst kısımdaki yatay kaydırılabilir kategori haplarını (Koli, Karton, Cam, Ahşap vb.) doldurur.
* **Tetiklenme Anı:** Ekran ilk yüklendiğinde.
* **Yanıt (200 OK):**
  ```json
  [
    { "id": "cat_koli", "code": "KOLI", "name": "Koli & Kutu", "reusable": true, "coinMultiplier": 1.0 },
    { "id": "cat_cam", "code": "CAM", "name": "Cam Şişe/Kavanoz", "reusable": true, "coinMultiplier": 1.2 },
    { "id": "cat_karton", "code": "KARTON", "name": "Karton", "reusable": true, "coinMultiplier": 1.0 },
    { "id": "cat_ahsap", "code": "AHSAP", "name": "Ahşap & Palet", "reusable": true, "coinMultiplier": 1.8 },
    { "id": "cat_hobi", "code": "HOBI", "name": "Hobi Malzemeleri", "reusable": true, "coinMultiplier": 1.5 },
    { "id": "cat_diger", "code": "DIGER", "name": "Diğer Atıklar", "reusable": true, "coinMultiplier": 1.0 }
  ]
  ```

### `GET /listings`
* **Açıklama:** Kullanıcının konumuna göre yakındaki ilanları cursor tabanlı sayfalama ile çeker. Arama ve kategori filtrelerini destekler.
* **Tetiklenme Anı:** Ekran açılışında, kategori seçildiğinde, arama yapıldığında veya liste aşağı kaydırıldığında (infinite scroll).
* **Query Parametreleri:** `lat`, `lon`, `radius` (metre, varsayılan 3000), `categoryId`, `q` (metin arama), `sort` (`distance` | `newest`), `limit`, `cursor`.
* **Yanıt (200 OK):**
  ```json
  {
    "items": [
      {
        "id": "ad_1",
        "title": "15 Adet Temiz Taşınma Kolisi",
        "description": "Taşınma sonrası boşta kalan temiz, hasarsız karton koliler.",
        "category": { "code": "KOLI", "name": "Koli & Kutu" },
        "quantityBand": "FEW",
        "condition": "GOOD",
        "thumbnailUrl": "https://storage.greentrack.app/listings/2026/08/ab12.jpg",
        "approxPoint": { "lat": 40.9912, "lon": 29.0261 },
        "distanceMeters": 400,
        "neighborhood": "Kadıköy, İstanbul",
        "publishedAt": "2026-08-27T05:00:00Z",
        "owner": { "id": "user_2", "displayName": "Ahmet Y.", "level": 4 }
      }
    ],
    "nextCursor": "eyJkIjoxMjAwLCJpZCI6ImFkXzEifQ==",
    "hasMore": true
  }
  ```

### `GET /listings/{id}`
* **Açıklama:** İlan kartına tıklandığında açılan detay modalı için tam bilgileri getirir.
* **Tetiklenme Anı:** Bir ilan kartına basıldığında.
* **Yanıt (200 OK):** İlanın fotoğrafları, tam açıklaması, yaklaşık konumu ve sahibinin seviyesi/güven skoru.

### `POST /listings/{id}/requests`
* **Açıklama:** Başkasının ilan detay modalında "İlgi Bildir / İste" butonuna basılarak talep oluşturulması.
* **Tetiklenme Anı:** "İlgi Bildir / İste" butonu tıklandığında.
* **İstek Gövdesi:**
  ```json
  { "message": "Merhaba, yarın elden teslim alabilirim." }
  ```
* **Yanıt (201 Created):**
  ```json
  { "id": "req_123", "status": "PENDING", "createdAt": "2026-08-27T05:45:00Z" }
  ```

### `POST /listings/{id}/withdraw`
* **Açıklama:** Kullanıcının kendi ilan detay modalında "Bu İlanı Kaldır" butonuna basarak ilanı yayından çekmesi.
* **Tetiklenme Anı:** "Bu İlanı Kaldır" butonu tıklandığında.
* **Yanıt (200 OK):** `{ "status": "WITHDRAWN" }`

---

## 3. Harita Görünümü (`src/screens/map/MapScreens.tsx`)

İlanların harita üzerinde pin'ler olarak görselleştirildiği, pin'e tıklandığında alt bilgi kartının açıldığı ekrandır.

### `GET /listings` (Harita Modu)
* **Açıklama:** Haritanın baktığı merkez koordinat ve yarıçap (`lat`, `lon`, `radius`) içindeki ilan pinlerini getirir.
* **Tetiklenme Anı:** Harita ekranı açıldığında veya haritada filtre hapları değiştirildiğinde.
* **Query Parametreleri:** `lat`, `lon`, `radius=5000`, `categoryId`.
* **Yanıt (200 OK):** Harita üzerine yerleştirilecek ilan pin dizisi (`id`, `title`, `category`, `approxPoint`).

### `POST /listings/{id}/requests`
* **Açıklama:** Haritada seçilen ilanın alt bilgi kartından "İlgi Bildir" butonuna basıldığında talep gönderir.
* **Tetiklenme Anı:** Alt karttaki "İlgi Bildir" butonu tıklandığında.

---

## 4. İlan Paylaşma ve Talep Açma Akışı (`src/screens/post/PostAdScreens.tsx`)

Kullanıcının fotoğraf çekip yeni bir malzeme paylaşım ilanı verdiği veya aradığı malzeme için ihtiyaç ilanı açtığı ekrandır.

### `POST /listings/photos/upload-url`
* **Açıklama:** Fotoğraf çekildiğinde S3/Object Storage'a doğrudan yükleme yapmak için presigned URL üretir.
* **Tetiklenme Anı:** Kamera ile fotoğraf çekildiği veya galeriden görsel seçildiği anda.
* **İstek Gövdesi:**
  ```json
  { "contentType": "image/jpeg" }
  ```
* **Yanıt (200 OK):**
  ```json
  {
    "uploadUrl": "https://storage.greentrack.app/listings/2026/08/ab12.jpg?X-Amz-Signature=...",
    "objectKey": "listings/2026/08/ab12.jpg"
  }
  ```

### `POST /waste-ai/classify`
* **Açıklama:** Yüklenen fotoğrafı analiz ederek malzeme kategorisi ve geri dönüşüm tavsiyesi üretir.
* **Tetiklenme Anı:** Fotoğraf S3'e başarıyla yüklendikten hemen sonra.
* **İstek Gövdesi:**
  ```json
  { "objectKey": "listings/2026/08/ab12.jpg" }
  ```
* **Yanıt (200 OK):**
  ```json
  {
    "categoryId": "cat_koli",
    "categoryName": "Koli & Kutu",
    "confidence": 0.92,
    "recommendation": "REUSE"
  }
  ```

### `POST /listings`
* **Açıklama:** Form doldurulduktan sonra ilanı yayınlar.
* **Tetiklenme Anı:** "İlanı Yayınla" butonuna basıldığında.
* **İstek Gövdesi:**
  ```json
  {
    "categoryId": "cat_koli",
    "title": "15 Adet Temiz Taşınma Kolisi",
    "description": "Taşınma sonrası boşta kalan sağlam koliler.",
    "quantityBand": "FEW",
    "condition": "GOOD",
    "photoKeys": ["listings/2026/08/ab12.jpg"],
    "pickupPoint": { "lat": 40.9901, "lon": 29.0250 },
    "usePublicMeetingPoint": true
  }
  ```
* **Yanıt (201 Created):**
  ```json
  { "id": "ad_123", "status": "PUBLISHED", "createdAt": "2026-08-27T05:48:00Z" }
  ```

### `GET /listings/mine`
* **Açıklama:** İlan verme sayfasının altındaki "AKTİF İLANLARIM" bölümünü listeler.
* **Tetiklenme Anı:** İlan verme sekmesi açıldığında.
* **Yanıt (200 OK):** Kullanıcının yayınladığı aktif ve tamamlanan ilan listesi.

---

## 5. İşlemler, Mesajlaşma ve Teslimat Doğrulama (`src/screens/transactions/TransactionsScreen.tsx`)

Giden/gelen taleplerin yönetildiği, mesajlaşıldığı ve buluşma anında teslim kodunun onaylandığı ekrandır.

### `GET /requests/mine`
* **Açıklama:** Kullanıcının hem alan (taker) hem de veren (giver) olduğu aktif ve geçmiş tüm işlem kayıtlarını listeler.
* **Tetiklenme Anı:** "İşlemler" sekmesi açıldığında veya "Aktif" / "Geçmiş" sekmeleri arasında geçiş yapıldığında.
* **Yanıt (200 OK):**
  ```json
  [
    {
      "id": "req_1",
      "listingId": "ad_1",
      "listingTitle": "15 Adet Temiz Taşınma Kolisi",
      "category": "KOLI",
      "status": "ACCEPTED",
      "giver": { "id": "user_2", "displayName": "Ahmet Y." },
      "taker": { "id": "my_user_id", "displayName": "Doğa Dostu" },
      "handoverId": "h_101",
      "reservedUntil": "2026-08-29T12:00:00Z"
    }
  ]
  ```

### `POST /requests/{id}/accept`
* **Açıklama:** İlan sahibinin gelen bir malzeme talebini onaylayıp ilanı rezerve etmesi ve teslimat kaydı (`Handover`) açması.
* **Tetiklenme Anı:** İlan sahibinin talep kartında "Onayla" butonuna basmasıyla.
* **Yanıt (200 OK):** `{ "status": "ACCEPTED", "handoverId": "h_101" }`

### `POST /requests/{id}/reject`
* **Açıklama:** İlan sahibinin talebi reddetmesi.
* **Tetiklenme Anı:** "Reddet" butonuna basıldığında.

### `POST /requests/{id}/cancel`
* **Açıklama:** Taraflardan birinin rezervasyonu iptal etmesi. İlan yeniden `PUBLISHED` durumuna döner.
* **Tetiklenme Anı:** "İptal Et" butonu tıklandığında.

### `GET /handovers/{id}/code` (Alan / Taker Tarafı)
* **Açıklama:** Malzemeyi **alan** kişinin teslimat anında paylaşana göstereceği 6 haneli tek kullanımlık kod ve QR verisini getirir.
* **Tetiklenme Anı:** Alan tarafı "Teslim Kodu Göster / QR" butonuna bastığında.
* **Yanıt (200 OK):**
  ```json
  {
    "code": "739201",
    "qrPayload": "greentrack://handover/h_101?code=739201",
    "expiresAt": "2026-08-30T12:00:00Z"
  }
  ```

### `POST /handovers/{id}/confirm` (Paylaşan / Giver Tarafı)
* **Açıklama:** Malzemeyi **paylaşan** kişinin buluşmada alanın gösterdiği 6 haneli kodu girerek veya QR'ı taratarak teslimi tamamlaması.
* **Tetiklenme Anı:** Paylaşan tarafı kodu girip "Teslim Ettim / Onayla" butonuna bastığında.
* **İstek Gövdesi:**
  ```json
  {
    "code": "739201",
    "lat": 40.9901,
    "lon": 29.0250
  }
  ```
* **Yanıt (200 OK):**
  ```json
  {
    "status": "CONFIRMED",
    "coinsGranted": 30,
    "capReached": false,
    "newBadges": [
      { "code": "FIRST_SHARE", "name": "İlk Paylaşım", "description": "İlk malzemenizi paylaştınız." }
    ]
  }
  ```

### `POST /handovers/{id}/no-show`
* **Açıklama:** Karşı taraf buluşma yerine gelmediğinde yapılan bildirim.
* **Tetiklenme Anı:** "Karşı Taraf Gelmedi" butonu tıklandığında.

---

## 6. Profil, Cüzdan ve Oyunlaştırma (`src/screens/profile/ProfileScreen.tsx`)

Kullanıcının seviyesinin, XP çubuğunun, Eco-Coin bakiyesinin, rozetlerinin ve çevresel tasarruf istatistiklerinin görüntülendiği ekrandır.

### `GET /users/me`
* **Açıklama:** Kullanıcının temel profil bilgilerini, seviyesini, XP'sini ve çevresel etki hesaplarını getirir.
* **Tetiklenme Anı:** Profil sekmesi açıldığında.
* **Yanıt (200 OK):**
  ```json
  {
    "id": "my_user_id",
    "displayName": "Doğa Dostu",
    "level": 3,
    "xp": 45,
    "sharedCount": 8,
    "receivedCount": 3,
    "sustainabilityStats": {
      "co2SavedKg": 14.8,
      "waterSavedLiters": 320,
      "landfillDivertedKg": 12.0
    }
  }
  ```

### `GET /wallet`
* **Açıklama:** Güncel Eco-Coin bakiyesi ve günlük/aylık kalan limitleri getirir.
* **Tetiklenme Anı:** Profil ve Cüzdan kartı açıldığında.
* **Yanıt (200 OK):**
  ```json
  {
    "balance": 320,
    "totalEarned": 580,
    "totalSpent": 260,
    "dailyCapRemaining": 70,
    "monthlyCapRemaining": 940
  }
  ```

### `GET /badges`
* **Açıklama:** Sistemdeki tüm rozetleri ve kullanıcının kazandıklarını (kilitli/açık) listeler.
* **Tetiklenme Anı:** Rozetler bölümü yüklendiğinde.
* **Yanıt (200 OK):**
  ```json
  [
    { "id": "b1", "code": "FIRST_SHARE", "name": "İlk Adım", "description": "İlk malzemenizi paylaştınız.", "iconName": "sprout", "earnedAt": "2026-05-12T10:00:00Z" },
    { "id": "b2", "code": "WATER_SAVER", "name": "Su Dostu", "description": "Cam atık paylaşarak su tasarrufu sağladınız.", "iconName": "droplet", "earnedAt": "2026-06-18T14:30:00Z" },
    { "id": "b4", "code": "GREEN_HERO", "name": "Yeşil Kahraman", "description": "50 kg CO2 tasarrufu sağlayın.", "iconName": "trophy", "earnedAt": null }
  ]
  ```

### `GET /quests`
* **Açıklama:** Bu ayın aktif görevlerini ve kullanıcının ilerleme durumunu getirir.
* **Tetiklenme Anı:** Görevler sekmesi veya profil sayfası yüklendiğinde.
* **Yanıt (200 OK):**
  ```json
  [
    {
      "id": "q_aug_1",
      "title": "3 Adet Cam Kavanoz Paylaş",
      "targetValue": 3,
      "currentValue": 2,
      "rewardCoins": 50,
      "completed": false
    }
  ]
  ```

### `GET /leaderboard`
* **Açıklama:** Mahalle, ilçe veya şehir düzeyinde puan sıralamasını getirir.
* **Tetiklenme Anı:** Sıralama tablosu görüntülendiğinde.
* **Query Parametreleri:** `scope` (`NEIGHBORHOOD` | `DISTRICT` | `CITY`), `period=2026-08`.

---

## 7. Ödül Harcama & Kupon Kataloğu (Rewards / Redemptions)

Eco-Coin'lerin belediye tesislerinde, toplu taşımada veya partner kampanyalarında harcandığı ekrandır.

### `GET /rewards`
* **Açıklama:** Harcanabilir aktif ödül ve kupon kataloğunu listeler.
* **Tetiklenme Anı:** Ödül Mağazası açıldığında.
* **Yanıt (200 OK):**
  ```json
  [
    {
      "id": "rew_101",
      "partnerName": "Kadıköy Belediyesi Sosyal Tesisleri",
      "title": "%20 İndirim Kuponu",
      "description": "Tüm belediye kafelerinde geçerlidir.",
      "costCoins": 150,
      "stock": 35,
      "validTo": "2026-12-31"
    }
  ]
  ```

### `POST /redemptions`
* **Açıklama:** Puan karşılığı ödül satın alır. `Idempotency-Key` başlığı zorunludur.
* **Tetiklenme Anı:** "Kuponu Al" butonuna basıldığında.
* **Headers:** `Idempotency-Key: <uuid>`
* **İstek Gövdesi:**
  ```json
  { "rewardId": "rew_101" }
  ```
* **Yanıt (201 Created):**
  ```json
  {
    "id": "red_881",
    "status": "CAPTURED",
    "voucherCode": "KDK-9921-ECO",
    "validUntil": "2026-12-31T23:59:59Z"
  }
  ```

### `GET /redemptions`
* **Açıklama:** Kullanıcının satın aldığı aktif ve kullanılmış kuponları listeler.

---

## 8. Geri Dönüşüm Toplama Noktaları (`src/screens/map/MapScreens.tsx` / Yönlendirme)

Paylaşıma uygun olmayan kırık cam, atık pil, elektronik gibi malzemeler için belediye toplama noktalarının listelendiği ekrandır.

### `GET /collection-points/nearby`
* **Açıklama:** Kullanıcının konumuna ve seçtiği atık türüne göre en yakın resmi geri dönüşüm merkezlerini listeler.
* **Tetiklenme Anı:** "Geri Dönüşüm Noktaları" filtresi seçildiğinde veya AI önerisi `RECYCLE` olduğunda.
* **Query Parametreleri:** `lat`, `lon`, `type` (`glass` | `paper` | `battery` | `electronic`), `limit=5`.
* **Yanıt (200 OK):**
  ```json
  [
    {
      "id": "cp_1",
      "name": "Kadıköy Belediyesi 1. Sınıf Atık Getirme Merkezi",
      "addressText": "Fahrettin Kerim Gökay Cad. No: 12",
      "distanceMeters": 650,
      "location": { "lat": 40.9920, "lon": 29.0340 },
      "acceptedTypes": ["glass", "paper", "battery", "electronic"],
      "openingHours": "Hafta içi 08:30 - 17:30"
    }
  ]
  ```

---

## 9. Ortak & Sistem İstekleri (Şikayet & Konum Güncelleme)

### `POST /reports`
* **Açıklama:** Uygunsuz ilan, sahte kullanıcı veya şüpheli teslimat şikayeti gönderme.
* **Tetiklenme Anı:** İlan veya kullanıcı profilindeki "Şikayet Et" seçeneğine basıldığında.
* **İstek Gövdesi:**
  ```json
  {
    "targetType": "LISTING",
    "targetId": "ad_1",
    "reason": "COMMERCIAL_SALE",
    "note": "İlanda ücretsiz yerine ücret talep ediliyor."
  }
  ```
* **Yanıt (201 Created):** `{ "id": "rep_123", "status": "OPEN" }`

### `PUT /users/me/location`
* **Açıklama:** Kullanıcı konum izni verdiğinde veya GPS konumu değiştiğinde yaklaşık konum ve mahalle güncellemesi yapar.
* **Tetiklenme Anı:** Uygulama açılışında GPS konumu alındığında.
* **İstek Gövdesi:**
  ```json
  { "lat": 40.9901, "lon": 29.0250 }
  ```
* **Yanıt (200 OK):** Güncellenmiş mahalle ve koordinat özeti.
