# Ömer Kripto Motoru V26 — Sade + Sessiz Bakım

Bu sürüm kullanıcı arayüzü ile motor/bakım katmanını ayırır.

## Yerel AI neden artık kurulum engeli değil?
Önceki sürümde HTML, `/api/local-ai/status` ve `/api/local-ai/download` adlı harici bir yardımcı servis bekliyordu. APK'nın içinde bu servis ve GGUF çalıştıran native runtime yoktu. Bu yüzden "Yerel model hazır değil" uyarısı sürekli kalabiliyordu.

V26'da büyük yerel model zorunlu değildir. Temel AI, piyasa hesapları, geçmiş hafıza ve AI bağlamı model yokken de çalışır. Uyumlu native runtime + GGUF modeli ayrıca mevcutsa AI bunu öncelikli kullanabilir.

Gerçek GGUF yerel çalıştırma için Android tarafında llama.cpp/benzeri native runtime gerekir; yalnızca GGUF dosyasını WebView depolamasına indirmek yeterli değildir.

## Arayüz
- Tek Dokunuş İşlemleri kaldırıldı.
- Teknik önemli notlar kullanıcı ekranından kaldırıldı.
- Canlı piyasa tablosu ana ekrandan kaldırıldı; motor arka planda kullanmaya devam eder.
- Fırsat rejimi/metrik ayrıntıları arka plana alındı.
- Sistem sağlık günlükleri ve otonom bakım kullanıcı ekranından gizlendi.
- AI durum rozeti gereksiz kalabalık oluşturmayacak şekilde gizlendi.

## Sessiz bakım
Android tarafında WorkManager ile en az 15 dakikalık periyotta ağ sağlık kontrolü planlanır. Foreground'da NetworkCallback + VALIDATED takibi devam eder. Bu kontrol emir göndermez ve piyasa taraması yapmaz; sadece bağlantı sağlığını ölçer.

WebView tarafında ayrıca veri yaşını, yerel hafızayı ve AI bağlamını sessizce denetleyen supervisor bulunur.

## AI öz-denetim
AI bağlamı; canlı piyasa, geçmiş hafıza ve sistem durumuyla hazırlanır. V26 ayrıca sessiz bir AI self-audit kaydı üretir. Bu kayıt otomatik strateji/emir değişikliği yapmaz; güvenlik için AI'nın doğrudan kodu veya işlem kurallarını kendi kendine değiştirmesine izin verilmez.


## V27
- Borsa karşılaştırması artık `/api/market/cross-exchange` gateway'ine bağımlı değil; Binance, Coinbase Exchange, Kraken ve OKX public ticker verilerini doğrudan toplar.
- Son fiyat, bid/ask, spread, gecikme ve ham borsa fiyat ayrışması gösterilir; ücret/slippage/order-book derinliği bilinmeden arbitraj kârı varsayılmaz.
- Son karşılaştırma snapshot'ı AI bağlamına aktarılır.
- AI sohbeti mobil-first, tek sütun, klavye güvenli ve mesaj baloncuklu hale getirildi.


## V28 değişiklikleri
- Tek Dokunuş İşlemleri ayrı bir kurulum panelinden çıkarıldı; Kurulum/Tahmin/Grid/Spot/Borsa/Analitik/Öğrenme/Sistem/Başarı/Ayarlar tek **Kontrol Merkezi** altında toplandı.
- Ana ekrana kompakt canlı piyasa şeridi ve mini SVG grafikler eklendi; ayrıntılar alt ekranlarda tutuluyor.
- Android `SwipeRefreshLayout` kaldırıldı. WebView artık doğrudan tam alan kaydırma yüzeyidir; manuel yenileme uygulama içinden yapılır.
- WebView sistem çentikleri/çubukları için WindowInsets uygulanır; dikey dokunma ve iç yatay listeler birbirine karışmaz.
- Borsa karşılaştırması için düşük satış/yüksek alış, spread, gecikme, brüt fark ve örnek komisyon sonrası fark özetleri eklendi.
- Tasarım dili güncel mobil borsa uygulamalarındaki kompakt üst özet + hızlı erişim + alt navigasyon yaklaşımından esinlenir; birebir marka kopyası değildir.
