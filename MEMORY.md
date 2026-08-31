# YENİDEN — Proje hafızası

## Güncel durum — 2026-08-30 SMSGate uygulama dilimi

Kullanıcı SMSGate seçti ve uygulamayı başlattı. Aşağıdaki 1–11 bölümler **başlangıç incelemesinin tarihsel fotoğrafıdır**; bu güncel bölüm, kod ve [task listesi](docs/IDENTITY_TASKS.md) artık mevcut durumun kaynağıdır.

Uygulanan ilk dilim:

- Java21/Maven3.9.8 Unix `mvnw` başlatıcısı, SHA-512 doğrulaması; bağımlılıklar sürüm yükseltmeden eklendi. Parent test dependency test scope'unda; common test kütüphanelerini runtime'a taşımıyor. Surefire 3.2.5 sabit.
- Yeni DB için Flyway V1: users/user_roles/trust_scores/auth_sessions/refresh_tokens; `ddl-auto=validate`, OSIV kapalı. Eski dolu şemaya otomatik baseline/dönüşüm yok.
- User ve TrustScore zamanları Instant; servis zamanları Clock. Varsayılan USER rolü. Yeni kullanıcı/trust/phoneVerifiedAt/session aynı DB transaction'ında oluşur.
- TR mobil normalizasyonu; SecureRandom 6 hane, HMAC proof ve telefon/IP pseudonymous Redis anahtarları, challenge UUID. Lua ile atomik reserve/activate/consume/invalidate. 180s TTL, 60s cooldown, 5 deneme; kayan saatte 3/telefon,10/IP. Gönderim kabulünden önce pending kod doğrulanamaz.
- SMSGate Local Server Basic Auth POST /message; varsayılan kapalı, açık alıcı allowlist, yerel HTTP opt-in, timeout/redirect/proxy kısıtları. 2xx kabul; teslim garantisi yok. Hata/timeout yeni challenge'ı yanlışlıkla silmez, OTP/kredensiyal loglanmaz.
- RS256 JWT <=15m; sub/sid/roles/nbh/iss/aud/iat/exp doğrulama. Refresh 256bit, SHA-256 saklama, rotating, mutlak 30gün. Consumed token geçmişi tutulur; refresh replay tüm kullanıcı session'larını iptal eder.
- Auth mutasyonları önce user row lock alır. Refresh/logout REQUIRES_NEW; replay reddi iptal commit'inden SONRA fırlatılır. Gelecek suspend/delete/rol mutasyonları aynı lock sırasını kullanmalı.
- Identity her Bearer isteğinde DB session/hesap durumu kontrol eder. Logout/replay/expiry/suspend sonrasında mevcut JWT de reddedilir. Bu, diğer servislerin auth açığını kendiliğinden kapatmaz.
- Yeni `/api/v1/auth/otp/request`, `/otp/verify`, `/refresh`, `/logout`; `/api/v1/users/me` GET/PATCH ve public `/users/{UUID}`. Eski doğrulamasız register, kod dönen OTP ve telefon sorgusu KALDIRILDI; alias yok. Gateway auth/users yollarını yönlendirir, IDENTITY_PORT ayarlanabilir.
- Private/public DTO ayrımı; PATCH yalnız displayName/avatarKey, bilinmeyen alanlar 400. ProblemDetail + lowercase code/gerçek status; 429 Retry-After, credentials no-store.

Doğrulama kanıtı:

- Başlangıç commit'inin ayrı snapshot'ında Java21 ile 18 test geçti.
- Reactor `package`: identity 175 (29 gerçek PostgreSQL/Redis testi dahil), diğer servisler 14; toplam **189 test**, 0 failure/error/skipped. Tüm modüller paketlendi. Kapsama yüzdesi ölçülmedi.
- Ayrı `GatewayIdentitySmokeIT`: **14 test** (13 tekrar edilen direct regresyon + 1 gerçek gateway HTTP giriş/private-public profil/refresh/logout akışı), 0 failure/error/skipped. SMS sender test double; gerçek cihaz teslimi DEĞİL.
- Unix bootstrap temiz geçici cache ile indirildi, SHA-512 doğrulandı, Maven/Java sürümü kontrol edildi. Testcontainers geçici verileri kullandı; mevcut uygulama volume'ları silinmedi. Gerçek SMS gönderilmedi, commit/push/deploy yapılmadı.
- Çalıştırma ve tekrar üretme: [IDENTITY_RUNBOOK.md](docs/IDENTITY_RUNBOOK.md); HTTP koleksiyonu: [identity.http](docs/identity.http). Ortamın varsayılan Java17'si değiştirilmedi; komutlarda JAVA_HOME Java21 verildi. Colima testler için başlatıldı.

