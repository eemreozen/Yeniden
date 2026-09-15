# API Sözleşmesi

Taban yol: `/api/v1`. Tüm gövdeler JSON, tüm zaman damgaları ISO-8601 UTC, tüm id'ler UUID.

## Kimlik doğrulama

- **JWT access token**, `Authorization: Bearer <token>`, ömür 15 dakika.
- **Refresh token**, dönen (rotating), ömür 30 gün, veritabanında hash'lenmiş saklanır. Kullanılan refresh token geçersizleşir; aynı token ikinci kez kullanılırsa o kullanıcının tüm oturumları kapatılır (token çalınması sinyali).
- Token içeriği: `sub` (userId), `nbh` (neighborhoodId), `roles`, `exp`. Bakiye, seviye gibi değişken bilgiler token'a konmaz.
- Rol seti: `USER`, `MODERATOR`, `ADMIN`.

## Ortak kurallar

**Sayfalama** — cursor tabanlı. İstek: `?limit=20&cursor=<opak>`. Yanıt:
```json
{ "items": [ ... ], "nextCursor": "eyJkIjo...", "hasMore": true }
```
`limit` üst sınırı 50'dir. Offset sayfalama desteklenmez.

**Idempotency** — `POST /redemptions` için `Idempotency-Key` başlığı **zorunludur**. Diğer POST uçlarında opsiyoneldir; verilirse 24 saat boyunca aynı yanıt döner.

**Hata gövdesi** — RFC 7807 tarzı:
```json
{
  "type": "https://greentrack.app/errors/listing_not_available",
  "title": "İlan artık müsait değil",
  "status": 409,
  "code": "listing_not_available",
  "detail": "Bu ilan başka bir talebe rezerve edildi.",
  "traceId": "0af7651916cd43dd"
}
```
`code` makine tarafından okunur ve kararlıdır; `title`/`detail` kullanıcının diline göre değişebilir.

| Kod | HTTP | Anlam |
|---|---|---|
| `validation_failed` | 400 | Alan doğrulama hatası (`errors[]` ekli) |
| `invalid_code` | 400 | Teslim kodu yanlış |
| `unauthorized` | 401 | Token yok / geçersiz |
| `forbidden` | 403 | Yetki yok (başkasının ilanı vb.) |
| `not_found` | 404 | Kaynak yok veya erişim hakkı yok |
| `listing_not_available` | 409 | İlan rezerve/kapalı |
| `duplicate_request` | 409 | Aynı ilana açık talebin var |
| `insufficient_balance` | 409 | Bakiye yetersiz |
| `out_of_stock` | 409 | Ödül stoğu bitti |
| `daily_cap_reached` | 422 | Teslim kaydedildi, puan tavanı doldu |
| `trust_too_low` | 403 | Güven skoru eşiğin altında |
| `rate_limited` | 429 | `Retry-After` başlığı eklenir |
| `partner_rejected` | 502 | Partner kurum reddetti |

## Uçlar

### auth

| Metot | Yol | Açıklama |
|---|---|---|
| `POST` | `/auth/otp/request` | `{phone}` → SMS gönderir. IP ve numara başına oranlanır |
| `POST` | `/auth/otp/verify` | `{phone, code}` → `{accessToken, refreshToken, isNewUser}` |
| `POST` | `/auth/refresh` | `{refreshToken}` → yeni çift |
| `POST` | `/auth/logout` | Mevcut refresh token'ı iptal eder |

### users

| Metot | Yol | Açıklama |
|---|---|---|
| `GET` | `/users/me` | Profil, mahalle, seviye, güven skoru, bakiye özeti |
| `PATCH` | `/users/me` | `displayName`, `avatarKey`, `notificationPrefs` |
| `PUT` | `/users/me/location` | `{lat, lon}` → yaklaşık konum + mahalle ataması |
| `GET` | `/users/{id}` | Herkese açık özet: görünen ad, seviye, rozetler, tamamlanan teslim sayısı. Konum ve telefon dönmez |
| `DELETE` | `/users/me` | Hesap silme; ilanlar kaldırılır, ledger anonimleştirilir |

