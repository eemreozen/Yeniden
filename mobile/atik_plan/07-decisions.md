# Mimari Kararlar (ADR)

Her kayıt: bağlam → karar → sonuç → reddedilen alternatif.

---

## ADR-001 — Modüler monolit

**Bağlam.** GreenTrack'in 10 mantıksal modülü var ve bunlar birbirine sıkı bağlı akışlarda buluşuyor (ilan → talep → teslim → puan). Ekip küçük, ürün henüz doğrulanmadı, trafik bilinmiyor.

**Karar.** Tek deploy edilen bir modüler monolit. Modül sınırları paket düzeyinde (`com.greentrack.<modul>.api` dışına erişim yasak) ve ArchUnit testiyle zorlanıyor. Veritabanı şemaları modül başına ayrı, şemalar arası yabancı anahtar yok.

**Sonuç.** Tek transaction'da tutarlılık sağlanabiliyor; teslim onayı ve ilan durumu güncellemesi dağıtık işlem gerektirmiyor. Deploy ve yerel geliştirme basit. Karşılığında tüm modüller birlikte ölçekleniyor ve bir modülün hatası tüm süreci etkileyebiliyor. Sınırlar disiplinli tutulduğu için ileride bir modülü çıkarmak şema ayrımı ve event sözleşmesi hazır olduğundan makul maliyetli.

**Reddedilen: mikroservisler.** Ürün doğrulanmadan servis sınırı çizmek, yanlış yerden bölme riski taşır. Ayrıca teslim onayı + puan verme akışının dağıtık tutarlılığı (saga, telafi işlemleri) ekibin bu aşamada ödemek istemeyeceği bir bedel.

---

## ADR-002 — Tek PostgreSQL + PostGIS

**Bağlam.** İki farklı erişim deseni var: ilişkisel bütünlük (ledger, durum makineleri) ve coğrafi yakınlık sorguları (ilan arama, toplama noktası).

**Karar.** Her ikisi de PostgreSQL + PostGIS üzerinde. `geography` tipi, GiST indeksi, `ST_DWithin` ile yarıçap araması.

**Sonuç.** Tek veri deposu, tek yedekleme stratejisi, tek transaction sınırı. PostGIS bu ölçekte fazlasıyla yeterli — mahalle bazlı yarıçap sorguları milyonlarca satırda da indeksle hızlı çalışır.

**Reddedilen: ayrı arama motoru (Elasticsearch/OpenSearch).** Coğrafi + metin araması için cazip ama ikinci bir veri deposu, senkronizasyon gecikmesi ve tutarsızlık ihtimali getiriyor. Metin araması ihtiyacı büyürse PostgreSQL full-text ile başlanır, gerçekten yetmezse o zaman ayrı motor değerlendirilir.

---

## ADR-003 — Oyunlaştırma event tabanlı, transactional outbox ile

**Bağlam.** Rozet, seviye, görev ve sıralama mantığı sık değişecek, kural sayısı artacak. Bunların teslim onayı gibi kritik bir akışı yavaşlatması veya bozması kabul edilemez.

**Karar.** `gamification` ve `notification` yalnızca domain event dinler; hiçbir modül onları senkron çağırmaz. Event'ler **transactional outbox** ile yayınlanır: iş verisi ve outbox kaydı aynı transaction'da yazılır, ayrı bir dispatcher teslim eder. Dinleyiciler `ProcessedEvent` tablosuyla idempotenttir.

**Sonuç.** Rozet motoru hata verse bile teslim tamamlanır ve puan verilir; rozet gecikmeli işlenir. Kural değişikliği çekirdek akışı riske atmaz. Karşılığında rozetler anlık değil, saniyeler mertebesinde gecikmeli görünür — bu yüzden `/handovers/{id}/confirm` yanıtındaki `newBadges` alanı "en iyi çaba" olarak belgelendi.

**Reddedilen: doğrudan senkron çağrı.** Basit görünüyor ama teslim onayını rozet mantığına bağımlı hale getiriyor ve modüller arasında döngüsel bağımlılık yaratıyor.

**Reddedilen: harici mesaj kuyruğu (Kafka/RabbitMQ) — şimdilik.** Outbox + veritabanı tablosu bu ölçekte yeterli ve operasyonel yük getirmiyor. Event hacmi ölçülüp gerçekten sorun olursa dispatcher'ın arkasına kuyruk takmak yerel bir değişiklik.

---

## ADR-004 — Double-entry ledger

**Bağlam.** Eco-Coin dış dünyada karşılığı olan bir kaynak. Yanlış hesaplanan veya çift verilen puan doğrudan mali kayıp ve güven kaybı demek.

**Karar.** Bakiye hiçbir yerde sütun olarak tutulmaz. Her hareket, net toplamı sıfır olan en az iki `CoinEntry` üretir. Kayıtlar değiştirilmez ve silinmez; düzeltme `ADJUSTMENT` ile ters kayıt atılarak yapılır. Her yazma `idempotency_key` ister.

