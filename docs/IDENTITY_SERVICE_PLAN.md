# Identity-service — Uygulama ve sunum planı

Tarih: 2026-08-30. Durum: kullanıcı SMSGate'i seçti ve uygulamayı başlattı. Güncel ilerleme [IDENTITY_TASKS.md](IDENTITY_TASKS.md) içindedir. Aşağıdaki araştırma seçenekleri tarihsel bağlamdır; gerçek SMS adapter'i SMSGate olacaktır.
Dayanak: [MEMORY.md](../MEMORY.md), [RULES.md](../RULES.md), [API sözleşmesi](05-api.md), [domain modeli](02-domain-model.md), [güven skoru kuralları](04-ecocoin-rules.md).

Bu görev planlama ve SMS araştırmasıdır. Hesap açılmadı, SMS gönderilmedi, paket alınmadı, uygulama kodu veya runtime ayarı değiştirilmedi. Sağlayıcı koşulları resmi sayfalardan araştırıldı; gerçek hesap/numara üzerinde denenmedi. Aşağıdaki yeni teknik tercihler kullanıcı tarafından önceden seçilmiş kararlar değil, uygulama için önerilen varsayılanlardır.

## 1. Hedef ve teslim seviyeleri

Identity; telefonla giriş, kullanıcı/rol/oturum yönetimi, profil, yaklaşık konum/mahalle ve güven skorunu sağlar. Catalog, exchange, ecocoin ve gamification verilerinin sahibi değildir.

**Sunum için çekirdek teslim:** OTP isteği → kodun seçilen kanaldan iletilmesi → doğrulama → JWT/refresh → kendi profilini gör/güncelle → token yenile → çıkış. Hatalı/tekrar kullanılan OTP, yetkisiz profil erişimi ve refresh tekrar kullanımının engellenmesi de gösterilir.

**Tam identity teslimi:** Çekirdeğe mahalle ataması, trust hesabı/olay tüketimi, hesap silme/askıya alma ve diğer servislerle anlaşılmış sözleşmeler eklenir. Bir servis içindeki demo, platformun tamamının güvenli veya tamamlanmış olduğu anlamına gelmez.

Sunum tarihi, ekip kapasitesi, arayüz varlığı ve Android cihaz erişimi bilinmiyor. Bu nedenle tarihler yerine bağımlı iş paketleri tanımlandı. İlk çalışma dilimi sonunda ortam/test sonucu ve sunum tarihine göre takvim çıkarılır.

## 2. SMS araştırması: ücretsiz ne anlama geliyor?

Türkiye'ye sınırsız, sürekli ücretsiz ve kısıtsız özel OTP metni gönderen bir ticari API doğrulanamadı. Ücretsiz üyelik/API erişimi, ücretsiz mesaj teslimi değildir. Deneme kredisi de sürekli ücretsiz hizmet değildir.

| Seçenek | Resmi kaynakta görülen koşul | Bizim OTP/sunum için değerlendirme |
|---|---|---|
| iletiMerkezi | Yeni hesap için 100 SMS hoş geldin kredisi. Onaysız `APITEST` metni sabit test mesajıyla değiştirir. Marka başlığı için abonelik/başlık onayı gerekir. | Ücretsiz deneme kredisi adayı. Özel OTP içeriği için onaylı başlık ve hesapta kredinin kullanılabilirliği doğrulanmalı. `APITEST` ile giriş akışı çalışır denemez. |
| Verimor | Resmi makalede 25 SMS ücretsiz deneme paketi; ayrı resmi HTTP API dokümanı var. | Alternatif aday. Kampanya yazısı eski; güncel hesapta kredi, başlık, özel OTP metni ve alıcı koşulları kesinleşmeden sunum planı buna bağlanmaz. |
| Twilio | Güncel genel trial sayfası 30 gün/100 SMS, en fazla 5 doğrulanmış alıcı, kayıt ülkesine gönderim ve hazır mesaj içeriği sınırları belirtiyor. | Genel SMS trial'ı özel Redis OTP metnimiz için güvenilir başlangıç değil. Eski Console ve ürün sayfalarında farklı koşullar var; hesap/ürün doğrulaması gerekir. |
| Firebase Phone Auth | SMS doğrulama için Blaze planı gerekiyor; kurmaca test numaraları mevcut. | Faturalama hesabı gerektirmeyen ücretsiz gerçek SMS çözümü değil. Test numarası gerçek SMS göndermez. Auth'u Firebase'e taşımak ayrıca kapsam değişikliğidir. |
| Android + SMSGate | Ücretsiz/açık kaynak uygulama; Android telefonu API ile SMS gateway yapar. Yerel mod aynı ağda çalışır. | Android ve uygun SMS paketi varsa gerçek SMS için pratik demo adayı. Operatör/SIM maliyeti ve koşulları devam eder. |
| Yerel demo mesaj kutusu | Bizim oluşturacağımız, dış servis kullanmayan test adapter'i. | Ek servis ücreti yok; SMS teslimi simüle edilir. OTP/JWT/Redis/DB akışı yine gerçek kodla çalışır. |

