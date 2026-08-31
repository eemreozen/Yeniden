# YENİDEN — Geliştirme kuralları

Bu dosya `docs/PROJECT_GUIDE.md`, domain/API dokümanları ve ADR'lerden çıkarılan çalışma kurallarını; identity incelemesinin güvenlik ve doğrulama ilkeleriyle birleştirir. `MEMORY.md` mevcut durum/eksikleri, bu dosya çalışma yaklaşımını tutar. Kullanıcının güncel talebi kapsamı belirler; burada yazan planlar tek başına uygulama yetkisi değildir.

## 1. Kapsam ve kaynak kullanımı

1. Önce `MEMORY.md` ve görevle ilgili kaynakları oku. Memory'yi güncel kaynak kodun yerine koyma.
2. İnceleme ile uygulamayı ayır. 2026-08-30 hazırlık talebinde yalnızca bağlam/kurallar dosyaları yazılır; uygulama, migration, POM ve runtime ayarlarına dokunulmaz. Sonraki geliştirme talebi kendi kapsamında bu aşamayı değiştirir.
3. Ana odak `identity-service`dir. Gerekli common/gateway/build entegrasyonunun nedenini ve etkilenen sözleşmeyi açıkla. Diğer servislerin ilgisiz teknik borcunu aynı işe katma.
4. “Kodda mevcut”, “dokümanda hedef”, “öneri”, “karar bekliyor” ve “çalıştırılarak doğrulandı” ayrımını koru. README veya roadmap etiketini test kanıtı sayma.
5. Dokümanlar çatışıyorsa farkı kaydet; ürün davranışını değiştiren kararı sessizce verme. Rutin ve kapsam içi teknik tercihleri gerekçelendirerek ilerlet.
6. Mevcut kullanıcı değişikliklerini koru. Talep edilmedikçe commit/push, deploy, gerçek SMS gönderimi veya veri silme yapma.

## 2. Mimari ve servis sahipliği

1. Java 21 ve mevcut Maven multi-module yapısını koru. Sürüm yükseltme veya mimari dönüşümü sırf bu çalışma başladı diye yapma.
2. `com.yeniden.identity` altında mevcut katmanları koru: `controller`, `dto`, `service`, `repository`, `domain`. Gerektiğinde odaklı `security`, `config`, `listener`, adapter paketleri eklenebilir.
3. Controller HTTP/validation/delegation yapar; OTP, Redis, token ve iş kurallarını içermez. Servis iş akışını, repository persistence'ı, adapter dış sistemi yönetir.
4. Servis sözleşmeleri odaklı interface'ler kullanır; bağımlılıklar constructor injection ile alınır. KISS ve SOLID uygula; gereksiz soyutlama veya devasa tek servis oluşturma.
5. Entity dış API'ye doğrudan dönmez. İstek DTO'su, kendi profil DTO'su, public profil DTO'su ve iç servis sözleşmeleri amaçlarına göre ayrılır.
6. Identity yalnızca kendi şemasına erişir. Şemalar arası FK, SQL join veya başka servisin repository/entity bağımlılığı ekleme. Aynı şema içinde gerekli FK/kısıtlar kullanılabilir.
7. Cüzdan, ledger, puan basma, rozet/seviye, ilan ve teslimat iş kuralları identity'ye taşınmaz. Profilde gösterilecek birleşik veriler için ayrıca sözleşme gerekir.
8. ADR-003 uyarınca gamification/notification iş akışlarına senkron servis bağımlılığı ekleme. Profil okuma ihtiyacını bu kuralla uzlaştır; mevcut olmayan entegrasyonu varmış gibi sunma.
9. Ortak modüle yalnızca gerçekten ortak sözleşmeleri koy. Identity'ye özel security/config/iş mantığını diğer servislere yan etkiyle taşıma; dependency scope ve gateway WebFlux uyumunu kontrol et.

## 3. Telefon ve OTP güvenliği

