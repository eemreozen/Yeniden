# Identity-service task listesi

Başlangıç: 2026-08-30. Kullanıcı SMSGate Local Server seçti ve uygulamaya izin verdi.
Kaynak: [ayrıntılı plan](IDENTITY_SERVICE_PLAN.md). Durum: `[ ]` bekliyor, `[~]` sürüyor, `[x]` kanıtla tamamlandı. “Dış bağımlılık” ilgili adımı sınırlar; diğer tasklar devam eder.

## P0 — Başlangıç

- [x] ID-001: SMSGate kararını ve tüm P0–P8 kapsamını task listesine aktar. Kabul: sağlayıcı seçimi açık, alternatifler tarihsel.
- [x] ID-002: Java 21 ve Maven 3.9.8/Unix wrapper. Kabul: repo kökünden tekrar üretilebilir build.
- [x] ID-003: Mevcut reactor test başlangıcı ve Surefire keşfi. Kabul: gerçek test sayısı/başarısızlık kaydı.
- [x] ID-004: İzole PostgreSQL/Redis test ortamı. Kabul: testler mevcut uygulama volume'larına dokunmaz.
- [x] ID-005: API/OTP/oturum kararlarını uygulanan sözleşme olarak kaydet. Kabul: eski auth bypass'ı korunmaz, uyumsuzluklar belgeli.

## P1 — Temel

- [x] ID-010: Identity bağımlılıkları; common test scope ve reactor regresyonu. Bağımlılık: 002–003. Kabul: Java21 derleme, tüm testler keşfedilir.
- [x] ID-011: Identity başlangıç migration ve mevcut DB geçiş yönergesi. Kabul: temiz DB + validate, veri silme/baseline otomasyonu yok.
- [x] ID-012: Kullanıcı/rol/trust kısıtları ve UTC/Clock. Kabul: unique telefon, geçerli durum/rol, test edilebilir zaman.
- [x] ID-013: TR mobil telefon normalizasyonu. Kabul: 05/+905/905 biçimleri tek E.164; hatalı/uluslararası kapsam dışı numara reddedilir.
- [x] ID-014: DTO validation, problem response, security hata yanıtları. Kabul: doğru 400/401/403/404/429/503, kararlı code, hassas içerik yok.
- [x] ID-015: Session/refresh tabloları ve indeksler. Kabul: hash unique, FK, geçmiş ve mutlak bitiş.

## P2 — SMSGate ve OTP

- [x] ID-020: `SmsSender` portu ve SMSGate yerel HTTP adapter'i. Kabul: Basic Auth + /message + süre sınırı; yanıltıcı başarı yok.
- [x] ID-021: SMSGate config güvenliği ve alıcı allowlist. Kabul: varsayılan gönderim kapalı, secret loglanmaz, yerel HTTP açık tercih gerektirir.
- [x] ID-022: SMSGate HTTP sözleşme testleri. Kabul: 2xx kabul/ret/timeout, gövdeyi belleğe almadan atma; hiçbir gerçek SMS yok. 2xx gövdesi ayrıştırılmaz, cihaz teslimi ayrıca doğrulanır.
- [x] ID-023: Kriptografik OTP + HMAC; challenge kimliği ve 180 saniye TTL. Bağımlılık: 013. Kabul: düz OTP Redis/response/log içinde yok.
- [x] ID-024: Redis atomik istek limitleri ve 60 saniye cooldown. Kabul: 3/numara/saat, 10/IP/saat, 429+Retry-After.
- [x] ID-025: Atomik verify/5 deneme/tek kullanımlık tüketim. Kabul: paralel verify yalnız bir kez başarılı, TTL yanlış girişte uzamaz.
- [x] ID-026: SMS gönderim hatası/eski challenge/resend yarışı. Kabul: eski sonuç yeniyi silemez; belirsiz timeout kör tekrar edilmez.
- [ ] ID-027: Android kurulumu ve ağ/izin/SIM kontrolü. Dış bağımlılık: kullanıcının cihazı/özel gateway adresi ve yerel secret ayarı.
- [ ] ID-028: İzinli test alıcısına gerçek SMS provası. Bağımlılık: 027; kodun cihazda aynen teslim edildiği kanıtlanır. HTTP stub başarısı gerçek SMS kanıtı değildir.

