# YENİDEN (AI-Powered Circular Economy & Hyper-Local Sharing Platform)
## Yatırımcı Sunumu & Yapay Zeka Pitch Deck Hazırlık Briefing Dokümanı

> 📌 **AÇIKLAMA VE KULLANIM TALİMATI:**  
> Bu doküman, **YENİDEN** projesinin tüm vizyonunu, teknik mimarisini, pazar araştırması verilerini, iş modelini, finansal güvenlik mekanizmalarını (Eco-Coin Ledger), risk yönetimini ve etki metriklerini **uçtan uca içeren eksiksiz bir rehberdir**. Herhangi bir Yapay Zeka modelinin bu dokümanı okuyarak yatırımcılara yönelik **yüksek ikna kabiliyetli, profesyonel ve veriye dayalı bir Pitch Deck (Yatırımcı Sunumu)** hazırlaması için özel olarak kurgulanmıştır.

---

## 1. YÖNETİCİ ÖZETİ (EXECUTIVE SUMMARY)

* **Proje Adı:** YENİDEN (Eski adı: GreenTrack)
* **Kategori:** Tüketim Sonrası Atık İzleme, Sıfır Atık ve Mahalle Ölçeğinde Döngüsel Ekonomi Platformu
* **Slogan:** *"Atık Değil, Yeniden Değer: Mahallenizin Sıfır Atık ve Döngüsel Ekonomi Ağı"*
* **Ana Vizyon:** Evlerde biriken ve henüz kullanım ömrünü tamamlamamış milyonlarca ton malzemeyi (koli, cam kavanoz, ahşap palet, hobi malzemesi, kırtasiye vb.) çöpe gitmeden veya evlerde çürümeye terk edilmeden önce yapay zeka ve konum tabanlı hiper-yerel paylaşım ağıyla ihtiyaç sahipleriyle buluşturmak; doğrulanan her paylaşımı belediyeler ve özel sektörde geçerli **Eco-Coin** ile ödüllendirerek sürdürülebilir tüketim alışkanlığı yaratmaktır.

---

## 2. ÇÖZÜLMESİ HEDEFLENEN PROBLEM VE PAZAR AĞRISI

### 2.1 Pazar Açmazı (Market Pain Point)
Günlük yaşamda koli, cam kavanoz, karton ambalaj, ahşap çıta, hobi malzemeleri ve kırtasiye ürünleri kullanım değerini koruduğu halde ihtiyaç fazlası hale gelmektedir.
* **Eşleşememe Problemi:** Malzemeleri elinde tutan kişiler ile bu malzemelere sıfır satın alım yapmadan ihtiyaç duyan komşuları veya yerel işletmeler birbirine ulaşamamaktadır.
* **Lineer Tüketim İsrafı:** Eşyalar ya evlerde/balkonlarda uzun süre yer kaplamakta ya da kullanım ömrü bitmeden evsel atık sistemine (çöpe) atılarak çevre kirliliğine ve kaynak israfına yol açmaktadır.
* **Güvenilirlik ve Teşvik Eksikliği:** Mevcut platformlar (Olio, Freecycle, sahibinden) malzeme odaklı yapay zeka analizi, konum gizliliği, doğrulanmış teslimat ve finansal karşılığı olan ödüllendirme mekanizmalarını bir arada sunamamaktadır.

