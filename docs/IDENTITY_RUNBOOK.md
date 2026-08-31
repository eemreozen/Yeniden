# Identity + SMSGate çalıştırma ve sunum

Bu dilim: OTP → kullanıcı → JWT/refresh → private/public profil. Mahalle, trust olayları, hesap silme/outbox ve diğer servislerin güvenliği henüz bitmedi. Task durumları için [IDENTITY_TASKS.md](IDENTITY_TASKS.md).

## 1. Gereksinimler ve test

Java 21, Docker, curl ve SHA-512 aracı gerekir. Depo kökünde:

```sh
# Bu Mac'te kurulu JDK; başka ortamda kendi JDK21 yolunu kullan.
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./mvnw --version
./mvnw test
./mvnw package
```

Unix başlatıcı Maven 3.9.8'i ilk çalıştırmada indirir, sabit resmi SHA-512 ile doğrular, Git dışında `.mvn/.cache` altında tutar. Windows için mevcut `mvnw.cmd` vardır. İlk indirmeler ağ erişimi ister.

Colima kullanılıyorsa:

```sh
colima start
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./mvnw -pl identity-service -am test
```

Gateway için ayrıca (önce JAR paketlenir, ardından gerçek HTTP smoke):

```sh
./mvnw -pl api-gateway -am package -DskipTests
./mvnw -pl identity-service -am -Dtest=GatewayIdentitySmokeIT -Dsurefire.failIfNoSpecifiedTests=false test
```

Bu açık smoke komutu 13 doğrudan regresyonu tekrarlar ve gerçek gateway süreciyle bir tam akış ekler; toplam 14 test. GatewayIdentitySmokeIT varsayılan `test` deseninin dışındadır. SMS hâlâ test double'dır; Android'e göndermez. Alt gateway süreci sonunda kapatılır, logu `identity-service/target/gateway-smoke.log` altındadır.

`./mvnw -pl identity-service -am test -DexcludedGroups=integration` yalnız birim/HTTP-adapter testlerini çalıştırır. Entegrasyon testleri varsayılan test komutunda keşfedilir; Docker yoksa sessizce atlanmaz. PostgreSQL/Redis Testcontainers verisi tek kullanımlıktır, ana Compose volume'ları kullanılmaz. Adapter testleri yerel HTTP stub'ıdır; hiçbir test gerçek SMS göndermez.

## 2. Ayrı yerel veritabanı

`IDENTITY_LOCAL_DB_PASSWORD` değerini yerel secret yöneticisinden veya terminalde görünmeyen girişle ayarla; gerçek değerleri Git'e/sohbete koyma. Ardından:

```sh
docker compose -f docker-compose.identity.yml up -d --wait
export DB_HOST=127.0.0.1
export DB_PORT=55432
export DB_NAME=identity_local
export DB_USER=identity_local
export DB_PASS="$IDENTITY_LOCAL_DB_PASSWORD"
export REDIS_HOST=127.0.0.1
export REDIS_PORT=56379
export SPRING_PROFILES_ACTIVE=local
export OTP_HMAC_SECRET="$(openssl rand -hex 32)"
```

OTP anahtarını sunum boyunca aynı tut; restart/secret değişimi eski challenge'ları doğrulanamaz yapar. Local profil geçici RSA anahtarı üretir; servis yeniden başlayınca eski access token geçersizdir. Uzun ömürlü yerel test için dış PEM çifti de verilebilir.

```sh
./mvnw -pl identity-service -am package -DskipTests
java -jar identity-service/target/identity-service-1.0.0-SNAPSHOT.jar
```

Bu aşamada `SMS_GATE_ENABLED=false`: uygulama başlar, SMS isteği 503 olur. Sahte başarı veya kod dönen demo endpoint'i yoktur.

Gateway'i ayrı terminalde:

```sh
./mvnw -pl api-gateway -am package -DskipTests
IDENTITY_HOST=127.0.0.1 java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

Identity 8081, gateway 8080. Yalnız yerel gösterim için her iki sürece `SERVER_ADDRESS=127.0.0.1` ver. Yukarıdaki `-DskipTests` paketleme kolaylığıdır, test kanıtı değildir.

## 3. Android SMSGate

Telefon ve bilgisayarı güvenilen aynı yerel ağa bağla. Android'de SMSGate'i kur, gerekli SMS izinlerini ver, Local Server anahtarını aç ve durumunu Online yap. Uygulamadaki **yerel** IP, port ve Basic Auth bilgilerini kullan. Bu mod JWT değil Basic Auth kullanır; cihazın `/docs` adresi API arayüzüdür. [Resmî yerel sunucu yönergesi](https://docs.sms-gate.app/getting-started/local-server/).

Aşağıdaki ayarların gerçek değerlerini yalnız yerel ortamına yaz:

| Değişken | Değer |
|---|---|
| `SMS_GATE_ENABLED` | `true` |
| `SMS_GATE_BASE_URL` | Örn. `http://192.168.1.42:8080`; sadece origin, `/message` ekleme |
| `SMS_GATE_USERNAME`, `SMS_GATE_PASSWORD` | Cihazın gösterdiği Basic Auth bilgileri |
| `SMS_GATE_ALLOW_PRIVATE_HTTP` | Yerel düz HTTP için açıkça `true` |
| `SMS_GATE_ALLOWED_RECIPIENTS` | İzinli gerçek test alıcısı; E.164, birden çoksa virgülle ayır |