Sınırlar/sonraki işler:

- Android/SIM kurulumu ve izinli numarada gerçek SMS provası kullanıcı cihaz bilgilerini gerektirir. Secret'lar sohbet/Git'e yazılmaz. SMSGate operatör SMS ücretini kaldırmaz.
- Mahalle/PostGIS veri ataması, trust formül/events, silme/outbox/bileşik profil tamamlanmadı. Başlangıç trust50 vardır; tüm domain bitmiş sayılmaz.
- Proxy IP güveni kasıtlı kapalı: remoteAddr kullanılır, gateway arkasında 10/IP kotası ortak olabilir. Gerçek çok kullanıcılı dağıtıma geçmeden güvenilen proxy tasarımı gerekir.
- Local profil geçici RSA ve ayrı issuer kullanır; prod explicit HTTPS issuer+PEM çiftini, ayrı OTP namespace ve secret'ı gerektirir. Üretim anahtar rotasyonu/JWKS, refresh-history retention ve DB rol izolasyonu ayrıca yapılacak.
- Redis consume ile DB tek transaction değildir. DB hatasında kullanıcı/session rollback, kod tüketilmiş kalır; yeni challenge gerekir. Gönderim hatası kotayı sıfırlamaz.
- V1 boş şema içindir. Mevcut veri yükseltme migration'ı/telefon çakışması/timestamp dönüşümü yapılmadı; mevcut DB'de Flyway'ı zorla baseline etme.

---

Son inceleme: 2026-08-30. İncelenen commit: `4e01efe` (`main`).
Depo: https://github.com/eemreozen/Yeniden
Yerel depo kökü: `/Users/bekir/yeniden/Yeniden` — üstteki `/Users/bekir/yeniden` depo kökü değildir.

## 1. Çalışma kapsamı ve kanıt sınırı

Kullanıcı `identity-service` üzerinde çalışmak istiyor. İlk görev: projeyi ve `docs/` klasörünü detaylı incelemek, memory/rules oluşturmak ve yapılacakları anlatmak. Bu aşamada uygulama kodu, mevcut yapılandırma veya bağımlılık değiştirilmedi.

`docs/` altındaki 10 dosyanın tamamı (1.569 satır), kök README, bütün servislerin Java kaynakları ve mevcut testleri, POM'lar, gateway/servis ayarları, Compose, DB başlangıç betiği ve wrapper incelendi. Aşağıdaki mevcut durum bulguları statik kaynak incelemesine dayanır. Servisler başlatılmadı; Maven testleri, build ve canlı uçtan uca akış çalıştırılmadı. README'deki başarı/kapsama iddiaları doğrulanmış kabul edilmez.

## 2. Ürün ve iş akışı

YENİDEN mahalle ölçeğinde ücretsiz eşya/malzeme paylaşımı ve geri dönüşüm platformudur. Aktörler: paylaşan, alan, partner kurum, moderatör ve sistem. Paylaşan/alan ayrı kalıcı yetki rolleri değildir; kullanıcının işlemdeki tarafını anlatır.

Hedef ana akış:

1. Kullanıcı telefonunu doğrular, profilini tamamlar ve mahalleye bağlanır.
2. Paylaşan fotoğraflı ilan açar; AI sınıflandırma önerisi isteğe bağlıdır.
3. Alan yakınındaki ilanı bulur ve talep gönderir.
4. Paylaşan kabul eder; ilan rezerve edilir.
5. Alanın gördüğü teslim kodunu paylaşan girer; süre, yetki ve risk kontrolleri yapılır.
6. Onaylanan teslim olayı paylaşana Eco-Coin kazandırır; oyunlaştırma ve bildirim asenkron ilerler.
7. İleriki kapsamda puan partner ödüllerinde harcanır. Paylaşıma uygun olmayan atık toplama noktasına yönlendirilir; bu yönlendirme puan üretmez.