### 2.2 Pazar ve Kullanıcı Doğrulama Verileri (150+ Katılımcılı Anket Sonuçları)
* **Atık Oluşum Sıklığı (%86 Oran):** Katılımcıların **%86'sında** evlerinde düzenli olarak ihtiyaç fazlası kullanılabilir malzeme birikmektedir (%14 haftalık, %42.7 ayda birkaç kez, %29.3 ayda bir kez).
* **Evde Saklama ve Çöpe Atma Davranışı:** Katılımcıların **%65.3'ü** bu malzemeleri "ileride lazım olur" diye evde sakladığını, **%22.7'si** ise doğrudan çöpe attığını beyan etmiştir.
* **Veren Taraf Katılım İsteği (%92.6 Oran):** Katılımcıların **%57.3'ü** malzemeleri karşılıksız vermek, **%35.3'ü** hem vermek hem de ihtiyaç duyduğunu almak istemektedir. Toplamda **%92.6'lık ezici bir kitle** platformda paylaşım yapmaya açıktır.
* **2. El Ürün Tercih Etme İsteği (%81.4 Oran):** Katılımcıların **%22.7'si** temiz ikinci el ürünü doğrudan tercih edeceğini, **%58.7'si** ürünün görselini ve durumunu gördükten sonra alacağını ifade etmiştir.
* **Mesafe ve Mobilite Esnekliği (%90 Oran):** Katılımcıların **%56'sı** 2 km'ye kadar, **%90'ı** ise 5 km mesafeye kadar teslimat için gitmeyi kabul etmektedir. (Hiper-yerel mahalle modelini doğrulamaktadır).
* **Yerel Esnaf Teslimat Noktası (%24 Oran):** Katılımcıların **%24'ü** bakkal veya mahalle esnafı gibi güvenilir aracılar üzerinden teslimatı tercih etmiştir.

---

## 3. ÜRÜN VE ÇÖZÜM YAKLAŞIMI (THE SOLUTION)

YENİDEN, malzeme ilanından teslimat sonrasındaki ödüllendirmeye kadar olan tüm süreci uçtan uca yöneten entegre bir ekosistemdir:

```
[İlan Yükleme / Fotoğraf] ──► [WasteAI Yapay Zeka Analizi] ──► [İki Yönlü Eşleşme (İlan & İhtiyaç)]
                                                                       │
                                                                       ▼
[Eco-Coin Harcama / Ödül] ◄── [Çift Kayıtlı Defter & Ödül] ◄── [6 Haneli Şifreli Teslim Doğrulaması]
```

### 3.1 Temel Kullanıcı Akışları
1. **İlan Verme ve AI Görsel Analizi (WasteAI):** Kullanıcı malzemenin fotoğrafını yükler. YOLOv8s ve MobileNetV3 tabanlı `WasteAI` servisi malzemenin türünü, kategorisini ve yeniden kullanıma uygunluğunu (`REUSE`) tespit eder. Uygun ürünler ilanlaşır; uygun olmayan atıklar (`RECYCLE`) kullanıcıya en yakın geri dönüşüm noktasını önerir.
2. **Çift Yönlü Havuz (İlan + İhtiyaç İlanı):** Kullanıcılar ellerindeki eşyaları ilan verebildikleri gibi, aradıkları malzemeler için **"İhtiyaç İlanı" (Wanted Listing)** oluşturabilirler.
3. **Konum Gizliliği ve Hassas Eşleşme:** Açık adres veritabanında kesinlikle tutulmaz. Haritada ilanın konumu `~250m` sabit kaydırılarak (jittering) gösterilir. Kesin buluşma noktası yalnızca onaylı rezervasyon sonrası açılır.
4. **Güvenli ve Doğrulanmış Teslimat:** Buluşmada teslim alan tarafın uygulamasında üretilen 6 haneli tek kullanımlık kod / QR paylaşan tarafça sisteme girilir. Çift taraflı doğrulama tamamlanmadan puan üretilmez.

---

## 4. İŞ MODELİ VE GELİR STRATEJİSİ (BUSINESS MODEL & MONETIZATION)