## P3 — Kimlik ve oturum

- [x] ID-030: OTP sonrası kullanıcı/trust/rol oluşturma ve phoneVerifiedAt. Kabul: OTP olmadan oturum yok; suspend/delete reddi.
- [x] ID-031: RS256 JWT encoder/decoder, issuer/audience/exp/sid. Kabul: 15 dakika, gizli alan yok, prod dış anahtar zorunlu.
- [x] ID-032: 256-bit refresh hash/rotasyon/30 günlük mutlak oturum. Kabul: tek tüketim ve consumed geçmişi.
- [x] ID-033: Refresh tekrarında tüm oturumları iptal et. Kabul: eşzamanlı işlemlerde iptal kalıcı, rollback iptali geri almıyor.
- [x] ID-034: Logout ve aktif oturum/hesap kontrolü. Kabul: iptal edilmiş session'ın access token'ı identity'de de reddedilir.
- [x] ID-035: Redis–DB hata sınırı ve yeniden deneme sözleşmesi. Kabul: başarısız DB işleminde token yok, tüketilen kod yeniden açılmaz.
- [x] ID-036: Anahtar/issuer ortam izolasyonu. Kabul: lokal token üretimde reddedilir, yerel anahtar yalnız açık local profilde üretilir.

## P4 — Profil ve sunum çekirdeği

- [x] ID-040: `/users/me` ve public profil DTO ayrımı. Kabul: public yanıtta telefon/e-posta/konum yok.
- [x] ID-041: Profil izinli alan güncelleme. Kabul: roller/status/trust/telefon doğrulama istemciyle değiştirilemez.
- [x] ID-042: Gateway auth/users yolları + eski register/OTP/telefon arama uçlarını kaldır. Kabul: bypass yok, diğer servis route'ları korunur.
- [x] ID-043: HTTP istek koleksiyonu/OpenAPI ve hata örnekleri. Kabul: secretsız, auth idempotency istisnası belgeli.
- [x] ID-044: Gateway üzerinden login/me/refresh/logout smoke testi. Kabul: doğrudan identity ve gateway'de aynı koruma. Gerçek HTTP + test SMS sender; cihaz provası değil.
- [ ] ID-045: Bileşik profil entegrasyon sözleşmesi. Dış bağımlılık: bakiye/seviye/rozet/notificationPrefs sahipleri. Eksik veri sabit değerle taklit edilmez.

## P5 — Mahalle

- [ ] ID-050: Neighborhood/PostGIS migration ve indeksler. Bağımlılık: 011.
- [ ] ID-051: İlk ilçe veri kaynağı/lisans veya açık sentetik demo fixture seçimi. Gerçek veri dış bağımlılıktır.
- [ ] ID-052: Konum validation, polygon ataması ve boundary/dış alan politikası. Kabul: deterministik seçim, kararlı hata.
- [ ] ID-053: Yaklaşık konum saklama, onboarding ve JWT nbh yenilenmesi. Kabul: ham konum sızıntısı yok; eski claim yetki kaynağı değil.
- [ ] ID-054: Konum entegrasyon testleri. Kabul: içeride/sınırda/dışarıda/bozuk koordinat.

## P6 — Trust

- [ ] ID-060: Saf skor hesaplayıcı ve yaş hesabı. Kabul: formül/clamp, 14/15 ve 29/30 sınır testleri.
- [ ] ID-061: Handover/no-show/doğrulanmış şikayet event sözleşmesi ve taraf sayımı. Dış bağımlılık: exchange/moderation; kullanıcıya açık skor yazma yok.
- [ ] ID-062: Ayrı identity kuyruğu, event dedup + sayaç transaction'ı. Kabul: duplicate ve paralel farklı event doğru.
- [ ] ID-063: Rebuild için olay etkisi günlüğü ve düzeltme olayları. Kabul: rebuild aynı skor/sayaçları verir.
- [ ] ID-064: Hesap yaşı yenileme ve düşük trust tüketim sözleşmesi. Catalog/exchange engel uygulaması ayrı servis teslimatıdır.
- [ ] ID-065: Broker/DB entegrasyon testleri ve izole demo event fixture. Kabul: queue paylaşılmaz, retry veri çoğaltmaz.

## P7 — Hesap yaşam döngüsü