**Sonuç.** Sistemin toplam bakiyesi her zaman sıfır olmalıdır ve bu gece işiyle denetlenir; sapma anında yakalanır. "Bu puan nereden geldi" sorusu her zaman cevaplanabilir. Karşılığında bakiye sorgusu bir toplama işlemi gerektirir — hareket sayısı büyüdüğünde checkpoint kaydı eklenecek şekilde tasarlandı.

**Reddedilen: `user.balance` sütunu + hareket logu.** Okuması hızlı ama iki kaynak arasında sessiz sapma üretir; en kötü hata türü budur — kimse fark etmeden bakiyeler yanlışlaşır.

---

## ADR-005 — Çift taraflı teslim kodu

**Bağlam.** Sistemin tek gerçek riski, fiziksel teslim olmadan puan üretilmesi. Ödül gerçek para değeri taşıdığı için bu risk teorik değil.

**Karar.** Rezervasyon onaylandığında 6 haneli tek kullanımlık bir kod üretilir. Kodu **alan taraf** görür, **paylaşan taraf** girer. Kod hash'lenmiş saklanır, 72 saat geçerlidir, 5 hatalı denemede teslim geçersizleşir. Onay anında risk sinyalleri (cihaz, IP, konum, tekrar eden çift, hesap yaşı, güven skoru) değerlendirilir; iki veya daha fazla sinyal birlikte gelirse teslim `PENDING_REVIEW` olur ve puan moderatör onayına kadar verilmez.

**Sonuç.** Sahte teslim için en az iki cihaz, iki doğrulanmış telefon numarası ve fiziksel buluşma iddiası gerekir. Tavanlarla birlikte kazanç ekonomik olmayan seviyede kalır. Karşılığında meşru kullanıcıların bir kısmı incelemeye takılabilir; bu yüzden inceleme oranı ölçülen bir metrik ([`06-roadmap.md`](06-roadmap.md)) ve %5'i aşarsa kurallar gevşetilir.

**Reddedilen: tek taraflı onay.** Paylaşanın "teslim ettim" demesi yeterli olsaydı puan üretmek tek hesapla mümkün olurdu.

**Reddedilen: fotoğraflı teslim kanıtı zorunluluğu.** Doğrulaması yine manuel, kullanıcı için zahmetli ve fotoğraf tekrar kullanılabildiği için güvenlik kazancı sınırlı.

---

## ADR-006 — AI sınıflandırma port-adapter ile ertelendi

**Bağlam.** Fotoğraftan atık türü tanıma ürünün vitrin özelliği ama MVP için gerekli değil ve eğitim verisi henüz yok.

**Karar.** `WasteClassifier` arayüzü baştan tanımlanır, MVP'de kural tabanlı bir stub döner. Çağrı senkron ama **zorunlu değildir**: 2 saniye zaman aşımı, hata durumunda akış önerisiz devam eder. Tüm tahminler ve kullanıcının kabul edip etmediği `AiClassification` tablosuna kaydedilir.

**Sonuç.** Mimari model olmadan kilitlenmiyor; Faz 1–2 boyunca biriken gerçek veri Faz 3'ün eğitim setini oluşturuyor. Model kalitesi ölçülebilir (`accepted_by_user` oranı) ve eşiğin altındaysa öneri hiç gösterilmiyor — kötü öneri, öneri olmamasından daha zararlı.

**Reddedilen: hazır üçüncü parti görsel API ile başlamak.** Genel amaçlı modeller "koli" ile "karton atık" ayrımını ürünün ihtiyaç duyduğu hassasiyette yapmıyor; ayrıca her fotoğrafı dışarı göndermek gizlilik ve maliyet yükü.

---

## ADR-007 — Konum gizliliği: ilan başına sabit kaydırma

**Bağlam.** İlan konumu aramanın merkezinde ama aynı zamanda kullanıcının ev adresine yakın bir bilgi. Sızması ciddi bir güvenlik sorunu.

**Karar.** Açık adres hiçbir yerde saklanmaz. İlanın `approx_point` değeri gerçek noktadan ~250 m kaydırılarak üretilir ve bu kaydırma **ilan başına bir kez** hesaplanıp sabit tutulur. `exact_point` yalnızca `RESERVED` durumunda, yalnızca iki tarafa döner. Arama sonuçlarındaki mesafeler 100 m'ye yuvarlanır. Arayüz varsayılan olarak kamusal buluşma noktası önerir.

**Sonuç.** Bir ilanı defalarca sorgulayan biri her seferinde aynı noktayı görür, ortalama alarak gerçek konumu çözemez. Mesafe yuvarlaması, farklı noktalardan üçgenleme yaparak konum daraltmayı zorlaştırır.

**Reddedilen: her istekte yeniden kaydırma.** İlk bakışta daha güvenli görünüyor ama tam tersi: yeterince örnek toplayan biri kaydırmaların ortalamasını alarak gerçek noktayı bulur.

**Reddedilen: yalnızca mahalle merkezini göstermek.** Gizlilik açısından en güçlüsü ama "yürüme mesafesinde mi" sorusunu cevaplayamadığı için ürünün temel değerini zayıflatıyor.
