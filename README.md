# 🌿 YENİDEN Backend - Akıllı Sıfır Atık & Döngüsel Ekonomi Platformu

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-green.svg)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-10%20Containers-blue.svg)
![Build & Tests](https://img.shields.io/badge/Unit%20Tests-100%25%20PASSED-brightgreen.svg)

**YENİDEN**, şehir insanlarının evlerindeki atık veya kullanmadıkları eşyaları mahalle ölçeğinde paylaşmalarını, geri dönüşüme kazandırmalarını ve dijital ödüller (EcoCoin, XP, Rozetler) kazanmalarını sağlayan çoklu mikroservis (Multi-Microservice) tabanlı backend sistemidir.

---

## ⚡ Hızlı Başlangıç (Quick Start)

Tüm altyapıyı (PostgreSQL PostGIS, Redis 7, RabbitMQ 3 ve 7 Mikroservis) Docker Compose ile çalıştırmak için Java 21, Docker/Compose ve Docker VM için en az 4 GB RAM kullanın:

```sh
# 1. Dockerfile'ların kopyalayacağı çalıştırılabilir JAR'ları üretin
./mvnw -DskipTests package

# 2. Her yerel çalışma için güçlü, geçici bir OTP HMAC anahtarı üretin
export OTP_HMAC_SECRET="$(openssl rand -hex 32)"

# 3. Konteynırları derleyin ve arka planda başlatın
docker compose up --build -d

# 4. Konteynır durumlarını kontrol edin
docker compose ps
```

`OTP_HMAC_SECRET` için depoda varsayılan değer bulunmaz. Üretim ortamında ayrıca `prod` profili, HTTPS issuer ve kalıcı RSA anahtar çifti sağlanmalıdır; `local` profili yalnızca Compose geliştirme ortamı içindir.

Birim (Unit) testlerini ve Maven projesini derlemek için:

```sh
# Tüm mikroservis testlerini çalıştırın
./mvnw test
```

---

## 🔌 Mikroservisler ve Port Listesi

Tüm dış HTTP istekleri **API Gateway (Port 8080)** üzerinden ilgili mikroservise yönlendirilir:

| Servis Adı | Port | Açıklama |
| :--- | :---: | :--- |
| 🚪 **`api-gateway`** | **8080** | Tüm istemci isteklerinin karşılandığı tek giriş noktası (Reverse Proxy). |
| 👤 **`identity-service`** | **8081** | Kullanıcı kaydı/giriş, Redis OTP doğrulama, Güven Puanı (Trust Score). |
| 📦 **`catalog-service`** | **8082** | Atık/eşya ilanları, kategoriler, PostGIS yakınlık sorguları. |
| 🤝 **`exchange-service`** | **8083** | İlan talepleri, 6 haneli teslimat kod üretimi/doğrulanması, RabbitMQ Event yayıncısı. |
| 💰 **`ecocoin-service`** | **8084** | Çift Kayıtlı Defter (Double-Entry Ledger) cüzdan mimarisi & günlük 100 coin tavanı. |
| 🏆 **`gamification-service`**| **8085** | XP/Puan takibi, Seviye atlama (`(XP/100)+1`), Rozet kazanımı. |
| 🤖 **`wasteai-service`** | **8086** | Yapay Zeka destekli atık/eşya görüntü sınıflandırma (Stub/Adapter). |

### 🗄️ Altyapı Konteynırları
- **PostgreSQL 16 + PostGIS:** `localhost:5432` (`yeniden_db` veritabanı, 5 yalıtılmış şema)
- **Redis 7 Alpine:** `localhost:6379` (OTP doğrulama & TTL önbelleği)
- **RabbitMQ 3 Management:** `localhost:5672` (AMQP Broker) / `localhost:15672` (Yönetim Paneli)

---

## 🏗️ Temel Mimari İkeler

1. **Şema İzolasyonu (Schema Isolation):** Her mikroservis PostgreSQL üzerinde kendine ait bir şemaya sahiptir (`identity`, `catalog`, `exchange`, `ecocoin`, `gamification`).
2. **Olay Güdümlü Mesajlaşma (Event-Driven AMQP):** Fiziksel teslimat onaylandığında `exchange-service` RabbitMQ'ya `HandoverConfirmedEvent` fırlatır; `ecocoin-service` bu olayı asenkron işleyerek kullanıcının cüzdanına 25 EcoCoin aktarır.
3. **Çift Kayıtlı Defter (Double-Entry Ledger):** Bakiye doğrudan güncellenmez; Borç (-) ve Alacak (+) kayıtları üzerinden `Net Sum = 0` garantisiyle tutulur.
4. **SOLID & Unit Test:** Tüm servis kodları SOLID prensiplerine uyar ve JUnit 5 + Mockito testleri ile %100 kapsanır.

---

## 📚 Detaylı Dokümantasyon

Daha ayrıntılı teknik bilgiler ve geliştirme kılavuzları için `docs/` klasörünü inceleyebilirsiniz:

- 🚀 [**Geliştirici & Takım Kılavuzu (PROJECT_GUIDE.md)**](docs/PROJECT_GUIDE.md)
- 🗄️ [**Veritabanı Mimarisi & ER Diyagramı (DATABASE_ARCHITECTURE.md)**](docs/DATABASE_ARCHITECTURE.md)
- 🗺️ [**Mimari Kararlar & Doküman Haritası (00-overview.md)**](docs/00-overview.md)
