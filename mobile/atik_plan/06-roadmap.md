# Yol Haritası

Fazlar özellik listesi değil, **çalışan dilim** olarak tanımlanmıştır. Her faz sonunda sistem uçtan uca kullanılabilir olmalıdır.

## Faz 1 — MVP: doğrulanmış paylaşım döngüsü

**Kapsam:** `identity`, `catalog`, `matching`, `handover`, `ecocoin` (yalnızca kazanım), `notification` (temel push), `moderation` (temel kuyruk), `wasteai` (yalnızca stub adapter).

| İş | Not |
|---|---|
| Proje iskeleti | Spring Boot 3, modül paket yapısı, ArchUnit ile bağımlılık kuralı testi |
| Veritabanı | PostgreSQL + PostGIS, Flyway ile şema başına migration |
| Telefon + OTP girişi | JWT + dönen refresh token |
| Mahalle verisi | En az bir ilçe için `Neighborhood` poligonları yüklenir |
| Kategori ağacı | Sabit başlangıç seti + `coin_multiplier` değerleri |
| İlan yaşam döngüsü | Presigned URL ile foto, jitter'lı `approx_point`, yayın/geri çekme |
| Yakınlık araması | `ST_DWithin` + cursor sayfalama |
| Talep ve rezervasyon | Koşullu UPDATE ile çakışma koruması, 48 saat rezervasyon |
| Teslim doğrulama | Tek kullanımlık kod, hash, TTL, deneme sayacı |
| Outbox + dispatcher | `HandoverConfirmed` ve `CoinsGranted` uçtan uca çalışır |
| Eco-Coin kazanımı | Double-entry ledger, tavanlar, temel risk sinyalleri |
| Zamanlanmış işler | Rezervasyon ve ilan süre dolumu, gece ledger denetimi |
| Basit moderasyon | Şikayet kaydı + `PENDING_REVIEW` kuyruğu için minimal panel |
| `wasteai` stub | `WasteClassifier` arayüzü + kural tabanlı stub; `AiClassification` kayıtları baştan birikmeye başlar |

**Biten tanımı**
- Bir kullanıcı ilan açıp başka bir kullanıcı bunu bulup alabiliyor; teslim kodla doğrulanıyor ve puan cüzdana düşüyor.
- Ledger denetim işi sıfır sapma raporluyor.
- ArchUnit modül bağımlılık testi yeşil.
- Aynı `HandoverConfirmed` event'i iki kez işlendiğinde puan bir kez veriliyor (test edilmiş).
- İlan detayında `exact_point` yetkisiz kullanıcıya dönmüyor (test edilmiş).

**Kapsam dışı:** rozet, seviye, görev, sıralama, ödül harcaması, gerçek AI modeli (yalnızca stub var), çoklu şehir.

## Faz 2 — Alışkanlık ve karşılık

**Kapsam:** `gamification`, `recycling`, `ecocoin` harcama tarafı.

| İş | Not |
|---|---|
| Rozet motoru | `Badge.rule` jsonb ile veri odaklı kurallar, kod değişikliği gerektirmez |
| Seviye | Toplam kazanılan puandan türetilir |
| Aylık görevler | Ay başı üretimi, ay sonu kapanışı, görev ödülü `grant` |
| Mahalle sıralaması | Saatlik batch → `LeaderboardSnapshot` |
| Toplama noktaları | Belediye açık verisinden içe aktarma, `accepted_types` etiketleme |
| "Uygun değil" yönlendirmesi | Kategori bazlı en yakın nokta akışı |
| Partner entegrasyonu | HOLD/CAPTURE/RELEASE, uzlaştırma işi, ilk partner ile canlı test |
| Ödül kataloğu | Stok yönetimi, geçerlilik tarihleri |

**Biten tanımı**
- Kullanıcı kazandığı puanı gerçek bir partnerde harcayıp voucher alabiliyor.
- Partner zaman aşımı senaryosunda uzlaştırma işi hold'u doğru şekilde CAPTURE veya RELEASE ediyor (kaos testi ile doğrulanmış).
- Sıralama sayfası 100 ms altında yanıt veriyor (snapshot'tan okuma).
- En az 20 rozet ve 3 aylık görev tanımlı.

## Faz 3 — Otomasyon ve ölçek

**Kapsam:** `wasteai` gerçek model, moderasyon otomasyonu, çoklu şehir.

| İş | Not |
|---|---|
| Görüntü sınıflandırma | Faz 1–2'de biriken `AiClassification` + kullanıcı düzeltmeleri eğitim verisi olur |
| Model servisi | Ayrı serviste, port-adapter arkasında; timeout ve devre kesici |
| Öneri kalitesi ölçümü | `accepted_by_user` oranı ana metrik; %70'in altındaysa öneri gösterilmez |
| Otomatik içerik denetimi | Uygunsuz görsel ön filtreleme, şüpheli metin işaretleme |
| Risk skorlama | Kural tabanlı sinyallerin üstüne öğrenen bir skor; kararı yine moderatör verir |
| Çoklu şehir | Mahalle verisi ölçeklenmesi, arama sorgusu için materyalize görünüm değerlendirmesi |

**Biten tanımı**
- Fotoğraf yüklendiğinde kategori önerisi %70+ kabul oranıyla geliyor.
- "Paylaşıma uygun mu" önerisi hatalı olduğunda kullanıcı tek dokunuşla düzeltebiliyor ve düzeltme kayda giriyor.
- Moderasyon kuyruğu manuel inceleme süresi Faz 2'ye göre yarıya iniyor.

## Metrikler

Fazlar arası karar bu sayılara bakılarak verilir.

| Metrik | Tanım | Hedef |
|---|---|---|
| **Teslim oranı** | Doğrulanan teslim / yayınlanan ilan | Faz 1 sonu ≥ %35 |
| **Yeniden kullanım hacmi** | Aylık doğrulanan teslim sayısı | Faz 2'de aydan aya büyüme |
| **Tekrar eden kullanıcı** | Ay içinde ≥ 2 teslim yapan kullanıcı oranı | Faz 2 sonu ≥ %30 |
| **Aktif mahalle** | Ayda ≥ 10 teslim olan mahalle sayısı | Faz 2 sonu ≥ 15 |
| **Sahte teslim oranı** | Doğrulanmış sahtecilik / toplam teslim | ≤ %1 |
| **İnceleme yükü** | `PENDING_REVIEW` oranı | ≤ %5 (yüksekse risk kuralları fazla hassas) |
| **Puan dönüşümü** | Harcanan / kazanılan Eco-Coin | Faz 2 sonu ≥ %25 — düşükse ödüller cazip değil |
| **İlan yaşam süresi** | Yayından teslime geçen medyan süre | ≤ 5 gün |
| **AI kabul oranı** | `accepted_by_user = true` oranı | Faz 3 ≥ %70 |

İki metrik özellikle erken uyarı verir: **inceleme yükü** yükseliyorsa anti-fraud kuralları meşru kullanıcıyı yakalıyordur; **puan dönüşümü** düşükse Eco-Coin ekonomisi anlamsızlaşmıştır ve partner tarafına yatırım gerekir.
