# Uçtan Uca Akışlar

Diyagramlardaki bileşen adları [`01-modules.md`](01-modules.md)'deki modüllerle, veri adları [`02-domain-model.md`](02-domain-model.md)'deki varlıklarla birebir aynıdır.

## 1. İlan verme

Fotoğraf API üzerinden geçmez; istemci presigned URL ile doğrudan object storage'a yükler. AI önerisi **tavsiye niteliğindedir** ve akışı bloklamaz.

```mermaid
sequenceDiagram
    actor G as Paylaşan
    participant API
    participant C as catalog
    participant S3 as Object storage
    participant AI as wasteai

    G->>API: POST /listings/photos/upload-url
    API->>C: presigned URL üret
    C-->>API: {uploadUrl, objectKey}
    API-->>G: {uploadUrl, objectKey}
    G->>S3: PUT fotoğraf (doğrudan)
    S3-->>G: 200

    G->>API: POST /waste-ai/classify {objectKey}
    API->>AI: classify(photoRef)
    alt Model cevap verdi
        AI-->>API: {kategori, güven, REUSE|RECYCLE}
        API-->>G: öneri gösterilir
    else Zaman aşımı / hata
        AI--xAPI: timeout (2 sn)
        API-->>G: öneri yok, akış devam eder
    end

    alt Öneri RECYCLE ve kategori reusable=false
        G->>API: GET /collection-points/nearby
        Note over G,API: Akış 5'e dallanır
    else Yeniden kullanılabilir
        G->>API: POST /listings {kategori, başlık, miktar, durum, konum, fotoğraflar}
        C->>C: approx_point = jitter(gerçek nokta, ~250m) — bir kez hesaplanır
        C->>C: status=DRAFT, expires_at=now+21g
        G->>API: POST /listings/{id}/publish
        C->>C: status=PUBLISHED (en az 1 foto + kategori kontrolü)
        C->>C: outbox: ListingPublished
        C-->>API: 200
    end
```

Yayın anında `ListingPublished` outbox'a yazılır ve aynı transaction'da commit edilir; dispatcher bunu `gamification`'a taşır.

## 2. Yakındaki ilanların aranması

```mermaid
sequenceDiagram
    actor T as Alan
    participant API
    participant C as catalog
    participant DB as PostgreSQL/PostGIS

    T->>API: GET /listings?lat&lon&radius=3000&categoryId=<uuid>&cursor=...
    API->>C: search(kriter)
    C->>DB: ST_DWithin(approx_point, :point, :radius) AND status='PUBLISHED'
    Note over DB: GiST indeksi kullanılır,<br/>ST_Distance ile sıralanır
    DB-->>C: sayfa (limit+1 satır)
    C->>C: mesafeleri 100 m'ye yuvarla, exact_point'i çıkar
    C-->>API: {items, nextCursor}
    API-->>T: sonuçlar
```

Sayfalama **cursor tabanlıdır**; cursor sıralama anahtarının kodlanmış halidir. Anahtar `sort` parametresine göre değişir: `distance` için `(distance, listing_id)`, `newest` için `(published_at DESC, listing_id)`. Offset kullanılmaz — akışa yeni ilan girdiğinde kayma olmaması için.

Yarıçap üst sınırı 25 km'dir; daha geniş sorgular reddedilir.

## 3. Talep → onay → teslim → Eco-Coin

Sistemin ana akışı.

