# Exchange Mobil Entegrasyon Planı

## Hedef akış

1. Alıcı yayınlanmış ilana talep gönderir (`PENDING`).
2. İlan sahibi gelen talebi görür ve kabul eder.
3. Exchange bir handover oluşturur (`PENDING_CODE`, 72 saat geçerli).
4. Alıcı 6 haneli teslimat kodunu görüntüler.
5. İlan sahibi kodu girer ve teslimatı onaylar (`CONFIRMED`).
6. Outbox eventi EcoCoin ve Gamification servislerini günceller; mobil ilan ve profil verisini yeniler.

## Mobil iş paketleri

- API/types: request, accept, code ve confirm DTO/status sözleşmeleri.
- State: işlem yükleme, talep gönderme/kabul, kod alma, kod doğrulama, hata ve loading durumları.
- UI: aktif/geçmiş işlemler, ilan sahibine kabul aksiyonu, alıcıya kod gösterimi, sağlayıcıya 6 haneli kod formu.
- Senkronizasyon: başarılı kabul sonrası katalog; başarılı teslimat sonrası katalog, EcoCoin ve Gamification yenileme.
- Doğrulama: TypeScript, ESLint ve Jest; iki kullanıcı/iki cihaz senaryosu için backend kontrat testi.

## Mevcut backend ile tamamlanabilenler

- Talep oluşturma ve bir ilanın gelen taleplerini listeleme.
- Talebi ilan sahibi olarak kabul edip handover oluşturma.
- Handover kimliği biliniyorsa alıcının kodu görmesi.
- Handover kimliği ve kod biliniyorsa sağlayıcının teslimatı onaylaması.

## Tam üretim akışı için backend gereksinimleri

- Kullanıcı bazlı gelen/giden talepleri ve handover kayıtlarını listeleyen endpoint.
- Request kimliğinden handover bulma endpoint'i; böylece farklı cihaz ve yeniden açılış desteklenir.
- Reject, cancel ve no-show endpoint'leri.
- Self/duplicate request ve tekrar accept için idempotency/409 kontrolleri.
- Accept sırasında katalog rezervasyonu; confirm sırasında ilanın teslim edildi/kapatıldı durumuna geçirilmesi.
- Kimlik doğrulanan kullanıcı ile body/query içindeki kullanıcı kimliklerinin sunucuda eşleştirilmesi.
- Exchange `BaseException` hatalarını 400/403/404/409 olarak döndüren ortak hata işleyici.

## Kabul kriterleri

- Demo transaction/chat verisi gösterilmez.
- Başarısız her istek kullanıcıya anlaşılır hata verir ve sahte başarı üretmez.
- Kod yalnız alıcıya gösterilir, yalnız sağlayıcı doğrular.
- Başarılı confirm sonrasında işlem geçmişe taşınır ve ödül verileri yenilenir.
- Uygulama yeniden açıldığında işlemler backend'den geri kurulabilir (yeni kullanıcı bazlı endpoint'lerden sonra).