Ayarları değiştirdikten sonra identity'yi yeniden başlat. Alıcı listesi boşsa hiçbir numaraya göndermez. Bu sunum odaklı allowlist sınırlamasıdır; genel kullanıcı açılımı ayrı karardır.

HTTP üzerinden Basic Auth şifrelenmez: yalnız güvenilen yerel ağ/VPN kullan; telefonu internete port yönlendirmeyle açma. HTTPS kullanıyorsan güvenilir sertifika gerekir; TLS kontrolü kapatılmaz. Adapter redirect/proxy/kör retry yapmaz. Timeout'ta SMS ulaşmış olabilir; yeni kod istemeden bekleme süresini kontrol et.

API `POST /message` ile `textMessage.text`, `phoneNumbers`, challenge UUID'sini `id` olarak gönderir. 2xx yalnız taşıma katmanında kabul sayılır; durum gövdesi okunmaz ve telefona teslim garantisi verilmez. [Mesaj gönderim sözleşmesi](https://docs.sms-gate.app/features/sending-messages/).

SMSGate seçimi operatör SMS ücretini kaldırmaz. Sunumdan önce SIM paketini/kotasını kendi operatöründen doğrula; burada ücretsiz teslim garantisi yok. Cihaz açık, şarjda ve uygulama çalışır durumda olsun. Gerçek alıcıya gönderim ve teslim provası henüz yapılmadı.

## 4. Uygulanan API ve davranış

Başarılar `ApiResponse.data`, hatalar `application/problem+json` içinde `code` ve gerçek HTTP statüsüdür. [Çalıştırılabilir istek taslağı](identity.http).

| İstek | Gövde / sonuç |
|---|---|
| POST /api/v1/auth/otp/request | `phone` → 202: challengeId, expiresIn, retryAfterSeconds |
| POST /api/v1/auth/otp/verify | phone, code, challengeId → 200: accessToken, refreshToken, isNewUser, expiresIn |
| POST /api/v1/auth/refresh | refreshToken → 200 yeni token çifti |
| POST /api/v1/auth/logout | refreshToken → 204; bilinmeyen/eski token için de idempotent |
| GET /api/v1/users/me | Bearer JWT → kendi telefon/e-posta/mahalle/durum bilgileri |
| PATCH /api/v1/users/me | Bearer JWT; yalnız displayName ve avatarKey |
| GET /api/v1/users/{UUID} | Public: id, displayName, avatarKey, trustScore |
| GET /api/v1/identity/health | Süreç liveness; DB/Redis hazırlık garantisi değil |

Eski `/api/v1/identity/auth/register`, `/otp/send`, `/otp/verify` ve telefonla kullanıcı sorgusu kaldırıldı. Alias yok; istemci yeni sözleşmeye geçmeli. Diğer servis yolları değişmedi. Bilinmeyen alanlar 400; istemci rol/status/telefon/trust yazamaz. avatarKey, dosya anahtarıdır; dosya yükleme/doğrulanmış sahiplik servisi bu dilimde yok.

Telefon kapsamı yalnız Türkiye mobil: `05...`, `5...`, `905...`, `+905...` aynı hesaba normalize edilir. OTP 6 hane, en çok 180 saniye, 5 deneme; tekrar istek 60 saniye, kayan saat içinde 3/telefon ve 10/IP. Başarısız gönderim de kotayı tüketir; saldırganlar hata üreterek kotayı sıfırlayamaz.

İstemcinin X-Forwarded-For başlığına güvenilmez; `remoteAddr` kullanılır. Gateway arkasında bu, gateway IP'sinin ortak limitlenmesi demektir. Sunumda tek kullanıcıya yeterlidir; gerçek çok kullanıcılı yayın öncesi güvenilen proxy topolojisi/IP çözümleme ayrıca uygulanmalıdır. Forwarded başlık güvenini kontrolsüz açma.

JWT RS256, en çok 15 dakika; refresh 256-bit rastgele, DB'de SHA-256, başlangıçtan itibaren mutlak 30 gün. Aynı refresh'i eşzamanlı veya ikinci kez kullanmak **tüm kullanıcı oturumlarını iptal eder**. Client refresh'i single-flight yapmalı, belirsiz ağ hatasında aynı token'ı kör tekrar etmemeli. Logout yalnız ilgili oturumu iptal eder. Identity her Bearer isteğinde DB oturum/durum kontrolü yapar; diğer servisler otomatik korunmuş değildir.

