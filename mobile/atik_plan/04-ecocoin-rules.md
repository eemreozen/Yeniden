# Eco-Coin Kuralları

Eco-Coin, dış dünyada karşılığı olan (ulaşım bakiyesi, tesis indirimi) bir kaynaktır. Bu yüzden **para gibi ele alınır**: tam sayı birim, çift kayıtlı defter, idempotent yazma, denetlenebilir geçmiş.

## Kazanım formülü

Puan yalnızca `HandoverConfirmed` event'i ile verilir. Başka hiçbir yol yoktur — ilan açmak, uygulamayı kullanmak veya toplama noktasına gitmek puan üretmez.

```
puan = round( TEMEL × kategori_katsayısı × miktar_katsayısı )
```

| Bileşen | Değer |
|---|---|
| `TEMEL` | 10 puan |
| `kategori_katsayısı` | `ItemCategory.coin_multiplier` — 0.8 – 2.0 arası |
| `miktar_katsayısı` | `SINGLE` = 1.0, `FEW` (2–5) = 1.5, `MANY` (6+) = 2.0 |

Örnek katsayılar:

| Kategori | Katsayı | Gerekçe |
|---|---|---|
| Cam kavanoz | 1.2 | yaygın, kolay yeniden kullanılır |
| Koli / karton | 1.0 | referans |
| Ahşap parça / palet | 1.8 | taşıması zahmetli, yeniden kullanım değeri yüksek |
| Hobi malzemesi | 1.5 | aksi halde kesin çöpe gider |
| Küçük ev eşyası | 2.0 | en yüksek çevresel kazanç |

Bir teslimden çıkabilecek aralık: **8 – 40 puan.**

**Her iki taraf da puan alır mı?** Hayır. Puanın tamamı paylaşana gider; alan taraf zaten malzemeyi almıştır. İki tarafa da puan vermek, birbirine boş kutu verip duran hesap çiftlerini doğrudan kârlı hale getirirdi.

### Tavanlar

| Sınır | Değer | Amaç |
|---|---|---|
| Günlük kazanım | 100 puan | uç durum sömürüsünü sınırlar |
| Aylık kazanım | 1200 puan | ekonomiyi öngörülebilir tutar |
| Aynı karşı tarafla aylık puanlı teslim | 2 | döngüsel hesap çiftlerini kırar |
| Aynı kategoriden günlük puanlı teslim | 3 | tek tip seri işlemi sınırlar |

Tavan aşıldığında teslim yine kaydedilir ve rozet/görev sayacına girer, ancak puan verilmez; kullanıcıya sebep açıkça bildirilir.

### Görev ödülleri

Aylık görev tamamlandığında `gamification`, `ecocoin.grant`'i `reason=QUEST_COMPLETED` ve `idempotencyKey=quest:{userId}:{questId}` ile çağırır. Görev ödülleri günlük tavana dahil değildir, aylık tavana dahildir.

## Ledger tasarımı

### Değişmezler

1. Her `CoinTransaction` en az iki `CoinEntry` üretir ve bunların net toplamı **sıfırdır**.
2. Bakiye hiçbir yerde sütun olarak tutulmaz; `SUM(CREDIT) − SUM(DEBIT)` ile hesaplanır.
3. Kullanıcı hesabının bakiyesi negatif olamaz. Kontrol, yazma transaction'ı içinde hesap satırı `SELECT ... FOR UPDATE` ile kilitlenerek yapılır.
4. Kayıt **silinmez ve güncellenmez.** Hata düzeltmesi `ADJUSTMENT` tipiyle ters kayıt atılarak yapılır.
5. Her yazma `idempotency_key` ister; `unique` kısıt bunu zorlar.

### Hesaplar

| Hesap | Rol |
|---|---|
| `SYSTEM_MINT` | Kazanımların kaynağı. Bakiyesi sürekli negatife gider; toplam basılan puanı gösterir |
| `SYSTEM_HOLD` | Harcama sırasında rezerve edilen puanların geçici durağı |
| `SYSTEM_BURN` | Harcanan puanların gittiği yer. Bakiyesi toplam yakılan puanı gösterir |
| `USER:{id}` | Kullanıcı cüzdanı |
| `PARTNER:{id}` | Partner mutabakatı için (opsiyonel, raporlama) |

### İşlem tipleri

| Tip | Borç (DEBIT) | Alacak (CREDIT) | Tetikleyen |
|---|---|---|---|
| `GRANT` | `SYSTEM_MINT` | `USER:{id}` | `HandoverConfirmed`, görev tamamlama |
| `HOLD` | `USER:{id}` | `SYSTEM_HOLD` | Ödül talebi |
| `CAPTURE` | `SYSTEM_HOLD` | `SYSTEM_BURN` | Partner onayı |
| `RELEASE` | `SYSTEM_HOLD` | `USER:{id}` | Partner reddi / zaman aşımı |
| `ADJUSTMENT` | duruma göre | duruma göre | Moderatör kararı, sahtecilik geri alımı |

### İdempotency anahtarları

| Kaynak | Anahtar |
|---|---|
| Teslim ödülü | `handover:{handoverId}` |
| Görev ödülü | `quest:{userId}:{questId}` |
| Ödül talebi | İstemcinin `Idempotency-Key` başlığı |
| Geri alma | `reversal:{originalTransactionId}` |

Aynı event iki kez teslim edilirse ikinci yazma unique kısıta takılır; kod bunu hata değil, "zaten işlendi" olarak ele alır.

### Denetim