1. Telefon sahipliği doğrulanmadan kullanıcı adına oturum/token üretme. Eski OTP'siz register yolu açık bırakılarak yeni auth akışı bypass edilemez.
2. Telefonu kararlaştırılan ülke politikasıyla E.164'e normalize et; aynı numaranın farklı gösterimleri yeni hesap oluşturmamalı. Tekilliği DB kısıtı da korumalı; yarış koşullarını ele al.
3. OTP için kriptografik güvenli rastgelelik kullan. OTP'yi API yanıtına veya normal loglara koyma; gerçek SMS gönderimini bir adapter arkasında tut.
4. Test/geliştirme adapter'i açıkça ayrılmış olmalı; üretimde sabit kod, doğrulama bypass'ı veya kod gösterimi etkinleşmemeli.
5. Mevcut 180 saniyelik OTP TTL gereksinimini yapılandırılabilir tut. Güvenli doğrulayıcı saklama tasarla; 6 haneli kod için yalnızca anahtarsız hızlı hash yeterli kabul edilmemeli.
6. OTP doğrulama/tüketme ve deneme sayaçları Redis'te atomik olmalı. Yanlış deneme kontrolsüzce TTL uzatmamalı; başarılı kod eşzamanlı iki istekte iki kez kullanılamamalı.
7. İstek limitleri numara başına 3/saat, IP başına 10/saat; 429 yanıtında `Retry-After`. OTP verify deneme ve resend politikası ayrıca kararlaştırılır; exchange teslim kodu kuralını OTP'ye yanlışlıkla aktarma.
8. IP rate limit için istemcinin gönderdiği herhangi bir forwarded başlığa güvenme; güvenilen proxy sınırını tanımla. Telefon/IP gibi verileri gereksiz loglama ve hata gövdelerinde sızdırma.
9. OTP doğrulaması, kullanıcı oluşturma, `phoneVerifiedAt` ve oturum üretimi arasındaki hata/tekrar deneme senaryolarını tasarla. Redis ve PostgreSQL tek ortak transaction gibi ele alınamaz.

## 4. Token, oturum ve yetkilendirme

1. Access JWT 15 dakika; rotating refresh 30 gün ve DB'de hash olarak saklanır. Kriptografik işlemlerde doğrulanmış kütüphaneler kullan; kendi JWT algoritmanı yazma.
2. `sub` doğrulanmış kullanıcı UUID'sidir; `nbh`, `roles`, `exp` sözleşmesini koru. Telefon, açık adres, bakiye, seviye ve güven skoru gibi hassas/değişken verileri JWT'ye ekleme.
3. İmza/algoritma, zaman ve seçilen issuer/audience politikası doğrulanır. Anahtarlar/secret'lar kaynak koda veya Git'e girmez.
4. Refresh tüketimi ve yeni token üretimi atomik olmalı. Kullanılmış token tekrar sunulduğunda doküman gereği kullanıcının tüm oturumlarını iptal et; tekrar kullanımı tespit edecek kayıt geçmişini koru.
5. Oturum iptalini yazdıktan sonra exception atılması bu iptali transaction rollback ile geri almamalı. Aynı ilke hatalı deneme ve güvenlik sayaçları için geçerlidir.
6. Logout, suspend/delete ve refresh-reuse durumlarının henüz süresi dolmamış access JWT'lere etkisini açıkça tanımla. Yalnız refresh iptalini “anında tüm erişim kapandı” olarak anlatma.
7. Mevcut kullanıcının kimliğini istemcinin body/query içindeki `userId`, `ownerId` veya rolünden alma; doğrulanmış principal kullan. Yetki ve hesap durumu ayrı ayrı kontrol edilir.
8. Varsayılan rol `USER`; istemci kendine `ADMIN`/`MODERATOR` atayamaz. Askıya alınmış/silinmiş hesap normal kullanıcı erişimi alamaz; politika test edilir.
9. Identity doğrudan porttan erişildiğinde de korunmalı. Gateway filtresi tek başına servis yetkilendirmesinin yerine geçmez. Diğer servislerdeki güvenlik borcu ayrı kapsam olarak bildirilir.

## 5. Profil, konum ve güven skoru