Kimlik servisi bu döngünün güven temelidir: doğrulanmış tekil telefon, güvenilir kullanıcı kimliği, hesap durumu, mahalle ve güven skoru sağlar. Eco-Coin hesabını veya teslimat durum makinesini sahiplenmez.

## 3. Gerçekte bulunan mimari

Java 21 hedefli Maven multi-module depo. Spring Boot `3.3.3`, Spring Cloud `2023.0.3`, Lombok `1.18.34`; sürümler kök `pom.xml` içindedir. Bunlar depoda kayıtlı sürümlerdir, güncellik/güvenlik değerlendirmesi yapılmamıştır.

| Modül | Port / veri | Mevcut uygulama özeti |
|---|---|---|
| `api-gateway` | 8080 | WebFlux/Netty; servis önekli yolları yönlendirir. JWT doğrulama veya güvenlik filtresi yok. |
| `identity-service` | 8081 / `identity` | Telefonla kullanıcı bul/oluştur, başlangıç güven skoru, kullanıcı sorguları ve bağımsız Redis OTP demosu. |
| `catalog-service` | 8082 / `catalog` | Taslak oluşturma/yayınlama/listeleme; yaklaşık koordinatlar ve dikdörtgen alan sorgusu. Gerçek `ST_DWithin` sorgusu veya cursor sayfalama yok. |
| `exchange-service` | 8083 / `exchange` | Talep, kabul, kod oluşturma/doğrulama ve RabbitMQ yayınlama. Kullanıcı kimlikleri isteklerden alınıyor. Catalog rezervasyon entegrasyonu ve identity risk kontrolü yok. |
| `ecocoin-service` | 8084 / `ecocoin` | Çift entry yazma, cüzdan sütununda bakiye ve günlük/aylık sayaçlar; teslim olayını tüketir. |
| `gamification-service` | 8085 / `gamification` | HTTP ile puan/rozet ekleme ve okuma; seviye `(puan / 100) + 1`. Event listener yok. |
| `wasteai-service` | 8086 / DB kullanmıyor | URL metninde cam/elektronik kelimelerini arayan stub; gerçek görüntü analizi yok. |
| `yeniden-common` | kütüphane | `ApiResponse`, `BaseException`, `HandoverConfirmedEvent`. Web, Redis, AMQP ve test starter bağımlılıklarını servislerin classpath'ine taşır. |

`notification-service` ve `moderation-service` dokümanlarda hedefleniyor, ancak Maven modülleri ve uygulama kodları yok. `recycling` işlevleri de uygulanmış değil. Frontend/mobil istemci bu depoda bulunmuyor.

Compose: 7 uygulama + PostgreSQL/PostGIS, Redis, RabbitMQ = 10 konteyner tanımı. PostgreSQL veritabanı `yeniden_db`. Beş servis fiilen şema kullanıyor; `docker/init-db.sql` ek olarak notification, moderation ve wasteai dahil toplam sekiz şema oluşturuyor. Tablolar migration ile değil `ddl-auto: update` ile oluşuyor. Mahalle/kategori seed verisi yok.

Şema izolasyonu hedefi uygulama seviyesinde; servisler aynı `postgres` kullanıcısını kullandığı için DB yetkileriyle erişim izolasyonu henüz sağlanmış değil. Servis portları host'a da açılmış; gateway'i tek güvenlik sınırı varsayamayız.

Mevcut olay hattı:

`exchange-service → yeniden.exchange / handover.confirmed → ecocoin.handover.queue → ecocoin-service`

`HandoverConfirmedEvent` alanları: `handoverId`, `requestId`, `listingId`, `providerId`, `receiverId`, `earnedPoints`, `confirmedAt`. Yayıncı sabit 25 puan gönderiyor. `eventId`, `reviewRequired`, kategori ve miktar alanları yok. Outbox yok; yayıncı ve tüketici hataları yakalayıp devam ediyor. Bu yapı güvenilir yeniden teslim/işleme garantisi olarak kabul edilmemeli.