Gece çalışan bir doğrulama işi şunları kontrol eder:
- Her transaction'ın entry toplamı sıfır mı
- Tüm hesapların bakiye toplamı sıfır mı (kapalı sistem)
- `HOLD` durumunda 15 dakikadan uzun süre bekleyen kayıt var mı
- Hiçbir kullanıcı bakiyesi negatif mi

Sapma bulunursa uyarı üretilir; otomatik düzeltme yapılmaz.

## Kötüye kullanıma karşı önlemler

Ana tehdit: **fiziksel bir teslim olmadan puan üretmek.** İki hesap açıp birbirine sahte ilan verip teslim onaylamak yeterli olmamalıdır.

### Katman 1 — Hesap maliyeti
Telefon doğrulaması zorunludur. Aynı telefon numarası tek hesaba bağlanır. Yeni hesabın ilk 7 günü boyunca günlük tavan 30 puana düşer.

### Katman 2 — Teslim doğrulaması
Kod alan tarafta üretilir, paylaşan tarafta girilir. Kod tek kullanımlıktır, 72 saat geçerlidir, hash'lenmiş saklanır, 5 hatalı denemeden sonra teslim `EXPIRED` olur.

### Katman 3 — Risk sinyalleri
Onay anında değerlendirilir; sonuç `Handover.risk_flags` alanına yazılır.

| Sinyal | Açıklama |
|---|---|
| `SAME_DEVICE` | İki tarafın cihaz parmak izi aynı |
| `SAME_IP` | Onay ve kod alma aynı IP'den |
| `LOCATION_MISMATCH` | Onay konumu, ilanın yaklaşık noktasından 5 km'den uzak |
| `REPEATED_PAIR` | Aynı kullanıcı çifti son 30 günde 2'den fazla teslim yaptı |
| `RAPID_SEQUENCE` | Aynı kullanıcı son 1 saatte 3'ten fazla teslim onayladı |
| `NEW_ACCOUNT_PAIR` | İki hesap da 7 günden yeni |
| `LOW_TRUST` | Taraflardan birinin güven skoru 30'un altında |

Tek bir sinyal engelleme sebebi değildir — aynı evden iki kişi meşru olarak aynı IP'yi kullanabilir. **İki veya daha fazla sinyal** birlikte geldiğinde teslim `PENDING_REVIEW` olur ve puan moderatör onayına kadar verilmez.

### Katman 4 — Güven skoru

```
score = 50
      + min(35, tamamlanan_teslim × 2)
      + min(15, hesap_yaşı_ay × 2)
      − no_show_sayısı × 5
      − doğrulanmış_şikayet × 15
```
Sonuç 0–100 arasına sıkıştırılır; formülün teorik tavanı tam 100'dür (50 + 35 + 15). 30'un altı risk sinyali üretir; 15'in altı ilan açmayı ve talep göndermeyi engeller.

### Katman 5 — Geri alma
Moderatör sahteciliği doğrularsa `ADJUSTMENT` ile puan geri alınır. Bakiye yetersizse hesap negatife düşürülmez; bunun yerine `USER:{id}` hesabına bir **borç kaydı** (`pending_clawback`) tutulur ve sonraki kazanımlardan mahsup edilir. Tekrarlayan sahtecilikte hesap `SUSPENDED` olur.

### Kabul edilen kalan risk
Fiziksel buluşmayı gerçekten yapan, iki ayrı cihaz ve iki ayrı telefon numarası kullanan kararlı bir kötüye kullanıcı sistemi yine de kandırabilir. Tavanlar bu durumda kazancı ekonomik olmayan seviyede tutar (aylık en fazla 1200 puan). Daha ağır önlemler (kimlik doğrulama, fotoğraflı teslim kanıtı) meşru kullanıcıyı da yavaşlatacağı için MVP'de tercih edilmemiştir.

## Harcama tarafı

### Partner API sözleşmesi

Partner kurumun sağlaması gerekenler:

```
POST {partner.api_endpoint}/vouchers
Headers: Authorization: Bearer <partner-token>
         Idempotency-Key: <redemptionId>
Body:    { "externalUserRef": "...", "rewardCode": "...", "amount": 250 }

200 { "voucherCode": "...", "validUntil": "..." }
409 { "code": "already_processed", "voucherCode": "..." }   → aynı anahtar
4xx { "code": "rejected", "reason": "..." }
```

- **İdempotency zorunludur.** Aynı `Idempotency-Key` ile tekrar çağrıldığında partner yeni voucher üretmemeli, mevcut olanı dönmelidir.
- Durum sorgusu için `GET /vouchers/{idempotencyKey}` gereklidir (anahtarın değeri `Redemption.id`'dir); zaman aşımı sonrası uzlaştırma bunu kullanır.
- GreenTrack tarafında kullanıcı kimliği partnere gönderilmez; her partner için türetilmiş, geri çevrilemez bir `externalUserRef` kullanılır.

### Hata ve iade

| Durum | Davranış |
|---|---|
| Partner 4xx | `RELEASE`, puan anında iade, kullanıcıya sebep bildirimi |
| Partner zaman aşımı | `Redemption.status=FAILED`, hold korunur; uzlaştırma işi 1 dk aralıklarla durum sorgular |
| Uzlaştırma 15 dk içinde sonuç alamadı | `RELEASE`, kullanıcıya bildirim |
| Voucher üretildi ama kullanıcıya iletilemedi | `CAPTURED` kalır, voucher `/redemptions/{id}` üzerinden tekrar okunabilir |
| Kullanıcı iptal talebi | Voucher kullanılmamışsa partner iptali + `ADJUSTMENT` ile iade; kullanılmışsa iade yok |

### Ödül stoğu
`Reward.stock` hold anında düşürülür, `RELEASE` durumunda geri artırılır. Stok kontrolü ve hold aynı transaction içindedir.