- [ ] ID-070: Silme/askıya alma geçişleri + yakın zamanda doğrulama. Kabul: yerel oturumlar iptal, yetkisiz değişim yok.
- [ ] ID-071: Saklama/anonimleştirme/telefon yeniden kullanım politikası. Ürün kararı; kanıt olmadan mevzuat garantisi verilmez.
- [ ] ID-072: Identity outbox ve publish retry/backoff. Kabul: yerel transaction + olay kalıcı, publish hatası veri kaybetmez.
- [ ] ID-073: Catalog/ecocoin/notification temizlik sözleşmesi. Dış servislerin uygulaması ayrı; küresel silme tamamlandı iddiası yok.
- [ ] ID-074: Hesap yaşam döngüsü ve outbox hata testleri. Kabul: duplicate/yeniden başlatma/publish hatası.

## P8 — Doğrulama ve sunum

- [x] ID-080: Redis/DB ile concurrency/TTL/rollback/migration testleri. Kabul: mock dışı kanıt.
- [x] ID-081: HTTP güvenlik/gizlilik test matrisi. Kabul: eksik/bozuk token, rol, hesap durumu, DTO sızıntısı.
- [ ] ID-082: Opsiyonel izole demo mesaj kutusu. Kabul: loopback+auth, test kimlikleri, TTL, görünür simülasyon etiketi; prod'da açılmaz.
- [x] ID-083: SMSGate kurulumu, secret ayarları, gateway/ağ ve hata yönergeleri. Kabul: gerçek telefon olmadan da test komutları çalışır.
- [x] ID-084: Sunum 6–8 dakikalık akış ve kota planı. Kabul: hatalı OTP, giriş, public/private, refresh replay gösterimi.
- [ ] ID-085: İki tam prova ve yedek; opsiyonel kayıt canlı diye sunulmaz. Bağımlılık: gerçek cihaz veya etiketli simülasyon.
- [x] ID-086: İlk dilimin reactor build/test raporu, mevcut sınırlamalar ve memory güncellemesi. Kabul: yapılan/koşmayan net; sonraki dilimlerde güncellenir.

## Kanıt günlüğü

- 2026-08-30 başlangıç: Java21 `/opt/homebrew/opt/openjdk@21` altında mevcut; varsayılan Java17 değiştirilmiyor. Maven indirilecek. Colima Docker motoru kapalı; başlatma ve izole test altyapısı hazırlanıyor.
- 2026-08-30 uygulama: Java21/Maven3.9.8 ile temiz başlangıç snapshot'ında 18 test geçti. Colima başlatıldı; mevcut uygulama verisine dokunulmadı.
- İlk dilim: SMSGate/OTP/JWT/refresh/profil kodu, Flyway V1, DTO validation, security, gateway route'ları ve Unix Maven başlatıcısı eklendi.
- Reactor `package`: 175 identity testi (29 gerçek Redis/PostgreSQL entegrasyonu dahil) + diğer servislerde 14 test; toplam 189 test, 0 hata/başarısızlık/atlama. Tüm modüller paketlendi. Kapsama yüzdesi ölçülmedi.
- [Runbook ve sunum akışı](IDENTITY_RUNBOOK.md), [HTTP koleksiyonu](identity.http). Gerçek SMS, Android kurulumu ve mevcut veri yükseltme migration'ı yapılmadı.
- Ayrı GatewayIdentitySmokeIT: 14/14 (13 tekrar direct test + 1 gerçek gateway HTTP akışı) geçti; gerçek SMS yok. Unix wrapper temiz cache indirme/SHA-512 kontrolü geçti.

## İlk dilim sonrası ek operasyon görevleri

- [ ] ID-090: Mevcut dolu identity şemasını envanterle; normalize telefon çakışmaları ve timestamp saat dilimi için veri koruyan upgrade migration'ı tasarla. V1 yeni DB içindir.
- [ ] ID-091: Gerçek çok kullanıcılı dağıtım için güvenilen proxy/IP çözümleme ve rate limit kapasitesi. Şimdilik gateway IP'si ortak limitlenir; X-Forwarded-For'a güvenilmez.
- [ ] ID-092: Üretim anahtar rotasyonu/JWKS, refresh geçmişi saklama/temizlik, DB rol izolasyonu ve operasyon izleme. Local ephemeral anahtar üretim çözümü değildir.
