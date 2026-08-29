# Uçtan Uca Akışlar

Diyagramlardaki bileşen adları [`01-modules.md`](01-modules.md)'deki mikroservislerle, veri adları [`02-domain-model.md`](02-domain-model.md)'deki varlıklarla birebir aynıdır.

## 1. İlan verme

Fotoğraf API üzerinden geçmez; istemci presigned URL ile doğrudan object storage'a yükler. AI önerisi **tavsiye niteliğindedir** ve akışı bloklamaz.

```mermaid
sequenceDiagram
    actor G as Paylaşan
    participant API as API Gateway
    participant C as catalog-service
    participant S3 as Object storage
    participant AI as wasteai-service

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

Yayın anında `ListingPublished` outbox/event broker'a yazılır; `gamification-service` bunu işler.

## 2. Yakındaki ilanların aranması

```mermaid
sequenceDiagram
    actor T as Alan
    participant API as API Gateway
    participant C as catalog-service
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

Sayfalama **cursor tabanlıdır**. Yarıçap üst sınırı 25 km'dir; daha geniş sorgular reddedilir.

## 3. Talep → onay → teslim → Eco-Coin

Sistemin ana akışı.

```mermaid
sequenceDiagram
    actor T as Alan
    actor G as Paylaşan
    participant API as API Gateway
    participant EX as exchange-service
    participant C as catalog-service
    participant E as ecocoin-service
    participant GM as gamification-service
    participant N as notification-service

    T->>API: POST /listings/{id}/requests {mesaj}
    API->>EX: createRequest
    EX->>EX: Request(status=PENDING)
    EX-->>N: (event) RequestCreated
    N-->>G: bildirim "yeni talep"

    G->>API: POST /requests/{id}/accept
    API->>EX: accept(requestId)
    EX->>C: markReserved(listingId, requestId)
    C->>C: UPDATE ... WHERE status='PUBLISHED'
    alt 0 satır etkilendi
        C-->>EX: çakışma
        EX-->>API: 409 listing_not_available
    else Başarılı
        EX->>EX: Request.status=ACCEPTED, reserved_until=now+48s
        EX->>EX: 6 haneli teslim kodu üret, hash'le, TTL=72s
        EX-->>N: (event) RequestAccepted
        N-->>T: "talebin kabul edildi"
        N-->>G: "buluşma detayları"
    end

    Note over T,G: Buluşma. exact_point artık iki tarafa görünür.
    T->>API: GET /handovers/{id}/code
    API->>EX: kodu getir (yalnızca alan tarafına)
    EX-->>T: 6 haneli kod + QR

    G->>API: POST /handovers/{id}/confirm {kod}
    API->>EX: confirm(kod, ip, konum)
    EX->>EX: hash karşılaştır, deneme sayacı, risk değerlendirmesi
    alt Kod yanlış
        EX-->>API: 400 invalid_code (5. denemede EXPIRED)
    else Kod doğru, risk yok
        EX->>EX: status=CONFIRMED
        EX->>C: markHandedOver(listingId)
        EX->>EX: outbox: HandoverConfirmed{reviewRequired=false}
    else Kod doğru, risk bayrağı var
        EX->>EX: status=PENDING_REVIEW
        EX->>EX: outbox: HandoverConfirmed{reviewRequired=true}
        Note over EX: Moderatör onayına kadar puan verilmez
    end

    EX-->>E: (event) HandoverConfirmed
    EX-->>EX: reviewRequired=false ise Request.status=COMPLETED
    E->>E: idempotencyKey=handover:{id}
    E->>E: puan hesapla, GRANT (SYSTEM_MINT → kullanıcı)
    E->>E: outbox: CoinsGranted
    E-->>GM: (event) CoinsGranted
    E-->>N: (event) CoinsGranted
    EX-->>GM: (event) HandoverConfirmed
    GM->>GM: rozet kuralları, seviye, görev ilerlemesi
    N-->>G: "X Eco-Coin kazandın"
```

## 4. İptal, no-show ve süre dolumu

```mermaid
sequenceDiagram
    actor T as Alan
    actor G as Paylaşan
    participant SCH as Zamanlanmış iş
    participant EX as exchange-service
    participant C as catalog-service
    participant N as notification-service

    loop 15 dakikada bir
        SCH->>EX: reserved_until < now olan ACCEPTED talepler
        EX->>EX: Request.status=EXPIRED
        EX->>C: releaseReservation(listingId)
        C->>C: status=RESERVED → PUBLISHED
        EX-->>N: (event) ReservationExpired
        N-->>T: "rezervasyon süresi doldu"
        N-->>G: "ilan yeniden yayında"
    end

    loop Günlük
        SCH->>C: expires_at < now olan PUBLISHED ilanlar
        C->>C: status=EXPIRED
    end
```

## 5. "Paylaşıma uygun değil" → toplama noktası

```mermaid
sequenceDiagram
    actor U as Kullanıcı
    participant API as API Gateway
    participant AI as wasteai-service
    participant C as catalog-service (recycling)
    participant DB as PostGIS

    U->>API: POST /waste-ai/classify {objectKey}
    API->>AI: classify
    AI-->>API: {tür: "kırık cam", öneri: RECYCLE}
    API-->>U: "Bu paylaşıma uygun değil, geri dönüşüme yönlendirilmeli"

    U->>API: GET /collection-points/nearby?lat&lon&type=glass&limit=5
    API->>C: findNearby
    C->>DB: point <-> :konum sıralı, accepted_types @> ARRAY['glass']
    DB-->>C: en yakın 5 nokta
    C-->>API: {nokta, adres, mesafe, çalışma saatleri}
    API-->>U: harita + yol tarifi bağlantısı
```

Bu akış **puan üretmez.**

## 6. Eco-Coin harcaması

```mermaid
sequenceDiagram
    actor U as Kullanıcı
    participant API as API Gateway
    participant E as ecocoin-service
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
            Note over E,P: Uzlaştırma işi GET /vouchers/{idempotencyKey} ile<br/>partner durumunu sorgular
        end
    end
```