```mermaid
sequenceDiagram
    actor T as Alan
    actor G as Paylaşan
    participant API
    participant M as matching
    participant C as catalog
    participant H as handover
    participant E as ecocoin
    participant GM as gamification
    participant N as notification

    T->>API: POST /listings/{id}/requests {mesaj}
    API->>M: createRequest
    M->>M: Request(status=PENDING)
    M-->>N: (event) RequestCreated
    N-->>G: bildirim "yeni talep"

    G->>API: POST /requests/{id}/accept
    API->>M: accept(requestId)
    M->>C: markReserved(listingId, requestId)
    C->>C: UPDATE ... WHERE status='PUBLISHED'
    alt 0 satır etkilendi
        C-->>M: çakışma
        M-->>API: 409 listing_not_available
    else Başarılı
        M->>M: Request.status=ACCEPTED, reserved_until=now+48s
        M->>H: open(requestId)
        H->>H: 6 haneli kod üret, hash'le, TTL=72s
        M-->>N: (event) RequestAccepted
        N-->>T: "talebin kabul edildi"
        N-->>G: "buluşma detayları"
    end

    Note over T,G: Buluşma. exact_point artık iki tarafa görünür.
    T->>API: GET /handovers/{id}/code
    API->>H: kodu getir (yalnızca alan tarafına)
    H-->>T: 6 haneli kod + QR

    G->>API: POST /handovers/{id}/confirm {kod}
    API->>H: confirm(kod, ip, konum)
    H->>H: hash karşılaştır, deneme sayacı, risk değerlendirmesi
    alt Kod yanlış
        H-->>API: 400 invalid_code (5. denemede EXPIRED)
    else Kod doğru, risk yok
        H->>H: status=CONFIRMED
        H->>C: markHandedOver(listingId)
        H->>H: outbox: HandoverConfirmed{reviewRequired=false}
    else Kod doğru, risk bayrağı var
        H->>H: status=PENDING_REVIEW
        H->>H: outbox: HandoverConfirmed{reviewRequired=true}
        Note over H: Moderatör onayına kadar puan verilmez
    end

    H-->>M: (event) HandoverConfirmed
    M->>M: reviewRequired=false ise Request.status=COMPLETED
    H-->>E: (event) HandoverConfirmed
    E->>E: idempotencyKey=handover:{id}
    E->>E: puan hesapla, GRANT (SYSTEM_MINT → kullanıcı)
    E->>E: outbox: CoinsGranted
    E-->>GM: (event) CoinsGranted
    E-->>N: (event) CoinsGranted
    H-->>GM: (event) HandoverConfirmed
    GM->>GM: rozet kuralları, seviye, görev ilerlemesi
    N-->>G: "X Eco-Coin kazandın"
```

Kritik noktalar:

- Kodu **alan taraf gösterir, paylaşan taraf onaylar.** Ters kurgu (paylaşanın kodu göstermesi) sahte teslimatı kolaylaştırırdı: paylaşan kendi ikinci hesabına kodu iletip tek başına döngüyü kapatabilirdi. Bu kurguda da mutlak bir engel yoktur ama en az iki cihaz ve fiziksel bir buluşma iddiası gerekir; kalan risk anti-fraud kurallarıyla yönetilir ([`04-ecocoin-rules.md`](04-ecocoin-rules.md)).
- `handover` puan miktarını **hesaplamaz**; yalnızca olguyu bildirir. Puan kuralı tek yerdedir.
- Tüm event'ler outbox üzerinden gider; dinleyiciler `ProcessedEvent` ile idempotenttir.

## 4. İptal, no-show ve süre dolumu

```mermaid
sequenceDiagram
    actor T as Alan
    actor G as Paylaşan
    participant SCH as Zamanlanmış iş
    participant M as matching
    participant C as catalog
    participant H as handover
    participant N as notification

    loop 15 dakikada bir
        SCH->>M: reserved_until < now olan ACCEPTED talepler
        M->>M: Request.status=EXPIRED
        M->>H: expire(handoverId)
        M->>C: releaseReservation(listingId)
        C->>C: status=RESERVED → PUBLISHED
        M-->>N: (event) ReservationExpired
        N-->>T: "rezervasyon süresi doldu"
        N-->>G: "ilan yeniden yayında"
    end

    loop Günlük
        SCH->>C: expires_at < now olan PUBLISHED ilanlar
        C->>C: status=EXPIRED
    end
```

