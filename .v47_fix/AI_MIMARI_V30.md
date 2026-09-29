# Ömer Kripto Motoru V30 — AI mimarisi

## 1. Katmanlar

1. **Piyasa veri katmanı:** REST/WebSocket, veri yaşı, bağlantı sağlığı.
2. **Deterministik analiz katmanı:** teknik göstergeler, Grid/Spot/Fırsat/Tahmin, backtest.
3. **Araç katmanı:** AI'nın hesaplamayı kendisi uydurması yerine uygulamanın gerçek fonksiyonlarını çağırması.
4. **Hafıza katmanı:** tahmin, Grid, Spot, fırsat, portföy ve sistem olaylarının sonuçları.
5. **Öğrenme katmanı:** özellik → sonuç ilişkisi, walk-forward doğrulaması ve hata analizi.
6. **Yerel LLM katmanı:** GGUF + native Android runtime. V30, gerçek runtime'a JNI sınırı koyar; native `.so` yoksa bunu açıkça bildirir.
7. **Öz-denetim katmanı:** veri tazeliği, örnek sayısı, model/runtime, borsa kapsamı, maliyet/slippage ve backtest güvenilirliği gibi eksikleri otomatik çıkarır.
8. **İyileştirme kuyruğu:** AI yalnızca öneri üretir; üretim kodu/risk limiti/emir yetkisi kendiliğinden değişmez. Öneri önce test/backtest/walk-forward/risk kontrolünden geçmelidir.

## 2. Yerel model

Önerilen başlangıç modeli `Qwen/Qwen3-4B-GGUF:Q4_K_M` olup yaklaşık 2.5 GB'dır. Model Apache-2.0 lisanslıdır. V30, modeli Android uygulamasının uygulama-özel model dizinine indirebilir veya Android dosya seçiciden GGUF içeri aktarabilir.

Gerçek inference için `libomer_ai.so` native katmanı gerekir. Bu katman llama.cpp Android binding'i ile bağlanacak şekilde hazırlanmıştır. Native katman eklenmediği sürece uygulama yerel model varmış gibi davranmaz.

## 3. AI'nın otomatik bulacağı geliştirme alanları

- Yerel runtime yok / model yok
- REST/WebSocket veri tazeliği düşük
- Geçmiş örnek sayısı yetersiz
- Sonuçlarda sınıf dengesizliği
- Walk-forward doğrulaması eksik
- Borsa karşılaştırmasında order-book derinliği eksik
- Komisyon/slippage/transfer maliyeti modeli eksik
- Sinyal ile gerçekleşen sonuç arasında kalibrasyon farkı
- Hafıza büyüklüğü / depolama temizleme ihtiyacı
- Cihaz RAM/disk/batarya sınırı nedeniyle model bağlamının fazla olması
- AI çıktısının deterministik hesapla doğrulanmaması

## 4. Güvenlik sınırı

AI gerçek emir gönderemez, risk limitini sessizce değiştiremez ve kendi kodunu doğrudan üretime yazamaz. AI'nın görevi analiz, araç çağrısı, teşhis ve test edilebilir iyileştirme önerisidir.
