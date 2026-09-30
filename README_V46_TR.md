# Ömer Kripto Motoru V46

V46 odağı: gerçek Android ağ yolu + çoklu endpoint + sayfa yenileme/recovery + AI web araştırma fallback + arka plan görünürlük temizliği.

## Temel prensip
Bir modül değiştiğinde sistem laboratuvarı, kalite laboratuvarı ve ağ dayanıklılığı birlikte değerlendirilir.

## AI
Yerel GGUF hazırsa önceliklidir. Hazır değilse kontrollü Android web araması ve uygulama araçları kullanılır; genel “yeterli veri yok” cevabı son çareye indirgenmiştir.

## Ağ
Android NetworkCallback + VALIDATED + native HTTP GET + Binance çoklu host + cache/recovery + WorkManager watchdog. Android tarafında ağ değişimlerini callback ile izlemek ve arka plan işlerini WorkManager kısıtlarıyla çalıştırmak önerilen yaklaşımdır.

## UI
Network/AI watchdog, engineering, gateway ve teknik paneller background-only olarak zorlanır; kullanıcı ekranı yalnızca gerekli ürün akışlarını gösterir.
