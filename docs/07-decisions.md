# Mimari Kararlar (ADR)

Her kayıt: bağlam → karar → sonuç → reddedilen alternatif.

---

## ADR-001 — Çoklu Mikroservis Mimarisi (Multi-Microservice Architecture)

**Bağlam.** YENİDEN projesi 8 mantıksal alana (kimlik, katalog, eşleşme/teslimat, puan, oyunlaştırma, bildirim, moderasyon, yapay zeka) sahiptir. Proje ekibi büyümekte olup, farklı mikroservislerin farklı geliştiriciler tarafından bağımsız olarak geliştirilmesi, test edilmesi ve deploy edilmesi istenmektedir.

**Karar.** Java 21 + Spring Boot 3 ile Maven Multi-Module yapısında **Çoklu Mikroservis Mimarisi**. Her mikroservis bağımsız bir Spring Boot uygulaması ve Docker konteyneri olarak çalışır. Veritabanı seviyesinde PostgreSQL üzerinde şema izolasyonu (`schema-per-service`) uygulanır; şemalar arası yabancı anahtar (FK) kullanılmaz.

**Sonuç.** Servisler bağımsız geliştirilebilir, ölçeklenebilir ve deploy edilebilir. Takım arkadaşları farklı mikroservisler üzerinde çakışma yaşamadan çalışabilir. Karşılığında servisler arası veri tutarlılığı Domain Event'ler ile (asenkron) sağlanır.

**Reddedilen: Tekil Monolit.** Ekip üyelerinin bağımsız çalışmasını zorlaştırdığı ve mikroservis dünyasını öğrenme hedefine uymadığı için tercih edilmedi.

---

## ADR-002 — Tek PostgreSQL + PostGIS (Şema İzolasyonu)

**Bağlam.** İki farklı erişim deseni var: ilişkisel bütünlük (ledger, durum makineleri) ve coğrafi yakınlık sorguları (ilan arama, toplama noktası).

**Karar.** Her mikroservis kendi şemasına (`identity`, `catalog`, `exchange`, `ecocoin`, `gamification` vb.) sahiptir. PostGIS eklentisi coğrafi arama için aktif edilir.

**Sonuç.** Tek veritabanı sunucusu ile yüksek performans ve düşük bellek kullanımı sağlanırken, mikroservisler arası veritabanı seviyesinde izolasyon elde edilir.

---

## ADR-003 — Oyunlaştırma ve Bildirim event tabanlıdır

**Bağlam.** Rozet, seviye, görev, sıralama ve bildirim mantığı sık değişecektir. Bunların teslim onayı gibi kritik bir akışı yavaşlatması veya bozması kabul edilemez.

**Karar.** `gamification-service` ve `notification-service` yalnızca domain event dinler; hiçbir modül onları senkron çağırmaz. Event'ler **transactional outbox / event broker** ile yayınlanır. Dinleyiciler `ProcessedEvent` tablosuyla idempotenttir.

**Sonuç.** Rozet veya bildirim motoru hata verse bile teslim tamamlanır ve puan verilir; rozet/bildirim asenkron işlenir.

---

## ADR-004 — Double-entry ledger

**Bağlam.** Eco-Coin dış dünyada karşılığı olan bir kaynak. Yanlış hesaplanan veya çift verilen puan doğrudan mali kayıp ve güven kaybı demek.

**Karar.** Bakiye hiçbir yerde sütun olarak tutulmaz. Her hareket, net toplamı sıfır olan en az iki `CoinEntry` üretir. Kayıtlar değiştirilmez ve silinmez; düzeltme `ADJUSTMENT` ile ters kayıt atılarak yapılır. Her yazma `idempotency_key` ister.

**Sonuç.** Sistemin toplam bakiyesi her zaman sıfır olmalıdır. "Bu puan nereden geldi" sorusu her zaman cevaplanabilir.

---

## ADR-005 — Çift taraflı teslim kodu (Exchange Service)

**Bağlam.** Sistemin tek gerçek riski, fiziksel teslim olmadan puan üretilmesi.

**Karar.** Rezervasyon onaylandığında 6 haneli tek kullanımlık bir kod üretilir. Kodu **alan taraf** görür, **paylaşan taraf** girer. Kod hash'lenmiş saklanır, 72 saat geçerlidir, 5 hatalı denemede teslim geçersizleşir. Onay anında risk sinyalleri değerlendirilir; iki veya daha fazla sinyal birlikte gelirse teslim `PENDING_REVIEW` olur.

**Sonuç.** Sahte teslim için en az iki cihaz, iki doğrulanmış telefon numarası ve fiziksel buluşma iddiası gerekir.

---

## ADR-006 — AI sınıflandırma port-adapter ile ertelendi (WasteAI Service)

**Bağlam.** Fotoğraftan atık türü tanıma ürünün vitrin özelliği ama MVP için gerekli değil ve eğitim verisi henüz yok.

**Karar.** `WasteClassifier` arayüzü `wasteai-service` altında tanımlanır, MVP'de kural tabanlı bir stub döner. Çağrı senkron ama **zorunlu değildir**: 2 saniye zaman aşımı, hata durumunda akış önerisiz devam eder.

---

## ADR-007 — Konum gizliliği: ilan başına sabit kaydırma

**Bağlam.** İlan konumu aramanın merkezinde ama aynı zamanda kullanıcının ev adresine yakın bir bilgi.

**Karar.** Açık adres hiçbir yerde saklanmaz. İlanın `approx_point` değeri gerçek noktadan ~250 m kaydırılarak üretilir ve bu kaydırma **ilan başına bir kez** hesaplanıp sabit tutulur. `exact_point` yalnızca `RESERVED` durumunda iki tarafa döner. Mesafeler 100 m'ye yuvarlanır.