Kaynaklar: [iletiMerkezi başlangıç/kredi](https://www.iletimerkezi.com/docs/api/overview), [APITEST davranışı](https://www.iletimerkezi.com/docs/api/test-mode), [Verimor deneme paketi](https://www.verimor.com.tr/makaleler/verimor-toplu-sms-ekran-goruntuleri/), [Verimor API](https://developer.verimor.com.tr/smsapi), [Twilio trial](https://www.twilio.com/docs/usage/trials), [Firebase limitleri](https://firebase.google.com/docs/auth/limits), [SMSGate](https://sms-gate.app/).

### Önerilen seçim sırası

1. **Onaylı başlık hazırsa:** iletiMerkezi deneme kredisinin bizim OTP metnimizle çalışmasını kontrollü bir gönderimle doğrula; başarılıysa bunu kullan. Kredi bitince otomatik ücretli pakete geçme.
2. **Onay bekleniyorsa ve Android varsa:** SMSGate yerel mod + ekip SIM'i. İkinci telefon alıcı olur; marka başlığı yerine SIM numarası görünür. SMS paketine dahilse ek mesaj maliyeti olmayabilir; paket/operatör şartı kontrol edilir.
3. **Her koşulda yedek:** açıkça etiketlenmiş yerel demo kutusu. Gerçek SMS arızasında üretim güvenliğini düşüren otomatik fallback olmaz; sunucu demo profiline bilinçli olarak geçirilir.

Kullanıcının seçimiyle gerçek SMS adapter'i **SMSGate Local Server** olarak kesinleşti. Diğer sağlayıcılar uygulanmayacak. Otomatik testlerde HTTP stub kullanılır; izole demo kutusu sunum yedeği taskı olarak kalır. Bu seçim giriş/oturum tasarımını değiştirmez.

### iletiMerkezi için kontrol listesi

- Kullanıcı hesabı kendisi açar; kimlik/e-Devlet/başlık işlemlerini kendisi yürütür. Secret'lar sohbet veya Git yerine yerel ortam değişkeninde saklanır.
- Panelde kredi gerçekten tanımlı mı, API erişimi açık mı, kullanılabilir onaylı başlık var mı kontrol edilir.
- Özel metin içindeki rastgele kodun aynı şekilde telefona ulaştığı doğrulanır. Sabit `APITEST` mesajı bunun kanıtı değildir.
- OTP kullanımı, alıcı kısıtları, kredi süresi ve varsa ek ücretler hesap özelinde kontrol edilir. Sağlayıcının belirttiği başlık süresi sunuma yetişme garantisi değildir.
- Birkaç ekip numarası dışında gerçek gönderim yapılmaz; mesajlar kısa tutulur ve SMS parça sayısı kontrol edilir.
- HTTP kabul yanıtı ile telefona teslim ayrılır. Kota bitmesi, hatalı yetki, reddedilen başlık ve zaman aşımı test edilir.

### Android/SMSGate için kontrol listesi

- Gönderici: erişim izni alınmış Android cihaz, SIM ve SMS paketi. Alıcı: ekibin izinli ikinci telefonu.
- Uygulama resmi kaynaktan kurulur; gerekli SMS izinleri gözden geçirilir. Gateway adresi/kimlik bilgileri yalnız backend'e verilir.
- Bilgisayar ve gateway aynı güvenilen özel ağa/hotspot'a bağlanır. Ortak kampüs Wi-Fi'sindeki cihaz izolasyonu önceden denenir; internet port yönlendirmesi açılmaz.
- Yerel mod HTTP + Basic Auth kullandığı için açık/paylaşımlı ağa bırakılmaz. Uzak erişim gerekirse güvenli taşıma ayrıca tasarlanır; OTP trafiği açık public cloud'a aktarılmaz.
- Telefon şarjda tutulur; arka plan kısıtı, ağ değişimi ve kapalı cihaz senaryosu denenir. Ağ internet olmadan çalışabilir, **gerçek SMS için hücresel şebeke yine gerekir**.
- Paketin otomatik/programatik gönderime uygunluğu kontrol edilir. Bu yol yüksek hacimli üretim altyapısı olarak sunulmaz.

Yerel modun koşulları: [SMSGate Local Server](https://docs.sms-gate.app/getting-started/local-server/).

### Gerçek telefona bildirim isteniyor ama SMS mümkün değilse

İkinci seçenek e-posta OTP olabilir. Resend ücretsiz planı günde 100, ayda 3.000 e-posta sunuyor; varsayılan `resend.dev` göndericisi yalnız hesabın kendi e-posta adresine gönderir. Başka alıcılar için sahip olunan domain doğrulanmalıdır. [Kota](https://resend.com/docs/knowledge-base/account-quotas-and-limits), [test domain kısıtı](https://resend.com/docs/knowledge-base/403-error-resend-dev-domain).

Ancak **e-posta doğrulaması telefon sahipliğini doğrulamaz**. Bu seçenek ayrı e-posta giriş akışı veya açıkça demo aktarımı olarak ele alınmalı; `phoneVerifiedAt` üretimde doldurulmamalı. İlk tercih olarak eklenmesi önerilmiyor: telefon temelli mevcut sözleşmeyi genişletir ve yine internet bağımlılığı yaratır.

## 3. Önerilen teknik kararlar

| Konu | Öneri | Kaynak / karar durumu |
|---|---|---|
| Dış API | `/api/v1/auth/**`, `/api/v1/users/**`; gateway identity'ye yollar | `05-api.md` ile uyumlu hedef. Eski yolların geçişi ayrıca belgelenir. |
| Eski auth | OTP'siz `/identity/auth/register` kapatılır; `/otp/send` güvenli yeni akışla değiştirilir | Bypass korunamaz. İstemci bağımlılığı önce araştırılır. |
| Başarı gövdesi | İlk geçişte mevcut `ApiResponse<T>`; token alanları `data` içinde | Önerilen uzlaştırma; `05-api.md` örnekleri geliştirmeyle birlikte güncellenir. |
| Hata gövdesi | Dokümandaki problem gövdesi; doğru HTTP status ve kararlı code | Controller + security filter hataları aynı sözleşmeye uyar. |
| Telefon kapsamı | İlk sunum `+90` mobil numaralar; ulusal biçimler TR bağlamıyla normalize edilir | Sunum varsayımı. Uluslararası kabul istenirse genişletilir. |
| OTP | 6 hane, 180 saniye, kriptografik rastgelelik; sunucuda HMAC tabanlı doğrulayıcı | TTL mevcut; saklama tasarımı güvenlik önerisi. |
| Deneme/resend | Challenge başına 5 yanlış deneme; yeniden gönderimde 60 saniye bekleme | **Yeni öneri**, exchange kuralından alınmış gereksinim değil. |
| İstek limitleri | Numara 3/saat, IP 10/saat; 429 + `Retry-After` | Mevcut API gereksinimi. |
| JWT | 15 dakika; asimetrik imza (RS256 önerisi), public key/JWKS paylaşımı | Ömür mevcut; algoritma/anahtar dağıtımı öneri. Tam OAuth/OIDC sunucusu kapsamda değil. |
| Refresh | Rastgele opaque token; hash DB'de; rotasyon + kullanılmış token geçmişi | Mevcut güvenlik hedefi. |
| Oturum ömrü | İlk girişten itibaren mutlak 30 gün; rotasyon bu sınırı uzatmaz | Belirsiz noktayı kapatan öneri; koddan önce sözleşmeye yazılır. |
| Access iptali | JWT'ye `sid`; identity her korumalı istekte oturum ve hesap durumunu DB'den kontrol eder | Sunum/ilk servis için basit ve anlık iptal. Diğer servisler aynı kontrolü yapmadıkça platform geneline garanti yok. |
| Onboarding | İlk OTP'de kullanıcı oluşur; mahalle yoksa `nbh` atlanır, `/me` onboarding durumunu belirtir | İlk giriş–mahalle çelişkisini çözen öneri. İlan/talep kısıtı ilgili servislere aittir. |
| Test/üretim | Ayrı DB/Redis ad alanı, issuer ve imza anahtarı; simülasyon üretimde başlatılamaz | Zorunlu güven sınırı. |

Dokümandaki genel “POST idempotency 24 saat aynı yanıt” yaklaşımı OTP/refresh/logout için aynen uygulanamaz: süresi geçmiş OTP ve iptal edilmiş token yanıtı cache'den verilmemeli. Auth uçlarının açık istisnası/tekrar deneme sözleşmesi yazılacak. Özellikle refresh tekrar kullanımı idempotency cache'iyle gizlenmeyecek.

Spring Security'nin JWT doğrulama desteği kullanılacak; güncel dokümandaki örnekler projeye doğrudan kopyalanmayacak, Boot 3.3.3'ün yönettiği sürümle uyumluluk doğrulanacak. [Resmi JWT dokümanı](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## 4. Planlanan API sözleşmesi

Aşağıdaki tüm yollar `/api/v1` öneki taşır. Alanlar ve HTTP kodları önerilen uygulama sözleşmesidir.

| Uç | İstek / erişim | Sonuç |
|---|---|---|
| `POST /auth/otp/request` | `{phone}`; anonim, rate limited | 202; `challengeId`, `expiresIn=180`, `retryAfterSeconds`; **kod yok**. Kabul, SMS teslim garantisi değildir. |
| `POST /auth/otp/verify` | `{phone, code, challengeId}` | 200; `accessToken`, `refreshToken`, `isNewUser`, `expiresIn`. `challengeId` eski kod/resend yarışlarını ayıran önerilen ek alan. |
| `POST /auth/refresh` | `{refreshToken}`; access süresi dolmuş olabilir | 200; yeni çift. Geçersiz/süresi dolmuş token 401; reuse tüm oturumları iptal eder. |
| `POST /auth/logout` | `{refreshToken}`; token sahipliğiyle mevcut oturum | 204; ilgili oturum iptal edilir; zaten iptal edilmiş token için idempotent davranış. |
| `GET /users/me` | Bearer token + aktif oturum/hesap | 200; private profil, mahalle, trust ve onboarding. Bakiye/seviye yoksa sahte varsayılan değer üretilmez. |
| `PATCH /users/me` | Bearer; `displayName`, `avatarKey` izin listesi | 200; güncel profil. Roller/status/trust yazılamaz. |
| `PUT /users/me/location` | Bearer; `{lat, lon}` | 200; yaklaşık konum/mahalle. Geçersiz koordinat 400; kapsam dışı nokta için kararlı 422. |
| `GET /users/{id}` | Public profil politikası | 200; görünen ad/avatar/tamamlanan teslim özeti. Telefon/e-posta/konum yok; silinmiş/görünmez hesap 404. |
| `DELETE /users/me` | Bearer + hassas işlem için yakın zamanda doğrulama | 202; yerel erişim kapanır, dış servis temizlik süreci başlatılır. 202 küresel silme tamamlandı demek değildir. |

`notificationPrefs`, bakiye, seviye ve rozetlerin bileşik profilde gösterilmesi için ayrı entegrasyon kararı gerekir. Sunum çekirdeği bu alanları kapsam dışı olarak belgeler. Tam doküman uyumu sonraki paketlerde kapatılır.

## 5. Veri modeli ve altyapı taslağı

| Kayıt | Alanlar / davranış | Paket |
|---|---|---|
| `identity.users` | Mevcut alanlar; E.164 unique, status, UTC zamanlar, hesap durumuna uygun okuma | P1 |
| `identity.user_roles` | `user_id`, `role`; aynı kullanıcı/rol çifti unique, varsayılan USER | P1 |
| `identity.auth_sessions` | `id`, `user_id`, oluşturma, mutlak bitiş, iptal zamanı/nedeni | P1/P3 |
| `identity.refresh_tokens` | Unique token hash, session FK, parent/replaced token ilişkisi, consumed/revoked/expiry | P1/P3 |
| `identity.trust_scores` | Mevcut sayaçlar + kullanıcı FK ve sınır kısıtları; başlangıç 50 | P1/P6 |
| `identity.neighborhoods` | İsim/ilçe/şehir, polygon/centroid ve mekânsal indeks | P5 |
| Trust olay günlüğü / processed events | Kaynak event ID + tür + user + güvenilir payload/etki; tekrar işleme ve rebuild | P6 |
| Identity outbox | Hesap durum/silme olayları, yayın denemesi ve yayınlandı bilgisi | P7 |
| Redis challenge | Ortama özel key, phone HMAC, challenge ID, kod HMAC, deadline, attempts, durum | P2 |
| Redis rate limit | Ortama özel telefon/IP pencere sayaçları; TTL ve atomik sınır kontrolü | P2 |

Refresh token en az 256 bit rastgelelik hedefiyle üretilir; hash token'ı geri getirmez. OTP düşük entropili olduğundan sunucu secret'lı HMAC ile saklanır; secret Redis'te tutulmaz. Düz OTP yalnız gönderim anında bellek/taşıma kanalında bulunur; demo sink bunun açıkça sınırlandırılmış test istisnasıdır.

Flyway geçişi temiz DB ve varsa mevcut şema için ayrı doğrulanır. Mevcut veritabanında önce envanter/yedek; kör `baselineOnMigrate`, şema drop veya tablo silme yapılmaz. Identity şemasına ait migration başka servisin tablolarına dokunmaz.

## 6. İş paketleri, bağımlılıklar ve kabul kriterleri

### P0 — Başlangıç ve sözleşmeyi sabitleme

Bağımlılık: yok. Çıktı: yeniden üretilebilir başlangıç ve net endpoint/kanal kararları.

- Java 21 seçimi, Maven/Unix wrapper, dependency çözümü ve test keşfini doğrula.
- Mevcut 18 testin hangilerinin gerçekten çalıştığını raporla; kırık olanları sınıflandır.
- Identity için `-pl identity-service -am` reactor çalışmasını doğrula; common/gateway yan etkilerini kaydet.
- Üretim olmayan demo ortamını belirle; hazır JAR bekleyen Dockerfile/Compose sırasını çalışır yönergeyle düzeltme kapsamını çıkar.
- API yolları, başarı zarfı, challengeId, auth idempotency istisnası ve sunum kanalını kısa karar kaydıyla sabitle.

Kabul: Java 21 build sonucu ve çalışan test sayısı biliniyor; secrets yok; sunum modu/gerçek SMS modu açıkça ayrılmış; kapsam dışı işler listelenmiş.

### P1 — Veri, hata ve validation temeli

Bağımlılık: P0. Çıktı: migration, kullanıcı/rol/oturum şeması, ortak HTTP davranışı.

- Identity'ye gereken Security, Validation, Flyway, test bağımlılıklarını açıkça tanımla; sürümler parent ile uyumlu olsun.
- User/TrustScore ve auth session/refresh migration'larını ekle; test DB ile schema validation doğrula.
- Telefon normalizasyonu, görünen ad uzunluk/boşluk, UUID/koordinat gibi girdi kurallarını tasarla.
- `BaseException` → problem response; validation/security hataları, trace ID, hassas veri maskeleme.
- Test edilebilir `Clock`/UTC kullanımı; mevcut tarih verisinin migration anlamını kontrol et.

Kabul: temiz test DB'de migration çalışır; tekrar çalıştırma şemayı bozmaz; aynı normalize telefon tek kullanıcıdır; hatalar 200 ile gizlenmez.

### P2 — OTP motoru ve gönderici adapter'i

Bağımlılık: P1. Sağlayıcı hesabı P0 ile paralel hazırlanabilir, kodu bloke etmez.

- `OtpService`, gönderici portu ve Redis işlemleri controller'dan ayrılır.
- Güvenli üretim, HMAC doğrulayıcı, 180 saniye TTL, resend/cooldown ve phone/IP rate limit.
- Önce yerel test göndericisi; sonra seçilen iletiMerkezi veya SMSGate adapter'i.
- Kabul/red/zaman aşımı ayrımı. Kabul edilmiş SMS henüz ulaşmamış olabilir; provider hatasında kullanıcıya başarı iddiası yapılmaz.
- Aynı telefonun eski challenge'ı yeniden gönderimde geçersiz olur; eski isteğin geç gelen sonucu yeni challenge'ı silemez.
- Provider çağrısını kör tekrar etme; idempotency desteği varsa kullan, belirsiz timeout'ta çift SMS riskini açık yönet.
- Atomik Lua/CAS ile doğrulama, yanlış deneme ve tüketim. Redis atomiklik için script desteği sağlar; scriptler kısa tutulur. [Redis scripting](https://redis.io/docs/latest/develop/programmability/eval-intro/).

Kabul: yanlış/expired/reused OTP reddedilir; iki paralel verify en fazla bir başarılı giriş işlemi oluşturur; limitler restart ve çoklu instance koşulunda Redis üzerinden korunur; auth response/log içinde kod yoktur.

### P3 — Giriş, JWT ve refresh güvenliği

Bağımlılık: P2. Çıktı: yeni/mevcut kullanıcı için çalışan oturum yaşam döngüsü.

- Başarılı OTP sonrası yeni kullanıcı + trust + USER rolü + oturum/refresh aynı DB transaction'ında oluşur; mevcut hesabın status'ü kontrol edilir.
- JWT üretimi/doğrulama: `sub`, `nbh` varsa, `roles`, `iat`, `exp`, `iss`, `aud`, `sid`. Private key servis dışına çıkmaz; log/token ekranında secret gösterilmez.
- Opaque refresh hash'leri; row lock/koşullu update ile tek tüketim. Kullanılmış token geçmişi korunur.
- Reuse halinde kullanıcı oturumlarını tek transaction'da iptal et; sonra 401 döndür. Exception iptal kaydını rollback etmemeli.
- Identity korumalı uçlarında session/status kontrolü; logout ardından aynı access token da identity'de reddedilir.
- Paralel refresh için istemci single-flight kullanır. Önerilen strict politika nedeniyle ağ yeniden denemesi de reuse sayılabilir; bu UX maliyeti dokümante edilir, sessiz grace window eklenmez.

Redis–DB sınırı: OTP atomik olarak tüketildikten sonra DB işlemi başarısız olursa token verilmez, tüketilen kod tekrar açılmaz. Kullanıcı yeni challenge ister; timeout/yeniden deneme sözleşmesi bunu açıklar. Daha karmaşık recovery gerekirse ayrı idempotent auth-attempt kaydı tasarlanır. Güvenliği gevşeterek “dağıtık transaction” taklidi yapılmaz.

Kabul: token yalnız doğrulanmış girişten çıkar; yanlış imza/issuer/audience/süre reddedilir; refresh replay diğer cihaz oturumlarını da kapatır; suspend/delete kontrolü işler; token/hash sızıntısı yoktur.

### P4 — Profil, gateway ve ilk sunum teslimi

Bağımlılık: P3. Çıktı: sunumun güvenilir minimum akışı.

- Private/public DTO ayrımı; `/me`, izinli profil güncellemesi ve public profil.
- Eski telefonla sorgu kapatılır veya gerçekten gerekli iç yetkili sözleşmeye taşınır; public kullanıcı dizini gibi kalmaz.
- Gateway yeni auth/users yollarını identity'ye taşır; eski route geçişi belgelenir. Identity doğrudan 8081'den de güvenlidir.
- OpenAPI ve tekrar üretilebilir HTTP/Postman istek koleksiyonu hazırlanır; demo arayüzü gerekiyorsa yalnız bu uçları çağıran küçük bir ekran seçilir.
- Browser kullanılırsa token saklama/CORS/CSRF kararı ayrıca verilir; refresh token gelişigüzel localStorage'a yazılmaz. Backend sunumu için koleksiyon yeterlidir.

Kabul: token olmadan `/me` 401; Alice'in token'ı yalnız Alice'i getirir; public profil telefon/e-posta/konum içermez; profil isteği rol/status/trust değiştiremez; gateway üzerinden login→me→refresh→logout tekrarlanabilir.

**Sunum yakınsa P0–P4 + P8 önceliklidir. P5–P7 tamamlanmamışsa sunumda açıkça geliştirme kapsamı olarak belirtilir.**

### P5 — Mahalle ve yaklaşık konum

Bağımlılık: P4 + veri kaynağı/ilk ilçe seçimi.

- Neighborhood modeli ve PostGIS mapping; sınır verisinin kaynağı/lisansı/kapsamı kaydedilir.
- Gerçek ilçe verisi yoksa yalnız demo DB'de açıkça sentetik olduğu belirtilen poligon kullanılır; gerçek coğrafi doğruluk iddia edilmez.
- Konum girdisi bounds kontrolü; polygon ataması; sınır çizgisi/çakışan polygon için deterministik seçim; kapsama dışı konum için hata.
- Tam ev adresi saklanmaz. Mahalle ataması için gereken ham nokta geçici işlenir; kalıcı konum kararlaştırılan yaklaşık hassasiyettedir. Profil noktası public DTO'ya girmez.
- Mahalle değişince mevcut JWT'deki `nbh` eskime politikası belirlenir; sonraki refresh güncel claim üretir. Yetki için tek başına eski claim kullanılmaz.

Kabul: ilçe içi/sınır/dış nokta testleri; geçersiz koordinatlar reddedilir; ham konum/log sızıntısı yok; onboarding tamamlanma durumu doğru.

### P6 — Trust hesabı ve event entegrasyonu

Bağımlılık: P4; canlı event entegrasyonu için exchange/moderation sözleşmesi.

- Formül ayrı saf hesaplayıcı; sayaçlar, tamamlanmış takvim ayı önerisi ve 0–100 clamp.
- `HandoverConfirmed` için identity'ye ayrı kuyruk; ecocoin kuyruğu paylaşılmaz. Event ID/schema version ve review durumu kesinleşir.
- No-show ve doğrulanmış şikayet event'leri henüz yok; isimleri ve alanları ilgili servis sahipleriyle kararlaştırılır.
- Aynı transaction'da event dedup + sayaç + score güncelleme; iki farklı event'in aynı kullanıcıya paralel gelişi lost update üretmemeli.
- Yeniden hesaplanabilir etki günlüğü; düzeltme/geri alınan şikayet senaryosu. Sadece event ID listesi rebuild için yeterli değildir.
- Hesap yaşı için günlük yenileme veya okumada hesaplama seçilir; saat sınırları test edilir.
- Demo event üreticisi yalnız test ortamında kullanılır; public “skor artır” endpoint'i açılmaz.

Kabul: başlangıç 50; yeni hesapta bir tamamlanmış teslim 52; bir no-show −5, doğrulanmış şikayet −15; eşikler 14/15 ve 29/30 doğru; duplicate event değişiklik yapmaz; rebuild aynı sonucu verir.

Tamamlanan teslimin hangi tarafların sayacını artıracağı netleştirilmeden canlı consumer tamamlandı sayılmaz. Trust'un `<15` sonucunu catalog/exchange'in uygulaması ilgili servislerin ayrı teslimatıdır.

### P7 — Hesap silme, askıya alma ve güvenilir olay yayını

Bağımlılık: P3/P4 + dış servis event sözleşmesi.

- Hesap durumu geçişleri ve hassas işlem için yakın zamanda OTP doğrulaması (öneri: son 5 dakika).
- Yerel session iptali, kimlik verisinin kararlaştırılmış anonimleştirme/saklama politikası ve outbox aynı transaction'da.
- Hesap silme olayı catalog/ecocoin vb. tüketicilere iletilir; identity onların DB'sine yazmaz.
- Outbox retry/backoff, duplicate olay, hata kuyruğu ve tamamlanmayan dış temizlik raporu.
- Eski telefonun tekrar kaydı ve silinmiş hesabın yeniden etkinleşmesi ayrı ürün politikası; otomatik eski hesaba login yapılmaz.

Kabul: hesabın identity erişimi derhal kapanır; publish hatası yerel silme kararını kaybettirmez; aynı event iki kez yayınlansa tüketici sözleşmesi idempotenttir. Catalog/ledger tarafı bitmeden “hesap tüm sistemden silindi” denmez.

### P8 — Kabul testleri, sunum provası ve devir

Bağımlılık: P4; P5–P7 eklenmişse onların testleri de dahil.

- Unit: normalizasyon, OTP/skor kuralları, durum/rol kontrolleri, mapper güvenliği.
- HTTP/security: 400/401/403/404/429/503, gateway ve doğrudan servis; yanıtta/logda hassas veri yok.
- Gerçek Redis/PostgreSQL ile integration: TTL, atomiklik, unique constraint, migration, refresh yarışları, rollback ve iptal kalıcılığı. Testcontainers veya izole Compose test altyapısı seçilir; Mockito tek başına yeterli değil.
- Adapter contract testleri: kabul, ret, kredi bitmesi, timeout; gerçek SMS yalnız kontrollü smoke provasında, otomatik testlerde gönderilmez.
- Ortam ayrımı: prod modunda demo adapter/mesaj kutusu açılamaz; demo JWT üretimde kabul edilmez; gateway demo kutusunu yönlendirmez.
- Test raporları ve komutlar README/identity dokümanına eklenir; memory güncellenir. Çalışmayan testler/entegrasyonlar açıkça listelenir.

Kabul: temiz demo ortamında iki ardışık prova; sunum kanalının yanı sıra yerel yedek çalışıyor; testler gerçekten keşfedilip koşuyor; hiçbir ücretli servis veya gerçek SMS otomatik tetiklenmiyor.

## 7. Planlanan değişiklik alanları

| Alan | Değişiklik | Sınır |
|---|---|---|
| `identity-service/src/main/java` | Auth/OTP/token/profile/trust/neighborhood, security ve adapter'ler | İşin ana kapsamı |
| `identity-service/src/main/resources` | Migration, ortam profilleri, secretsız ayar şablonu | Başka şemayı migrate etmez |
| `identity-service/src/test` | Unit, HTTP, Redis/DB ve concurrency testleri | Gerçek kişiye SMS göndermez |
| `identity-service/pom.xml` | Gerekli runtime/test bağımlılıkları | Versiyonlar parent uyumlu |
| `api-gateway` | Identity route ve gerekiyorsa identity için auth header/güvenlik entegrasyonu | Diğer servisleri toplu yeniden yazmaz |
| `yeniden-common` / parent POM | Gerekliyse ortak event sözleşmesi, test scope/Surefire düzeni | Tüm reactor testleriyle regresyon kontrolü |
| Docker/çalıştırma dosyaları | Identity demo ortamı ve JAR/build sırası | Var olan DB volume silinmez |
| `docs`, memory/rules | Kararlar, API, çalıştırma/test ve sunum kılavuzu | Öneri ve uygulanmış durum ayrılır |
| Diğer servisler | Trust/hesap olaylarının üretimi/tüketimi için sözleşme | Uygulamaları ayrıca kapsamlandırılır |

## 8. Sunumun uygulanabilir senaryosu

Hedef süre yaklaşık 6–8 dakika; sunum saatine ilişkin varsayım değildir.

| Sıra | Gösterim | Anlatılacak değer |
|---|---|---|
| 1 | Mimari: istemci → gateway → identity → Redis/PostgreSQL; gönderici adapter'i | Servis sınırları ve kimlik doğrulama sahipliği |
| 2 | Telefon gir, OTP iste; gerçek SMS veya etiketli demo kutusu aç | Kod API yanıtından gelmiyor; kanal değiştirilebilir |
| 3 | Bir yanlış kod gir, ardından doğru kodu gir | Doğrulama ve kötüye kullanım kontrolü |
| 4 | Token ile `/users/me`, ardından public profil | Kimlik ve kişisel veri ayrımı |
| 5 | Refresh yap, yeni token ile devam et | Kısa ömürlü access + dönen refresh |
| 6 | Ayrı hazırlanmış oturumda eski refresh'i tekrar kullan | Replay tespiti ve tüm oturumların iptali |
| 7 | Hazırsa mahalle/trust; değilse test raporu ve açık backlog | Çalışan kapsamın dürüst sınırı |

OTP tekrar kullanımını ayrıca gösterebiliriz. Rate limit testini ana giriş provasından sonra veya ayrı test fixture'ıyla yaparız; 3/saat limitini canlı sunumdan önce tüketmeyiz. Gerçek SMS ve simülasyon için ayrı test verisi/rate-limit ad alanı kullanılır; prod limitleri sunum kolaylığı için değiştirilmez.

### Yerel demo kutusunun güven sınırı

- Yalnız `demo` ortamında ve özel test DB/Redis ile çalışır; normal/production bootstrap bu adapter seçimini reddeder.
- Kutunun erişimi loopback üzerinde ayrı port + sunucu tarafı demo parolası ile sınırlanır. Docker varsa port yalnız `127.0.0.1`'e publish edilir; ana API gateway'den geçmez.
- Sadece önceden tanımlı test kimliklerine mesaj kabul eder; gerçek kullanıcılara ait OTP/telefon listesi toplanmaz.
- Kodlar sınırlı bellek/TTL ile tutulur; uygulama loguna veya auth response'a yazılmaz. Üretim Redis'inden kod okuyan bir arka kapı yoktur.
- Kutuda “SMS simülasyonu — telefon sahipliği doğrulanmaz” etiketi görünür. Demo kimlikleri ve token'ları production ortamında geçerli değildir.
- Sabit `123456` veya “her kodu kabul et” kullanılmaz; yanlış/süresi dolmuş/tekrar kullanılan kod yine reddedilir.
- Gerçek provider hatası bu kutuyu sessizce açmaz. Sunum sırasında bilinçli mod geçişi yapılır ve izleyiciye söylenir.

Önerilen sunum cümlesi: “OTP üretimi, süre ve deneme kontrolleri, kullanıcı kaydı ve token yönetimi backend'de çalışıyor. Bu gösterimde mesaj teslimini yerel bir test kutusuyla simüle ediyoruz; telefon sahipliğini gerçek SMS ile doğrulama, aynı arayüze bağlı sağlayıcı üzerinden yapılacak.”

### Prova ve hata durumunda devam

- Önceden dependency/image indirme ve local build tamamlanır; canlı sunum sırasında kurulum yapılmaz.
- Başarılı giriş için izinli demo alıcıları ve sunum kotası ayrılır; telefonda bildirim gizliliği kontrol edilir.
- Gerçek SMS sağlayıcısı varsa sunumdan önce aynı ağ ve alıcıyla prova yapılır; kredi/başlık durumu son kez kontrol edilir.
- SMS gecikirse sonsuz tekrar gönderilmez. Makul gösterim süresi içinde yerel simülasyona açıkça geçilir; gerçek SMS kaydını taklit eden görüntü gösterilmez.
- İsteğe bağlı kısa önceden kaydedilmiş prova videosu yedek olur; canlı sonuçmuş gibi sunulmaz.

## 9. Tamamlandı kontrol listesi

- [ ] P0: Ortam/test başlangıcı ve sözleşme kararları kaydedildi.
- [ ] P1: Migration/validation/hata modeli doğrulandı.
- [ ] P2: OTP motoru, limitler ve bir mesaj adapter'i çalışıyor.
- [ ] P3: JWT, rotating refresh, reuse/logout/hesap durumu güvenliği doğrulandı.
- [ ] P4: Private/public profil ve gateway sunum akışı çalışıyor.
- [ ] P5: Mahalle/konum ve veri kaynağı doğrulandı.
- [ ] P6: Trust hesabı + idempotent, yeniden hesaplanabilir olay işleme doğrulandı.
- [ ] P7: Hesap yaşam döngüsü + dış servis sözleşmeleri ve outbox doğrulandı.
- [ ] P8: Test kanıtı, iki prova, kanal yedeği ve güncel dokümanlar hazır.

Bu dosyanın oluşturulması yukarıdaki geliştirme maddelerinin tamamlandığı anlamına gelmez. Sonraki başlangıç noktası P0'dır; gerçek SMS seçimi hesabın/cihazın uygunluğu doğrulandığında kesinleştirilir.
