# Atik × Yeniden entegrasyon durumu

## Uygulanan ilk dilim

- Ortak gateway API istemcisi: `src/api/client.ts`
- `ApiResponse.data` zarfı ve RFC 7807/ProblemDetail hataları
- Bearer access token ve 401 sonrası tekil refresh
- Rotating refresh token’ın `react-native-keychain` ile güvenli saklanması
- Türkiye E.164 telefon normalizasyonu
- Gerçek Identity OTP request/verify/logout ve uygulama açılışında session restore
- Mevcut backend servisleri için typed API yüzeyi: catalog, exchange, ecocoin, gamification, wasteai

## P1 — Catalog server-state

- Home ve Map authenticated akışta Catalog API’den gerçek ilanları yüklüyor.
- Pull-to-refresh ve retry davranışı eklendi; API hatasında mock ilanlar sessizce gösterilmiyor.
- Mobil istemci gateway üzerinden çalışıyor (`10.0.2.2:8080` Android emulator varsayılanı); mikroservis portlarına doğrudan bağlanmıyor.
- Backend liste yanıtı artık `categoryCode`/`categoryName` döndürüyor ve mobil kategori filtresi bunu kullanıyor. Medya URL’si ve bölge adı backend sözleşmesine henüz dahil değil; mobil bu iki alanı nötr sunum değerleriyle gösteriyor.
- EcoCoin ledger geçmişi (`Page<CoinEntryDto>`) ve Gamification quest sözleşmeleri typed API olarak hazır.

## Commit `1d1e05e` — EcoCoin / Gamification mobil yüzeyi

- Profile wallet, level ve badges verisini canlı servislerden çekiyor.
- Aylık Gamification görevleri Profile ekranında gösteriliyor.
- EcoCoin ledger pagination sözleşmesi (`page`, `size`, `PageDto`) istemci katmanında hazır; Profile son 5 hareketi gösteriyor.

## Çalıştırma

`src/api/config.ts` içindeki gateway adresi Android emulator için `10.0.2.2:8080` olarak ayarlıdır.
Fiziksel telefonda bilgisayarın LAN IP adresi kullanılmalı veya `adb reverse tcp:8080 tcp:8080` çalıştırılmalıdır.
Gateway ve Identity aynı anda açık olmalıdır; mobil doğrudan mikroservis portlarına bağlanmamalıdır.

## Backend ile gerçek sözleşme

Mevcut backend yolları `/catalog`, `/exchange`, `/ecocoin`, `/gamification`, `/wasteai` prefix’lerini kullanır ve başarılı cevabı `data` altında döner. Mobil API katmanı bu gerçek sözleşmeye göre yazılmıştır.

## Sonraki tasklar

1. **Tamamlandı:** Home/Map ekranlarını `catalogApi` ile server state’e geçirmek; cursor/pagination backend’de henüz yok.
2. Post flow’u create → publish akışına bağlamak; photo upload endpoint’i backend’de henüz yok.
3. Requests/handover ekranını `exchangeApi` ile bağlamak; mine/reject/cancel/no-show endpoint’leri backend’de henüz yok.
4. **Kısmen tamamlandı:** Profile’da `/users/me`, wallet, seviye ve rozet verilerini birleştirmek. İşlem sayaçları ve sürdürülebilirlik metrikleri için backend sözleşmesi henüz yok.
5. Gateway’de Identity dışındaki servisler için JWT doğrulama ve servislerde principal tabanlı sahiplik kontrolü eklemek.
6. Dokümante edilen hedef API ile mevcut controller sözleşmesini uyumlu hale getirmek veya açık bir v2 sözleşmesi yayınlamak.
7. Android gerçek cihaz smoke testleri: OTP request, verify, refresh, logout, listings ve handover.

Eksik endpoint’ler istemci tarafından sessizce mock’lanmamalı; ilgili backend task’ı tamamlanana kadar ekranlarda açıkça “henüz kullanılamıyor” durumu gösterilmelidir.