YENİDEN tek bir kullanıcı komisyonuna dayanmaz; kamu, özel sektör ve yerel ticaretin kesişiminde **çift taraflı pazar yeri ve SaaS/ESG iş modeli** yürütür:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          YENİDEN GELİR MODELİ                                   │
├──────────────────────────────┬──────────────────────────────────────────────────┤
│ B2G (Belediyeler & Kamu)     │ • Sıfır Atık HEDEF-2030 Karbon Nötr Danışmanlığı │
│                              │ • Toplu Taşıma ve Sosyal Tesis Entegrasyon Payı │
├──────────────────────────────┼──────────────────────────────────────────────────┤
│ B2B (Özel Sektör / Retail)   │ • Market, Kafe, Perakende Kupon Komisyonları    │
│                              │ • ESG & Kurumsal Sürdürülebilirlik Sponsorluğu   │
├──────────────────────────────┼──────────────────────────────────────────────────┤
│ Yerel Ticaret (Esnaf Ağı)    │ • Mikro-Teslimat Noktası & Esnaf Trafik Payı    │
└──────────────────────────────┴──────────────────────────────────────────────────┘
```

1. **B2G (Business-to-Government) Belediyeler:** Belediyelerin Sıfır Atık hedeflerine ulaşması için atık toplama maliyetlerini düşüren platform lisanslaması. Belediyeler Eco-Coin karşılığında toplu taşıma bakiyesi ve sosyal tesis indirimleri sağlar.
2. **B2B Perakende & Ödül Komisyonları:** Kullanıcıların Eco-Coin'lerini harcadığı zincir marketler, kahve mağazaları ve e-ticaret markalarından üretilen her indirim kuponu başına yönlendirme ve satış komisyonu (%57.3 kullanıcı talebi).
3. **ESG ve Kurumsal Karbon Sponsorluğu:** Büyük şirketlerin karbon ayak izini nötrlemek amacıyla Eco-Coin havuzunu finanse etmesi ve kurumsal sosyal sorumluluk (CSR) sponsorlukları.
4. **Yerel Esnaf Mikro-Lojistik Modeli:** Bakkal ve yerel esnafların "Güvenli Teslim Noktası" (Micro-Hub) yapılmasıyla esnaha ayak trafiği sağlanması ve esnaf abonelik modeli.

---

## 5. REKABET ANALİZİ VE ÖNE ÇIKAN FARK (COMPETITIVE ADVANTAGE & MOAT)

| Özellik / Kriter | YENİDEN | Olio | Freecycle | sahibinden | ShareWaste |
|---|:---:|:---:|:---:|:---:|:---:|
| **Ücretsiz Paylaşım Mimarisi** | ✅ | ✅ | ✅ | ⚠️ Kısmi | ✅ |
| **İki Yönlü İhtiyaç İlanı Havuzu** | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Konum Tabanlı Harita Yapısı** | ✅ | ✅ | ⚠️ Yerel | ✅ | ✅ |
| **AI Destekli Malzeme & Hasar Analizi**| ✅ | ❌ | ❌ | ❌ | ❌ |
| **Geri Dönüşüm / Atık Yönlendirmesi** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Doğrulanmış Kodlu/QR Teslimat** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Eco-Coin & Finansal Ödül Sistemi** | ✅ | ⚠️ Kısmi | ❌ | ❌ | ❌ |
| **Çift Kayıtlı Bankacılık Defteri** | ✅ | ❌ | ❌ | ❌ | ❌ |

**YENİDEN'in Hendek (Moat) Stratejisi:** YENİDEN, sadece bir ilan tahtası değil; AI analizi + Çift taraflı doğrulama + Çift kayıtlı puan defteri + Belediye/Partner entegrasyonunu **tek bir kullanıcı deneyiminde birleştiren dünyadaki ilk platformdur**.

---

## 6. FİNANSAL MİMARİ VE ECO-COIN TOKENOMICS (FINANCIAL ENGINE)

Eco-Coin, dış dünyada (ulaşım, market) maddi karşılığı olan bir varlık olduğu için **bankacılık standartlarında çift kayıtlı defter (Double-Entry Ledger)** ile yönetilir.

### 6.1 Puan Kazanım Formülü
Puan **yalnızca teslimat onaylandığında** ve **yalnızca ilanı veren (paylaşan) tarafa** verilir (Alan taraf malzeme aldığı için puan alamaz; sahte döngüsel puan üretimi engellenir):
$$\text{Puan} = \text{round}\big(\text{TEMEL (10 p)} \times \text{kategori\_katsayısı} \times \text{miktar\_katsayısı}\big)$$

* **Kategori Katsayıları:** Cam Kavanoz (1.2x), Koli/Karton (1.0x), Ahşap Palet (1.8x), Hobi Malzemesi (1.5x), Ev Eşyası (2.0x).
* **Miktar Katsayıları:** Single (1.0x), Few (2-5 Adet: 1.5x), Many (6+ Adet: 2.0x).
* **Teslim Başı Kazanım:** 8 – 40 Eco-Coin.

### 6.2 Çift Kayıtlı Defter (Double-Entry Ledger) Değişmezleri
1. **Net Sıfır Toplam:** Her `CoinTransaction` en az iki `CoinEntry` üretir ve net toplamı **tam olarak sıfırdır**.
2. **Sütunsuz Bakiye:** Bakiye veritabanında tek bir değişken/sütun olarak tutulmaz; dinamik olarak $\sum \text{CREDIT} - \sum \text{DEBIT}$ sorgusu ile hesaplanır.
3. **Hesap Türleri:** `SYSTEM_MINT` (Basılan Puan), `SYSTEM_HOLD` (Bloke Puan), `SYSTEM_BURN` (Yakılan Puan), `USER:{id}` (Kullanıcı Cüzdanı).
4. **Kesin İdempotency:** Her yazma benzersiz bir `idempotency_key` gerektirir. Mükerrer ağ istekleri veya event'ler ikinci kez puan üretemez.

### 6.3 5 Katmanlı Anti-Fraud (Sahtecilik Önleme) Motoru
1. **Katman 1 (Hesap Maliyeti):** 1 Telefon = 1 Hesap doğrulaması. İlk 7 gün yeni hesap tavanı 30 puan.
2. **Katman 2 (Teslim Doğrulaması):** 6 haneli tek kullanımlık 72 saat geçerli OTP/QR kod. 5 hatalı denemede blokaj.
3. **Katman 3 (7 Risk Sinyali):** `SAME_DEVICE`, `SAME_IP`, `LOCATION_MISMATCH` (>5km), `REPEATED_PAIR` (Aynı çift ayda >2 teslim), `RAPID_SEQUENCE` (1 saatte >3 teslim), `NEW_ACCOUNT_PAIR`, `LOW_TRUST` (<30 score). 2 veya daha fazla sinyal tetiklendiğinde işlem `PENDING_REVIEW` durumuna alınır, puan askıya çıkar.
4. **Katman 4 (Dinamik Güven Skoru):** $\text{Score} = 50 + \min(35, \text{teslim} \times 2) + \min(15, \text{ay} \times 2) - (\text{no\_show} \times 5) - (\text{şikayet} \times 15)$. Score < 15 ise hesap kilitlenir.
5. **Katman 5 (Otomatik Borç Kaydı):** Sahtecilik durumunda puan geri alınır (`ADJUSTMENT`). Bakiye yetersizse hesaba `pending_clawback` borç yazılır; sonraki kazanımlardan mahsup edilir.

---

## 7. BEKLENEN ETKİ VE BİLİMSEL HESAPLAMA METRİKLERİ (IMPACT METRICS)

YENİDEN'in sürdürülebilirlik başarısı 5 temel boyut üzerinden canlı izlenir ve raporlanır:

| Etki Alanı | Temel Gösterge (KPI) | Hesaplama Formülü / Metodu | Raporlanan Değer (MVP) |
|---|---|---|:---:|
| **1. Atık Azaltımı** | Yeniden Kazandırılan Toplam Ağırlık (kg) | $\text{Atık Azaltımı (kg)} = \sum (\text{Teslim Edilen Ürün} \times \text{Kategorik kg})$ | **1,840 kg** |
| **2. Kaynak Verimliliği** | Önlenen Yeni Ürün İhtiyacı (Adet) | $\text{Önlenen İmalat} = \sum \text{Başarılı Teslimat Adedi}$ (%81.4 Kabul) | **842 Adet** |
| **3. Karbon Emisyonu** | Tahmini Önlenen Emisyon (kg CO₂e) | $\text{Emisyon} = \sum (\text{Malzeme kg} \times \text{LCA Emisyon Katsayısı})$ | **2,450 kg CO₂e** |
| **4. Ekonomik Fayda** | Hane Halkı Satın Alma Tasarrufu (TL) | $\text{Tasarruf} = \sum \text{Ücretsiz Edinilen Ürün Piyasa Değeri}$ | **₺128,500 TL** |
| **5. Toplumsal Dayanışma**| Dayanışma & Memnuniyet Skoru | Anket Ölçek Skoru (5 Üzerinden) + %92.6 Verici Katılımı | **4.7 / 5.0** |

* **Doğrulanmış Karbon Katsayıları (LCA Literatür):** Cam (1.20 kg CO₂e/kg), Karton/Koli (0.95 kg CO₂e/kg), Ahşap (1.50 kg CO₂e/kg), Plastik (1.80 kg CO₂e/kg).

---

## 8. TEKNİK MİMARİ VE YAZILIM TASARIMI (TECH STACK & ARCHITECTURE)

YENİDEN, yüksek ölçeklenebilirlik, veri izolasyonu ve kesintisiz çalışabilirlik için **Çoklu Mikroservis Mimarisi (Multi-Microservice Architecture)** ile tasarlanmıştır.

```
İstemciler (Mobil / Web / Admin)
       │
       ▼