## 4. Doküman haritası ve çıkarımlar

| Kaynak | İçerik ve identity açısından önemi |
|---|---|
| [00-overview.md](docs/00-overview.md) | Ürün, aktörler, ana değer akışı, güvenlik/gizlilik hedefleri. Diyagram gelecekteki servisleri de içeriyor. |
| [01-modules.md](docs/01-modules.md) | Servis sahiplikleri, OTP zorunluluğu, mahalle ataması, güven skorunun tüketicileri, event sözleşmeleri. |
| [02-domain-model.md](docs/02-domain-model.md) | `User`, `Neighborhood`, `TrustScore`; FK sınırları, durum makineleri, konum gizliliği ve indeksler. |
| [03-flows.md](docs/03-flows.md) | İlan, arama, teslim, iptal/süre dolumu, toplama noktası ve ödül harcama akışları. Identity için bağımlılık bağlamı sağlar. |
| [04-ecocoin-rules.md](docs/04-ecocoin-rules.md) | Güven skoru formülü/eşikleri, telefon tekilliği, yeni hesap riski, ödül ve anti-fraud kuralları. |
| [05-api.md](docs/05-api.md) | OTP giriş, JWT, rotating refresh, profil/konum/silme uçları; hata formatı ve rate limit. |
| [06-roadmap.md](docs/06-roadmap.md) | Fazlar ve biten tanımı. “Faz 1 tamamlandı” etiketi mevcut kodla doğrulanmıyor. |
| [07-decisions.md](docs/07-decisions.md) | ADR-001–007: mikroservis, şema izolasyonu, event tabanlı yan etkiler, ledger, teslim kodu, AI adapter ve konum gizliliği. |
| [PROJECT_GUIDE.md](docs/PROJECT_GUIDE.md) | Katmanlar, interface tabanlı servisler, constructor injection, `BaseException`, DTO ve test ilkeleri. Kurulum örnekleri Windows odaklı. |
| [DATABASE_ARCHITECTURE.md](docs/DATABASE_ARCHITECTURE.md) | SQL/Redis taslakları, OTP 180 saniye. Bazı tablo/alan adları ve kısıtlar gerçek entity'lerle uyuşmuyor. |

Dokümanlar tek ve tutarlı bir uygulanmış şartname değil. Hedef ürün davranışını anlamak için kullanılır; mevcut davranış kaynak koddan, çalışan davranış ise test/çalıştırma kanıtından belirlenir. Çelişkiler sessizce giderilmiş sayılmaz.

## 5. Identity-service mevcut durum

Paket: `com.yeniden.identity`; katmanlar `controller`, `dto`, `service`, `repository`, `domain`.

- `User`: UUID, unique `phone_e164`, `phoneVerifiedAt`, email, displayName, avatarKey, neighborhoodId, status, createdAt, updatedAt. Telefon normalizasyonu/doğrulaması yok; `phoneVerifiedAt` mevcut akışta atanmıyor. `home_point` ve rol modeli yok.
- `TrustScore`: userId, score, completedHandovers, noShowCount, reportCount, updatedAt. Yeni kullanıcıda 50 oluşturuluyor; entity callback'i yalnızca skoru 0–100'e sıkıştırıyor. Formül, sayaç güncellemesi, zamanla yeniden hesaplama veya event tüketimi yok.
- `UserServiceImpl.registerOrLogin`: telefon eşleşirse kullanıcıyı döndürür, yoksa kullanıcı + trust kaydı oluşturur. OTP kanıtı istemiyor; `SUSPENDED`/`DELETED` kontrolü yok. Bu işlem token üretmediği için gerçek oturum açma değildir.
- `OtpController`: `Random`, düz metin Redis değeri, `OTP:{phone}`, 180 saniye TTL. Kodu HTTP yanıtında döndürür. Başarılı doğrulamada anahtarı siler ama okuma/karşılaştırma/silme atomik değildir. SMS göndermez, kullanıcı kaydını doğrulamaz ve token üretmez. Rate limit ve hatalı deneme sayacı yok.
- `UserDto`: phone, email, neighborhoodId ve hesap durumu dahil tüm sorgularda aynı DTO kullanılıyor; public/private profil ayrımı yok.
- `UserServiceTest`: 4 Mockito unit testi. Yeni/mevcut kullanıcı ve ID ile bulma/bulunamama senaryoları var. OTP, auth, HTTP güvenliği, Redis, DB ve eşzamanlılık testleri yok.