### listings

| Metot | Yol | Açıklama |
|---|---|---|
| `POST` | `/listings/photos/upload-url` | `{contentType}` → `{uploadUrl, objectKey}` (presigned, 10 dk) |
| `POST` | `/listings` | Taslak oluşturur |
| `PATCH` | `/listings/{id}` | Yalnızca `DRAFT` ve `PUBLISHED` durumunda |
| `POST` | `/listings/{id}/publish` | `DRAFT → PUBLISHED` |
| `POST` | `/listings/{id}/withdraw` | `PUBLISHED → WITHDRAWN` |
| `GET` | `/listings/{id}` | Detay. `exactPoint` yalnızca sahip ve rezervasyon sahibine döner |
| `GET` | `/listings` | Yakınlık araması |
| `GET` | `/listings/mine` | Kendi ilanları, `?status=` filtreli |

`POST /listings` gövdesi:
```json
{
  "categoryId": "uuid",
  "title": "5 adet taşıma kolisi",
  "description": "Taşınmadan kaldı, sağlam.",
  "quantityBand": "FEW",
  "condition": "GOOD",
  "photoKeys": ["listings/2026/07/ab12.jpg"],
  "pickupPoint": { "lat": 41.0421, "lon": 29.0075 },
  "usePublicMeetingPoint": true
}
```

`GET /listings` parametreleri:

| Parametre | Zorunlu | Not |
|---|---|---|
| `lat`, `lon` | evet | arama merkezi |
| `radius` | hayır | metre, varsayılan 3000, üst sınır 25000 |
| `categoryId` | hayır | alt kategorileri de kapsar |
| `quantityBand` | hayır | |
| `q` | hayır | başlık/açıklama metin araması |
| `sort` | hayır | `distance` (varsayılan) veya `newest` |
| `limit`, `cursor` | hayır | |

Yanıt öğesi:
```json
{
  "id": "uuid",
  "title": "5 adet taşıma kolisi",
  "categoryId": "uuid",
  "quantityBand": "FEW",
  "condition": "GOOD",
  "thumbnailUrl": "https://...",
  "approxPoint": { "lat": 41.043, "lon": 29.006 },
  "distanceMeters": 800,
  "neighborhood": "Kuzguncuk",
  "publishedAt": "2026-07-29T09:12:00Z",
  "owner": { "id": "uuid", "displayName": "Emre", "level": 4 }
}
```
`distanceMeters` her zaman 100 m'ye yuvarlanır.

### requests

| Metot | Yol | Açıklama |
|---|---|---|
| `POST` | `/listings/{id}/requests` | `{message}` → talep oluşturur |
| `GET` | `/listings/{id}/requests` | Yalnızca ilan sahibi |
| `GET` | `/requests/mine` | Gönderdiğim talepler |
| `POST` | `/requests/{id}/accept` | İlan sahibi; ilanı rezerve eder, `Handover` yaratır |
| `POST` | `/requests/{id}/reject` | İlan sahibi |
| `POST` | `/requests/{id}/cancel` | Her iki taraf; rezervasyonu serbest bırakır |

### handovers

| Metot | Yol | Açıklama |
|---|---|---|
| `GET` | `/handovers/{id}` | Durum, karşı taraf, buluşma noktası |
| `GET` | `/handovers/{id}/code` | **Yalnızca alan taraf.** `{code, qrPayload, expiresAt}` |
| `POST` | `/handovers/{id}/confirm` | **Yalnızca paylaşan taraf.** `{code, lat, lon}` |
| `POST` | `/handovers/{id}/no-show` | Karşı tarafın gelmediğini bildirir |

