# V45 — İnternet Dayanıklılık Motoru

## Amaç
Ömer Kripto Motoru'nun interneti tek bir sağlayıcıya veya tek bir HTTP isteğine bağlı görmemesini sağlamak.

## Katmanlar
- Android `ConnectivityManager.NetworkCallback`: ağ değişimini anında izler.
- `NET_CAPABILITY_VALIDATED`: sistemin gerçek internet doğrulamasını kullanır.
- Çoklu endpoint probu: Binance, Coinbase, Kraken ve Cloudflare.
- Quorum: doğrulanmış Android ağı + en az bir başarılı endpoint veya en az iki başarılı endpoint.
- Endpoint başarısızlık sayacı ve gecikme ölçümü.
- Foreground recovery: online/resume/pageshow olaylarında yeniden doğrulama ve veri yenileme.
- Background WorkManager: 15 dakikalık dayanıklılık denetimi.
- AI teşhis bağlamı: ağ raporu + hata sınıfı + salt-okunur politika.
- Güvenli web okuma: yalnız HTTPS, allowlist, GET, boyut sınırı; API anahtarı ve yazma isteği yok.
- Cache/stale çalışma: bağlantı geçici bozulsa son başarılı uygulama verisi korunur.

## Bilinçli sınırlar
Uygulama Android'in Wi-Fi/mobil veri ayarlarını kullanıcı izni olmadan değiştirmez. Ağ sorununu tespit eder, yeniden doğrular, endpoint değiştirir, retry/backoff ve cache uygular; sistem ayarını zorla değiştirmez.

## AI rolü
AI ağ raporunu okuyabilir, teşhis koyabilir ve güvenli düzeltme planı önerebilir. Üretim kodunu ağ hatası bahanesiyle sessizce değiştiremez; değişiklikler mevcut transaction/rollback ve doğrulama zincirinden geçer.