Mevcut yolların ortak öneki `/api/v1/identity`:

| Metot | Yol | Mevcut davranış |
|---|---|---|
| GET | `/health` | Sabit sağlık mesajı; bağımlılık sağlık testi değil. |
| POST | `/auth/register` | JSON `phone`, `displayName`, `email` ile kullanıcı bul/oluştur. |
| GET | `/users/{id}` | Telefon/e-posta dahil `UserDto`. |
| GET | `/users/phone/{phone}` | Telefonla kullanıcı sorgulama; koruma yok. |
| POST | `/otp/send` | Query param `phone`; OTP'yi yanıt içinde döndürür. |
| POST | `/otp/verify` | Query param `phone`, `code`; boolean sonuç, token yok. |

Doğrulanan başlıca eksikler: Spring Security/JWT, refresh oturum modeli, roller, request validation, merkezi exception handler, public DTO, profil güncelleme/silme, mahalle entity/verisi, güven skoru hesabı, SMS adapter, migration ve abuse kontrolleri.

## 6. Dokümanlardan çıkan identity hedefleri

### Kimlik ve oturum

- Telefon SMS OTP ile zorunlu doğrulanır; E.164 olarak tekilleştirilir. E-posta opsiyoneldir; parola ile giriş tanımlanmamıştır.
- OTP doğrulaması yeni kullanıcıyı oluşturur veya mevcut kullanıcı için giriş yapar; `{accessToken, refreshToken, isNewUser}` döner.
- Access JWT: 15 dakika. Alanlar `sub`, `nbh`, `roles`, `exp`; bakiye/seviye gibi değişen veriler eklenmez.
- Roller: `USER`, `MODERATOR`, `ADMIN`. İlk kayıt kendiliğinden ayrıcalıklı rol vermez.
- Refresh: 30 gün, DB'de hash, her kullanımda rotasyon. Kullanılmış token yeniden sunulursa **o kullanıcının tüm oturumları** iptal edilir; sadece tek cihaz/token ailesiyle sınırlanmaz.
- Logout mevcut refresh oturumunu iptal eder. Aktif access token'ın anlık iptal stratejisi dokümanda belirtilmemiştir.
- OTP istek limiti: numara başına 3/saat, IP başına 10/saat; 429 ve `Retry-After`. OTP doğrulama deneme sınırı belirtilmemiştir. Teslim kodunun 5 deneme kuralı OTP'ye otomatik aktarılmaz.

### Profil, mahalle ve gizlilik

Hedef yollar `/api/v1` altında: `GET/PATCH/DELETE /users/me`, `PUT /users/me/location`, `GET /users/{id}`.

- Kişinin kendi profili ile halka açık profil farklı sözleşmelerdir. Public profilde telefon, e-posta ve konum bulunmamalı.
- Mahalle: `Neighborhood(id, name, district, city, boundary, centroid)`; yaklaşık konumdan atama, en az bir ilçenin poligon verisi hedefleniyor.
- Kullanıcı açık adresi saklanmaz. İlan başına ~250 m sabit kaydırma kuralı catalog'a aittir; profil konumu algoritmasının ayrıntıları ayrıca belirlenmeli.
- `/users/me` bakiyeyi/seviyeyi; public profil rozet/seviyeyi de hedefliyor. Bunlar identity verisi değil. Birleştirme katmanı veya event ile beslenen okuma modeli kararı eksik.
- `notificationPrefs` notification sorumluluğuyla kesişiyor; servis henüz yok.
- Hesap silme: identity hesabı/oturumları, ilan kaldırma ve ledger anonimleştirme birlikte düşünülmeli. Başka şemaya doğrudan silme/yazma yapılamaz; servisler arası sözleşme gerekli.

### Güven skoru

Kaynak: `docs/04-ecocoin-rules.md`, Katman 4.

```text
score = clamp(0, 100,
    50
    + min(35, completedHandovers * 2)
    + min(15, accountAgeMonths * 2)
    - noShowCount * 5
    - verifiedReportCount * 15)
```

