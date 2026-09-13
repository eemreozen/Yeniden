# Yol Haritası (Roadmap)

Fazlar özellik listesi değil, **çalışan dilim** olarak tanımlanmıştır. Her faz sonunda sistem uçtan uca kullanılabilir olmalıdır.

## Faz 1 — MVP: Doğrulanmış paylaşım döngüsü ve Mikroservis İskeletleri (Tamamlandı)

**Kapsam:** `identity-service`, `catalog-service`, `exchange-service`, `ecocoin-service` (kazanım), `notification-service` (temel push), `moderation-service` (temel kuyruk), `wasteai-service` (stub adapter).

| İş | Not |
|---|---|
| Proje iskeleti | Spring Boot 3 + Java 21, Maven Multi-Module kök POM ve Maven Wrapper |
| Veritabanı | PostgreSQL + PostGIS, Docker Compose, şema izolasyonlu init-db.sql |
| Telefon + OTP girişi | JWT + dönen refresh token (`identity-service`) |
| Mahalle verisi | En az bir ilçe için `Neighborhood` poligonları yüklenir |
| Kategori ağacı | Sabit başlangıç seti + `coin_multiplier` değerleri (`catalog-service`) |
| İlan yaşam döngüsü | Presigned URL ile foto, jitter'lı `approx_point`, yayın/geri çekme |
| Yakınlık araması | `ST_DWithin` + cursor sayfalama (`catalog-service`) |
| Talep ve rezervasyon | Koşullu UPDATE ile çakışma koruması, 48 saat rezervasyon (`exchange-service`) |
| Teslim doğrulama | Tek kullanımlık 6 haneli kod, hash, TTL, deneme sayacı (`exchange-service`) |
| Outbox / Event Broker | `HandoverConfirmed` ve `CoinsGranted` uçtan uca çalışır |
| Eco-Coin kazanımı | Double-entry ledger, tavanlar, temel risk sinyalleri (`ecocoin-service`) |
| Zamanlanmış işler | Rezervasyon ve ilan süre dolumu, gece ledger denetimi |
| Basit moderasyon | Şikayet kaydı + `PENDING_REVIEW` kuyruğu (`moderation-service`) |
| `wasteai-service` | SOLID Çoklu Sağlayıcı mimarisi (Remote Vision API + Local Ollama/Qwen2-VL + RuleBasedFallback), token logprob güven hesabı, %75 eşikli moderasyon yönlendirmesi |
| `api-gateway` | Spring Cloud Gateway (Port 8080) reaktif yönlendirme kapısı |
| `redis` | Redis 7 (Port 6379) OTP önbellekleme ve TTL doğrulama altyapısı |
| `rabbitmq` | RabbitMQ (Port 5672/15672) Asenkron olay tabanlı iletişim altyapısı (`HandoverConfirmedEvent`) |

**Biten tanımı**
- Tek bir `docker compose up` komutuyla tüm mikroservisler ve veritabanı sorunsuz ayağa kalkıyor.
- Bir kullanıcı ilan açıp başka bir kullanıcı bunu bulup alabiliyor; teslim kodla doğrulanıyor ve puan cüzdana düşüyor.
- Ledger denetim işi sıfır sapma raporluyor.
- Aynı `HandoverConfirmed` event'i iki kez işlendiğinde puan bir kez veriliyor (test edilmiş).
- İlan detayında `exact_point` yetkisiz kullanıcıya dönmüyor (test edilmiş).
- `wasteai-service` atık fotoğraflarını çoklu sağlayıcı failover mimarisiyle sınıflandırıp %75 güven kontrolüne göre moderasyon kuyruğuna iletebiliyor (test edilmiş).

**Kapsam dışı:** rozet, seviye, görev, sıralama, ödül harcaması, çoklu şehir.

## Faz 2 — Alışkanlık ve karşılık

**Kapsam:** `gamification-service`, `recycling`, `ecocoin-service` harcama tarafı.

| İş | Not |
|---|---|
| Rozet motoru | `Badge.rule` jsonb ile veri odaklı kurallar (`gamification-service`) |
| Seviye | Toplam kazanılan puandan türetilir |
| Aylık görevler | Ay başı üretimi, ay sonu kapanışı, görev ödülü `grant` |
| Mahalle sıralaması | Saatlik batch → `LeaderboardSnapshot` |
| Toplama noktaları | Belediye açık verisinden içe aktarma, `accepted_types` etiketleme |
| "Uygun değil" yönlendirmesi | Kategori bazlı en yakın nokta akışı |
| Partner entegrasyonu | HOLD/CAPTURE/RELEASE, uzlaştırma işi |
| Ödül kataloğu | Stok yönetimi, geçerlilik tarihleri |

## Faz 3 — Otomasyon ve ölçek

**Kapsam:** `wasteai-service` gerçek model, moderasyon otomasyonu, çoklu şehir.

| İş | Not |
|---|---|
| Görüntü sınıflandırma | Faz 1–2'de biriken `AiClassification` + kullanıcı düzeltmeleri eğitim verisi olur |
| Model servisi | Ayrı serviste, port-adapter arkasında; timeout ve devre kesici |
| Öneri kalitesi ölçümü | `accepted_by_user` oranı ana metrik |
| Çoklu şehir | Mahalle verisi ölçeklenmesi |

## Metrikler

| Metrik | Tanım | Hedef |
|---|---|---|
| **Teslim oranı** | Doğrulanan teslim / yayınlanan ilan | Faz 1 sonu ≥ %35 |
| **Yeniden kullanım hacmi** | Aylık doğrulanan teslim sayısı | Faz 2'de aydan aya büyüme |
| **Tekrar eden kullanıcı** | Ay içinde ≥ 2 teslim yapan kullanıcı oranı | Faz 2 sonu ≥ %30 |
| **Aktif mahalle** | Ayda ≥ 10 teslim olan mahalle sayısı | Faz 2 sonu ≥ 15 |
| **Sahte teslim oranı** | Doğrulanmış sahtecilik / toplam teslim | ≤ %1 |
| **İnceleme yükü** | `PENDING_REVIEW` oranı | ≤ %5 |
| **Puan dönüşümü** | Harcanan / kazanılan Eco-Coin | Faz 2 sonu ≥ %25 |
| **İlan yaşam süresi** | Yayından teslime geçen medyan süre | ≤ 5 gün |
| **AI kabul oranı** | `accepted_by_user = true` oranı | Faz 3 ≥ %70 |