OTP Redis'te tek kullanımlık tüketildikten sonra DB transaction'ı başlar. DB/token üretimi başarısızsa kullanıcı/trust/session transaction'ı geri alınır, tüketilen OTP geri gelmez; cooldown sonrası yeni OTP gerekir. Redis kullanılamıyorsa doğrulama/gönderim başarısız olur. DB kullanılamıyorsa token ile erişim kapalı kalır.

Refresh geçmişi replay tespiti için tutulur; bu dilimde otomatik temizlik/saklama işi yok. Logout için auth header zorunlu değil; süresi dolmuş Bearer başlığı göndermeyin.

## 5. Üretim ve mevcut DB sınırı

`local` dışında açık HTTPS issuer, ayrı Redis namespace ve PKCS#8 private/X.509 public RSA PEM çifti zorunlu:

- `JWT_ISSUER=https://identity.example.org`, `JWT_AUDIENCE=yeniden-api`
- `IDENTITY_TOKENS_PRIVATE_KEY_PATH`, `IDENTITY_TOKENS_PUBLIC_KEY_PATH`: güvenli dosya yolları.
- `IDENTITY_OTP_NAMESPACE`: bu ortam için benzersiz namespace.
- `OTP_HMAC_SECRET`, DB erişimi ve SMS secret'ları secret yöneticisinden.
- En az 2048-bit eşleşen RSA anahtarları; `local` ile `prod/production` beraber açılmaz.

Local issuer `urn:yeniden:identity:local` ile üretim issuer'ı ayrıdır. Anahtar rotasyonu için çoklu aktif doğrulama anahtarı/JWKS bu dilimde yok; üretim operasyonu ayrıca tasarlanmalı. Auth rehberinin least-privilege/secret/lifecycle yaklaşımı bu sınırları belirledi.

Flyway V1 **yeni/boş identity şemasına** yöneliktir. Eski `ddl-auto:update` tabloları olan DB'ye otomatik uyarlama, baseline veya silme yapılmaz. Böyle bir şemada başlangıcın durması korumadır. Önce yedek + şema/veri envanteri + telefon çakışma ve timestamp saat dilimi analizi + ayrı veri koruyan geçiş migration'ı gerekir. Eski kayıtlar OTP doğrulanmış varsayılmaz. `baseline-on-migrate=true` açarak bu kontrolleri atlama.

Test ortamı tablo kısıtları/migration/validate'i kanıtlar; gerçek mevcut verinin yükseltilmesini kanıtlamaz. DB rol izolasyonu, TLS, anahtar rotasyonu, üretim kapasitesi ve bağımsız güvenlik incelemesi tamamlanmadan üretime hazır denmez.

## 6. Sunum akışı (6–8 dakika)

1. **0:00–1:00:** “Telefon doğrulanmadan hesap adına oturum açılmıyor.” Android gateway ile akışı göster; şifreleri/telefonun tamamını yansıtma.
2. **1:00–2:00:** İzinli numaraya OTP iste, telefonda gerçek mesajı göster. HTTP yanıtta kod bulunmadığını göster.
3. **2:00–3:00:** Bir kez yanlış kodla 400, sonra doğru kodla giriş. Kodun tekrar kullanımının reddini göster.
4. **3:00–4:00:** Kendi profilinde özel alanlar, public profilde bu alanların yokluğu; ad güncelleme.
5. **4:00–5:00:** Refresh ile yeni token çifti; eskisini tekrar kullanınca bütün oturumların reddi.
6. **5:00–6:00:** Yeniden OTP için cooldown'u gözet; alternatif olarak logout akışını ayrı hazırlanmış ikinci oturumda göster.
7. **6:00–8:00:** Test raporları, güvenlik sınırları ve mahalle/trust/outbox sıradaki taskları.

Bir numara için saatte en fazla 3 SMS isteği olduğundan tekrar tekrar resetleme yapma. Aynı saatte iki tam prova + canlı sunum kota tüketebilir; provayı önceden yap veya yalnız izinli ikinci test numarasını planla. Otomatik testler kotayı tüketmez.

Telefon bağlantısı yoksa gerçek SMS gösterimi yapılamaz. Yedek olarak test raporu ve önceden alınmış, açıkça “kayıt” etiketli video kullanılabilir. Kod gösteren geliştirme inbox'ı henüz uygulanmadı; varmış gibi sunma.

## 7. Sık hatalar

- 503 OTP: gönderim kapalı, alıcı allowlist dışı, cihaz erişilemiyor, Basic bilgileri yanlış, Redis kapalı veya provider timeout.
- 429: `Retry-After` kadar bekle; başarılı verify cooldown'u sıfırlamaz.
- 400 invalid_otp: yanlış/eski/tüketilmiş/henüz aktive olmayan challenge.
- 401 refresh_token_reused: tüm oturumlar iptal; yeni OTP giriş gerekir.
- 401 /users/me: token, issuer/audience/imza/süre veya DB session/hesap durumu geçersiz.
- 403 account_unavailable: OTP doğrulandı ama hesap normal girişe kapalı.
- Flyway non-empty schema: mevcut DB için inceleme ve migration gerekir; volume silme.