- Başlangıç 50; `<30` risk sinyali, `<15` ilan açmayı/talep göndermeyi engeller. Bu son engeller ilgili catalog/exchange iş akışlarında uygulanır.
- Tamamlanan teslim, no-show ve **doğrulanmış** şikayet olayları gerekir. Ham şikayet otomatik ceza sebebi değildir.
- Sayaçlar tekrar gelen event ile iki kez artmamalı; geçmişten yeniden hesaplanabilmeli. Hesap yaşı event gelmese de değiştiği için skor güncelleme stratejisi gerekli.
- Mevcut event kataloğu identity tüketimini, no-show ve doğrulanmış şikayet sözleşmelerini tanımlamıyor. Kesin event isimleri/yükleri henüz kararlaştırılmadı.
- İlk 7 gün günlük 30 Eco-Coin sınırı ecocoin'e aittir; identity hesap yaşı bilgisini sağlar. Identity puan basmaz veya cüzdan değiştirmez.

## 7. Çelişkiler ve entegrasyon riskleri

| Konu | Kanıt / fark | Geliştirme etkisi |
|---|---|---|
| API yolları | `05-api`: `/api/v1/auth/**`, `/api/v1/users/**`; kod/gateway: `/api/v1/identity/**` | Dış sözleşme ve geçiş/alias kararı birlikte yapılmalı. Öneri: dokümandaki yolları hedeflemek; mevcut uçları sessizce kırmamak. |
| Yanıt/hata | Kod `ApiResponse`; hedef RFC 7807 tarzı hata ve kararlı küçük harfli code | `BaseException` HTTP'ye çevrilmiyor; status alanı tek başına HTTP statüsünü ayarlamaz. Başarı zarfı kararı ayrı tutulmalı. |
| Trust kolonları | DB dokümanı `successful_handovers`, `no_shows`; entity/domain dokümanı `completed_handovers`, `no_show_count`, `report_count` | Migration kodla karşılaştırılmalı; DB dokümanı doğrudan DDL kaynağı sayılamaz. `user_id` için entity'de FK eşlemesi yok. |
| OTP | DB dokümanı `otp:`; kod `OTP:`; ikisi de düz kodu tarif ediyor | Mevcut Redis key değişimi, yeni güvenli saklama ve adapter davranışı açık tasarlanmalı. |
| Mahalle zamanı | Modül dokümanı kayıt sırasında mahalle; verify gövdesi yalnızca telefon/kod | İlk giriş ile onboarding ayrılmalı; mahalle seçilmeden `nbh` claim ve izin verilen işlemler belirlenmeli. |
| Servis sınırları | Profil bileşik veriler istiyor; ADR-003 oyunlaştırmaya senkron servis çağrısını yasaklıyor | Identity'den gamification'a doğrudan çağrı varsayılmamalı. BFF/istemci birleştirmesi veya event okuma modeli değerlendirilmeli. |
| Olay güvenilirliği | Event kimliği/review bayrağı/outbox yok; hatalar yutuluyor | Trust güncellemeleri için güvenilir event sözleşmesi ve ayrı consumer kuyruğu gerekir. Ecocoin kuyruğu identity ile paylaşılmaz. |
| Tamamlanma iddiaları | Roadmap “tamamlandı”, README “%100”; kodda büyük eksikler var | Mevcut test sayısı/kapsamı üzerinden konuş; test sonucu/kapsama uydurma. |
| Portlar | Eski diyagramda wasteai 8088; gerçek Compose/yml 8086 | Mevcut çalıştırma için 8086 esas; olmayan servisler eklenmiş sayılmaz. |
| Ledger/ödül | ADR bakiye sütununu yasaklıyor; kodda `wallet.balance` var. Hedef 8–40 puan formülü; yayıncı 25 gönderiyor | Diğer servis teknik borcu; identity görevinde yeniden yazılmayacak. |
| Teslim kodu | ADR hash-only; entity `raw_code_for_receiver` de saklıyor. DB dokümanı max 3, servis kontrolü 5 | Identity OTP tasarımına örnek alınmamalı. Hatalı deneme kaydı sonrası runtime exception transaction'ı geri alabilir; entegrasyon testi gerekir. |
| İş ödülleri | Eco-Coin metni “yalnızca teslim” derken aynı belgede görev ödülü de var | Ekonomi kapsamı ileride netleştirilmeli; identity bunu kendi kararıyla çözmez. |
| Asenkron yanıt | Teslim API örneği anlık coin/rozet içeriyor; tasarım bunları asenkron üretiyor | Profil/teslim ekranları için eventual consistency ve yanıt sözleşmesi kararı gerekir. |