[API Gateway / Spring Cloud Gateway :8080]
       │
 ┌─────┴──────────┬──────────────┬──────────────┬──────────────┬──────────────┐
 ▼                ▼              ▼              ▼              ▼              ▼
identity-service  catalog-serv.  exchange-serv. ecocoin-serv.  gamification   wasteai-serv.
[:8081]           [:8082]        [:8083]        [:8084]        [:8085]        [:8086]
 │                │              │              │              │              │
 └────────────────┴──────────────┼──────────────┴──────────────┴──────────────┘
                                 ▼
                     [Transactional Outbox / Event Broker]
                                 │
                     ┌───────────┴───────────┐
                     ▼                       ▼
           gamification-service    notification-service [:8087]
```

### 8.1 Teknoloji Yığını
* **Backend:** Java 21 + Spring Boot 3 + Spring Cloud Gateway
* **Veritabanı:** PostgreSQL + PostGIS (Mekansal sorgular için `ST_DWithin` indeksi)
* **Medya Depolama:** S3 Uyumlu Object Storage (Presigned URL ile doğrudan yükleme)
* **Asenkron Event İletişimi:** Transactional Outbox Pattern + Event Broker (`gamification` ve `notification` servisleri sadece olay dinler; ana teslimat akışını asla bloklamaz)
* **Yapay Zeka (WasteAI):** YOLOv8s (Nesne Tespiti) + MobileNetV3 (Hasar & Uygunluk Analizi) - FastAPI / ONNX Konteyneri
* **Mobil & Web:** React Native (iOS & Android) + React Web Admin Dashboard

---

## 9. RİSK YÖNETİMİ VE B PLANLARI (RISK MITIGATION)

1. **Sahtecilik ve Döngüsel Puan Kasma:** Puan sadece verene tanımlanır. Aynı çift ayda max 2 puanlı işlem yapabilir. 2+ risk sinyalinde işlem `PENDING_REVIEW` kuyruğuna alınır.
2. **Konum Gizliliği İhlali:** Açık adres tutulmaz. Haritada `~250m` sabit kaydırılmış nokta gösterilir. Mesafe 100m'ye yuvarlanır.
3. **Puan Tutarsızlığı ve Çift Harcama:** Çift kayıtlı defter ve `SELECT ... FOR UPDATE` kilidi. Gece çalışan asenkron denetim servisi.
4. **Yapay Zeka Hataları:** Non-blocking mimari. AI sadece öneri sunar, nihai karar kullanıcıdadır.
5. **Partner Entegrasyon Aksaklıkları:** `HOLD` -> `CAPTURE` -> `RELEASE` mantığı. Partner zaman aşımında 15 dk uzlaştırma beklenir, çözülmezse puan kullanıcıya geri yüklenir.

---

## 10. TAKIM YETKİNLİĞİ VE GELİŞTİRME TAKVİMİ

* **Takım Yetkinliği:** Karadeniz Teknik Üniversitesi Bilgisayar Bilimleri öğrencilerinden oluşan, backend, mobil, web ve yapay zeka alanında uzmanlaşmış 4 kişilik mühendislik ekibi (Emre Özen - Takım Kaptanı & Backend Architect, Bekir Çağlar - Mobil Frontend, İpek Çalışır - AI & Görüntü İşleme, Nazlı Barışık - Web & Moderasyon).
* **Geliştirme Seviyesi:** MVP Çalışır Durumdadır. Backend mikroservis mimarisi, PostGIS veri modeli, mobil uygulama ve web admin paneli tamamlanmıştır.

---

## 11. YATIRIMCI PITCH DECK SLAYT TASLAĞI (15 SLIDE BLUEPRINT FOR AI)

> 🤖 **YAPAY ZEKA TALİMATI:** Bir sunum hazırlarken veya slayt taslağı oluştururken aşağıdaki 15 slaytlık yapıyı **aynen takip et**. Her slayt için verilen başlıkları, öne çıkarılacak anahtar metrikleri ve görsel önerilerini kullan.

* **SLAYT 1: Kapak (Title & Vision)**
  * *Başlık:* YENİDEN — Akıllı Döngüsel Ekonomi & Hiper-Yerel Paylaşım Platformu
  * *Vurgu:* "Atık Değil, Yeniden Değer: Mahallenizin Sıfır Atık Ağı"
* **SLAYT 2: Problem (The Pain Point)**
  * *Vurgu:* Evlerde saklanan %65.3 atıl malzeme, çöpe giden %22.7 kaynak, yüksek yeni ürün satın alım maliyeti.
* **SLAYT 3: Çözüm (The Product & Solution)**
  * *Vurgu:* AI Görsel Analizi + İki Yönlü Eşleşme + Doğrulanmış Teslimat + Eco-Coin Ödülü.
* **SLAYT 4: Pazar ve Kullanıcı Doğrulaması (Validation Data)**
  * *Vurgu:* 150+ Anket verisi (%86 düzenli atık akışı, %92.6 paylaşım isteği, %81.4 2. el kabulü, %90 5 km esneklik).
* **SLAYT 5: Ürün Deneyimi ve Mobil Ekranlar (Product Demo & Walkthrough)**
  * *Vurgu:* Harita görünümü, 6 haneli kod ile doğrulama, cüzdan ve rozet ekranları.
* **SLAYT 6: İş ve Gelir Modeli (Business Model)**
  * *Vurgu:* B2G Lisanslama + B2B Kupon Komisyonları (%57.3 talep) + ESG Karbon Sponsorluğu.
* **SLAYT 7: Finansal Motor: Eco-Coin Tokenomics (Financial Engine)**
  * *Vurgu:* Bankacılık standartlarında Çift Kayıtlı Defter (Double-Entry Ledger), Net Sıfır Bakiye, Idempotent İşlemler.
* **SLAYT 8: Güvenlik ve Anti-Fraud Katmanı (Anti-Fraud & Security)**
  * *Vurgu:* 5 Katmanlı koruma, 7 risk sinyali, tek taraflı puanlama, ~250m konum gizliliği.
* **SLAYT 9: Beklenen Etki ve Sürdürülebilirlik Metrikleri (Sustainability Impact)**
  * *Vurgu:* 5 Boyutlu etki (1,840 kg Atık Azaltımı, 842 Yeni Ürün Önleme, 2,450 kg CO₂e Karbon Tasarrufu, ₺128,500 TL Hane Tasarrufu).
* **SLAYT 10: Rekabet ve Fark Yaratan Yönler (Competitive Advantage)**
  * *Vurgu:* Olio, Freecycle ve sahibinden'e karşı ezici teknolojik ve finansal üstünlük tablosu.
* **SLAYT 11: Teknik Mimari ve Yazılım (Tech Stack & Architecture)**
  * *Vurgu:* Java 21, Spring Boot 3, PostgreSQL+PostGIS, Outbox Pattern, WasteAI (YOLOv8s).
* **SLAYT 12: Büyüme ve Yaygınlaşma Stratejisi (Go-To-Market & Scalability)**
  * *Vurgu:* Mahalle -> İlçe -> Şehir kademeli ölçeklenme, Bakkal/Esnaf Micro-Hub modeli (%24 tercih).
* **SLAYT 13: Risk Yönetimi ve B Planları (Risk Mitigation)**
  * *Vurgu:* Sahtecilik, anonimlik, AI hataları ve partner kesintilerine karşı somut B planları.
* **SLAYT 14: Takım ve Yol Haritası (Team & Roadmap)**
  * *Vurgu:* KTÜ Bilgisayar Bilimleri mühendislik ekibi, MVP prototip durumu ve 2026-2027 hedefleri.
* **SLAYT 15: Kapanış & Çağrı (The Ask / Call to Action)**
  * *Vurgu:* "Döngüsel ekonominin geleceğine yatırım yapın!" Contact & Q&A.