`confirm` yanıtı:
```json
{
  "status": "CONFIRMED",
  "coinsGranted": 30,
  "capReached": false,
  "newBadges": [{ "code": "FIRST_SHARE", "name": "İlk Paylaşım" }]
}
```
`status` `PENDING_REVIEW` döndüğünde `coinsGranted` 0'dır ve `reviewReason` alanı eklenir. Rozet bilgisi event işlendikten sonra oluştuğu için yanıt en iyi çabayla doldurulur; boş dönebilir, istemci `/badges` ucundan tazeler.

### wallet & rewards

| Metot | Yol | Açıklama |
|---|---|---|
| `GET` | `/wallet` | `{balance, totalEarned, totalSpent, dailyCapRemaining, monthlyCapRemaining}` |
| `GET` | `/wallet/entries` | Hareket dökümü, sayfalı |
| `GET` | `/rewards` | Ödül kataloğu, `?partnerId=` filtreli |
| `POST` | `/redemptions` | `{rewardId}` + `Idempotency-Key` zorunlu |
| `GET` | `/redemptions` | Geçmiş |
| `GET` | `/redemptions/{id}` | Voucher kodu dahil detay |

### gamification

| Metot | Yol | Açıklama |
|---|---|---|
| `GET` | `/badges` | Tüm rozetler + kullanıcının kazandıkları |
| `GET` | `/quests` | Bu ayın görevleri + ilerleme |
| `GET` | `/leaderboard` | `?scope=NEIGHBORHOOD\|DISTRICT\|CITY&period=2026-07` |

`/leaderboard` `LeaderboardSnapshot` tablosundan okur; canlı hesaplama yapmaz. Yanıt `generatedAt` alanı taşır. Kullanıcı ilk 100'de değilse kendi satırı `me` alanında ayrıca döner.

### recycling & AI

| Metot | Yol | Açıklama |
|---|---|---|
| `GET` | `/collection-points/nearby` | `?lat&lon&type=&limit=` → en yakın noktalar |
| `GET` | `/categories` | Kategori ağacı, `reusable` bayrağıyla |
| `POST` | `/waste-ai/classify` | `{objectKey}` → `{categoryId, confidence, recommendation, modelVersion}` |

`/waste-ai/classify` en fazla 2 saniyede döner; model yanıt vermezse `{recommendation: "UNKNOWN"}` ile 200 döner. İstemci bu ucun başarısızlığını akış kesici saymaz.

### moderation

| Metot | Yol | Rol |
|---|---|---|
| `POST` | `/reports` | `USER` — `{targetType, targetId, reason, note}` |
| `GET` | `/moderation/queue` | `MODERATOR` — açık şikayetler ve `PENDING_REVIEW` teslimler |
| `POST` | `/moderation/handovers/{id}/approve` | `MODERATOR` — puanı serbest bırakır |
| `POST` | `/moderation/handovers/{id}/reject` | `MODERATOR` — teslimi iptal eder |
| `POST` | `/moderation/reports/{id}/resolve` | `MODERATOR` |

Moderasyon uçlarındaki her karar `resolved_by` ile kaydedilir ve geri alınamaz denetim izi bırakır.

## Rate limiting

| Uç | Sınır |
|---|---|
| `POST /auth/otp/request` | numara başına 3/saat, IP başına 10/saat |
| `POST /handovers/{id}/confirm` | teslim başına 5 deneme (kalıcı), kullanıcı başına 20/saat |
| `POST /listings` | 10/saat |
| `POST /listings/{id}/requests` | 30/gün |
| `POST /waste-ai/classify` | 60/saat |
| `GET /listings` | 120/dakika |
| Genel | kullanıcı başına 600/dakika |

Aşımda `429` + `Retry-After`. Sınırlar Redis tabanlı kayan pencere ile uygulanır; Redis erişilemezse istekler geçirilir (fail-open) ve uyarı üretilir — oran sınırlama güvenlik katmanının kendisi değil, destekleyicisidir.