**No-show bildirimi.** Rezervasyon süresi dolduktan sonra taraflardan biri `POST /handovers/{id}/no-show` ile karşı tarafı bildirebilir. Bu bildirim tek başına yaptırım doğurmaz; `identity` modülünde `TrustScore.no_show_count` artırılır ve eşik aşılırsa `moderation` kuyruğuna düşer. Karşılıklı suçlamalarda otomatik karar verilmez.

Manuel iptalde (`POST /requests/{id}/cancel`) aynı serbest bırakma adımları senkron çalışır.

## 5. "Paylaşıma uygun değil" → toplama noktası

```mermaid
sequenceDiagram
    actor U as Kullanıcı
    participant API
    participant AI as wasteai
    participant R as recycling
    participant DB as PostGIS

    U->>API: POST /waste-ai/classify {objectKey}
    API->>AI: classify
    AI-->>API: {tür: "kırık cam", öneri: RECYCLE}
    API-->>U: "Bu paylaşıma uygun değil, geri dönüşüme yönlendirilmeli"

    U->>API: GET /collection-points/nearby?lat&lon&type=glass&limit=5
    API->>R: findNearby
    R->>DB: point <-> :konum sıralı, accepted_types @> ARRAY['glass']
    DB-->>R: en yakın 5 nokta
    R-->>API: {nokta, adres, mesafe, çalışma saatleri}
    API-->>U: harita + yol tarifi bağlantısı
```

Bu akış **puan üretmez.** Toplama noktasına gidildiği sistem tarafından doğrulanamaz; doğrulanamayan bir davranışa puan vermek Eco-Coin'in değerini bozar. İleride belediye tarafında doğrulanabilir bir bırakma kanıtı (kart okutma, terazi kaydı) olursa ayrı bir kazanım kuralı eklenir.

Kullanıcı AI kullanmadan doğrudan kategori seçerek de bu akışa girebilir.

## 6. Eco-Coin harcaması

İki tarafın da hata verebildiği bir akış; para benzeri bir kaynak söz konusu olduğu için `HOLD → CAPTURE → RELEASE` üç adımlıdır.

```mermaid
sequenceDiagram
    actor U as Kullanıcı
    participant API
    participant E as ecocoin
    participant P as Partner API

    U->>API: POST /redemptions {rewardId} [Idempotency-Key]
    API->>E: redeem(userId, rewardId, key)
    E->>E: bakiye ve stok kontrolü
    alt Yetersiz bakiye
        E-->>API: 409 insufficient_balance
    else Yeterli
        E->>E: HOLD (kullanıcı → SYSTEM_HOLD), Redemption(status=HELD)
        E->>P: POST /vouchers {externalUserRef, rewardCode, amount}<br/>Idempotency-Key: redemptionId
        alt Partner onayladı
            P-->>E: 200 {voucherCode, validUntil}
            E->>E: CAPTURE (SYSTEM_HOLD → SYSTEM_BURN), status=CAPTURED
            E->>E: outbox: RedemptionCaptured
            E-->>API: {voucherCode}
        else Partner reddetti
            P-->>E: 4xx
            E->>E: RELEASE (SYSTEM_HOLD → kullanıcı), status=RELEASED
            E-->>API: 502 partner_rejected
        else Cevap yok / zaman aşımı
            P--xE: timeout
            E->>E: status=FAILED, hold korunur
            Note over E,P: Uzlaştırma işi GET /vouchers/{idempotencyKey} ile<br/>partner durumunu sorgular:<br/>başarılıysa CAPTURE, değilse RELEASE
        end
    end
```

- Hold süresi **15 dakikadır.** Uzlaştırma işi bu süre içinde sonuca varamazsa hold serbest bırakılır ve kullanıcıya bildirim gider.
- `Idempotency-Key` başlığı zorunludur; aynı anahtarla tekrar eden çağrı yeni hold açmaz, mevcut `Redemption` kaydını döner.
- Partner API'sinin idempotent olması sözleşme şartıdır ([`04-ecocoin-rules.md`](04-ecocoin-rules.md), "Harcama tarafı").