## 8. Geliştirme sırası önerisi — henüz uygulanmadı

1. **Başlangıç ve sözleşme:** Java 21/build/test ortamını doğrula; test keşfini kontrol et. Dış API yolları, başarı/hata gövdeleri, OTP onboarding ve servis sınırlarını karara bağla. Sadece gereken identity/common/gateway değişikliklerini kapsamlandır.
2. **Veri ve güvenlik temeli:** Identity migration başlangıcı; User/TrustScore kısıtları, oturum/refresh ve rol modeli. DTO validation, merkezi hata dönüşümü, UTC zaman ve test edilebilir saat kaynağı. Mevcut veri varsa tahribatsız migration planı.
3. **Gerçek OTP giriş dilimi:** Telefon normalizasyonu, güvenli rastgele kod, Redis TTL ve atomik tüketim/deneme sayaçları, numara/IP limitleri, SMS gönderici arayüzü ve test adapter'i. Başarılı doğrulama → kullanıcı/telefon doğrulama kaydı → token çifti.
4. **Oturum yaşam döngüsü:** JWT doğrulama, refresh hash/rotasyon/tekrar kullanım tespiti, logout, askıya alınmış/silinmiş hesap engeli. Anlık hesap/oturum iptalinin mevcut access token'lara etkisi belirlenir ve test edilir.
5. **Profil ve mahalle:** `/users/me`, profil güncelleme, gizliliği koruyan public DTO; `Neighborhood`/PostGIS ve ilçe verisiyle konum atama. Bakiye/rozet entegrasyonu ayrı sözleşme olarak ele alınır; mevcutmuş gibi sabit değer üretilmez.
6. **Trust ve hesap yaşam döngüsü:** Saf/test edilebilir skor hesabı, event sözleşmeleri, idempotent consumer ve yeniden hesaplama; silme/askıya alma ve diğer servislerin yapacağı işler için olaylar. Diğer servis sahiplerinin teslimatları ayrı takip edilir.
7. **Kabul ve dokümantasyon:** HTTP güvenliği, gerçek Redis/PostgreSQL davranışı ve eşzamanlılık testleri; gateway üzerinden auth/profile smoke testi; çalıştırma yönergeleri ve bu memory güncellenir.

İlk teslim edilebilir dilim: **OTP iste → doğrula → güvenli token al → yalnızca kendi profilini gör → token yenile/çıkış yap.** Mahalle, trust event entegrasyonu ve silmenin servisler arası etkileri sonraki dilimlerdir; bunlar bitmeden tam identity kapsamı tamamlandı denmez.

Önerilen iç sorumluluklar: auth akışını yöneten servis, OTP servisi, SMS gönderici portu, token/oturum servisi, kullanıcı profil servisi, mahalle servisi, trust hesaplayıcı ve event consumer. Kesin sınıf adları veya gereksiz interface sayısı şimdiden dayatılmamıştır.

## 9. Uygulamadan önce ilgili aşamada çözülecek kararlar

- Dış API öneki ve mevcut demo uçlarının kaldırılması/geçiş süresi. OTP'siz kayıt bir auth bypass olarak korunamaz.
- SMS sağlayıcısı, gerçek gönderim için hesap/secret; geliştirme adapter'inin nasıl kullanılacağı.
- Telefon ülke kapsamı; ulusal numaralar kabul edilecekse hangi ülke varsayımıyla normalize edileceği.
- OTP doğrulama deneme limiti, tekrar gönderim bekleme süresi, IP güven sınırı, provider hata davranışı.
- JWT imza algoritması/anahtar dağıtımı, issuer/audience, rol yönetimi ve access token iptal politikası.
- Refresh süresinin her rotasyonda mı uzayacağı, toplam oturum ömrü ve eşzamanlı yenileme davranışı.
- İlk OTP doğrulamasında mahalle yokken onboarding ve izinler; mahalle veri kaynağı/ilk ilçe, sınırda/dışarıda kalan konum davranışı.
- Profil özetlerini birleştirme, notificationPrefs sahipliği, hesap silmenin ve telefonun yeniden kullanılmasının kuralları.
- Trust için teslim sayısının taraflara etkisi, no-show doğrulama yöntemi, hesap yaşı ay hesabı, event geçmişi/tekrar işleme sözleşmesi.