1. Public profil telefon, e-posta veya konum döndürmez. Telefonla kullanıcı arama ucu kimlik/veri sızdıran herkese açık bir uç olarak kalamaz.
2. Güncellenebilir alanları açık bir izin listesiyle sınırla. Profil isteğiyle rol, status, phoneVerifiedAt veya trust skoru değiştirilemez.
3. Açık ev adresi saklama. Konumu kararlaştırılmış yaklaşık konum politikasına göre işle; mahalleyi güvenilir veriyle ata. İlk girişte mahalle eksikliği ve poligon dışı nokta davranışını tanımla.
4. Trust formülünü `docs/04-ecocoin-rules.md` ile aynı uygula; sonucu 0–100'e sıkıştır. Yalnız doğrulanmış şikayetler ceza verir. Formül, `<30` ve `<15` eşikleri sınır testlerine sahip olmalı.
5. Hesap yaşına bağlı skorun zamanla güncellenmesini düşün. Skor/sayaçlar istemciden kabul edilmez; güvenilir olaylardan türetilir ve yeniden hesaplanabilir.
6. Hesap silme başka servis şemalarında doğrudan silme değildir. Identity oturum/hesap durumunu yönetir; ilan kaldırma ve ledger anonimleştirme ilgili servisin sözleşmesiyle yürür. Saklama/yeniden kayıt politikası uydurulmaz.

## 6. Persistence, olaylar ve HTTP sözleşmesi

1. Schema değişiklikleri sürümlü ve veri koruyan migration ile planlanır. Mevcut `ddl-auto: update` davranışını üretim migration stratejisi olarak sürdürme; geçişi ilgili kapsamda açıkça yap.
2. DB kısıtları, transaction sınırları ve yarış koşulları birlikte değerlendirilir. Sadece önce `exists` kontrolü yapmak tekillik/atomiklik garantisi değildir.
3. API zamanları ISO-8601 UTC'dir. Yeni zamana bağlı kurallarda test edilebilir saat kaynağı kullan; mevcut `LocalDateTime.now()` kodunu güvenilir UTC varsayımı sayma.
4. İş hataları `BaseException` veya uygun alt türleriyle ifade edilir. HTTP katmanında gerçek statü ve dokümandaki problem gövdesine dönüştürülür; 200 içinde `success=false` ile auth/validation hatası gizlenmez.
5. Alan doğrulaması ve kararlı hata code'ları test edilir. API önekleri, başarı zarfı ve hata formatındaki değişiklikler gateway/istemci sözleşmesiyle birlikte ele alınır.
6. Consumer'lar event kimliği veya kararlaştırılmış güvenilir iş anahtarıyla idempotent olmalı; event işaretleme ve yerel veri değişikliği aynı transaction'da gerçekleşmeli.
7. Identity consumer'ı kendine ait queue kullanır; ecocoin kuyruğunu paylaşarak mesajları yarışmalı tüketmez. Event adları/yükleri mevcut değilse bunları önceden varmış gibi kullanma.
8. Güvenilir yayın için yerel DB işlemiyle atomik outbox ve tekrar deneme tasarımını değerlendir. Publish hatasını loglayıp yutmayı teslim garantisi olarak kabul etme. Başka servisin outbox işi otomatik olarak identity kapsamına eklenmez.

## 7. Doğrulama ve teslim

1. Anlamlı iş/güvenlik davranışı değişikliklerini JUnit 5/Mockito ile doğrula. Boş assertion, yalnız getter/setter veya implementasyonu aynen tekrar eden testlerle kapsama görüntüsü üretme.
2. Auth için en az yanlış/süresi dolmuş/tekrar kullanılan OTP, rate limit, token doğrulama, refresh rotasyonu/reuse, logout, hesap durumu, yetkisiz erişim ve public veri sızıntısı senaryolarını kapsa.
3. Redis atomikliği, PostgreSQL tekilliği/migration, transaction rollback ve eşzamanlı refresh için gerçek altyapıyla entegrasyon testleri gerekir; mock testlerini bunların kanıtı sayma.
4. Java 21 ve Maven/test keşfi önce doğrulanır. `./mvnw -pl identity-service -am test` ve ilgili reactor testlerinde gerçekten çalışan test sayısı rapordan kontrol edilir. Son kanıt MEMORY ve task günlüğünde tutulur; eski raporları yeni çalıştırma sayısına ekleme.
5. Build, unit test, integration test ve gateway smoke test sonuçlarını ayrı raporla. Koşmayan testleri ve ortam engellerini açıkça belirt; “%100” veya “tamamlandı” iddiası kanıt gerektirir.
6. Yalnız dokümantasyon değişiklikleri için uygulama testi ekleme. Dosya yolları, içerik tutarlılığı ve diff kapsamını doğrula.
7. Her tamamlanan dilimden sonra memory'deki mevcut durum, açık kararlar ve doğrulama kaydını güncelle. Başlangıçta olmayan bir özelliği ancak uygulanıp doğrulanınca “mevcut” olarak işaretle.