Bu kararlar mevcut özellik veya kullanıcı tarafından seçilmiş seçenek değildir. İlgili dilimde somut öneri sunulur; dış hesap/ücretli servis/başka takım kapsamı gerektiren işler ayrıca ele alınır.

## 10. Ortam, build ve test notları

- İnceleme ortamında `java -version`: OpenJDK `17.0.18`. Proje 21 hedefliyor; başka bir JDK 21 kurulu olup olmadığı araştırılmadı.
- `mvn` PATH üzerinde bulunamadı. Unix `mvnw` dosyası yok; yalnızca `mvnw.cmd` ve wrapper properties var. Yapılandırılmış wrapper dağıtımı Maven `3.9.8`.
- Docker CLI `29.6.2`, Compose `5.3.1` mevcut; daemon erişimi ve konteyner çalışması denenmedi.
- Dockerfile'lar hazır `target/*-1.0.0-SNAPSHOT.jar` kopyalıyor; kaynak derleyen multi-stage build yok. Temiz klonda JAR bulunmadığı için README'deki yalnızca `docker compose up --build -d` yönergesi yeterli değil.
- Local gateway host varsayılanları Docker servis adlarıdır; host üzerinde ayrı çalıştırırken `IDENTITY_HOST=localhost` gibi ayarlar gerekir.
- `yeniden-common` içindeki `spring-boot-starter-test` test scope'unda değil. Surefire sürümü açıkça ayarlanmamış; test keşfi raporlardan doğrulanmalı. JaCoCo/kapsama raporu yok.
- Kaynakta 6 test sınıfı, toplam 18 `@Test` var; identity 4 test içeriyor. Bu, testlerin çalıştığı veya kapsamanın %100 olduğu anlamına gelmez.

Java 21 ve Maven sağlandıktan sonra başlangıç kontrolü için önerilen, **bu incelemede çalıştırılmamış** komutlar (depo kökünden):

```sh
mvn -pl identity-service -am test
mvn test
mvn package
```

`-am` ortak modülü de reactor'a alır. JUnit test sayıları Surefire raporlarından kontrol edilmelidir. Test başarısı, Docker altyapısı veya güvenli HTTP erişiminin yerine geçmez.

## 11. Güncelleme kaydı

- 2026-08-30: İlk ayrıntılı analiz tamamlandı. `MEMORY.md`, `RULES.md`, `AGENTS.md` oluşturuldu. Uygulama geliştirmesi başlatılmadı; test/build çalıştırılmadı. Sonraki adım kullanıcı geliştirmeyi başlattığında ilk auth diliminin sözleşmesi ve ortam doğrulamasıdır.
- 2026-08-30: Kullanıcı ayrıntılı identity planı ve sunum için ücretsiz SMS/alternatif istedi. [Identity uygulama ve sunum planı](docs/IDENTITY_SERVICE_PLAN.md) oluşturuldu: P0–P8 iş paketleri, endpoint/veri önerileri, kabul testleri ve demo senaryosu. Plan öneridir; uygulama başlamadı.
- SMS araştırması: iletiMerkezi 100 SMS deneme kredisi aday; onaysız APITEST özel OTP metnini sabit mesajla değiştiriyor. Gerçek kullanım için onaylı başlık/hesap uygunluğu doğrulanmalı. Android+SMSGate/SIM paketi koşullu gerçek SMS alternatifi; her durumda izole ve açıkça etiketli yerel demo kutusu öneriliyor. Hiçbir hesap açılmadı ve SMS gönderilmedi. Güncel kaynaklar ve diğer adayların kısıtları plan dosyasında.
