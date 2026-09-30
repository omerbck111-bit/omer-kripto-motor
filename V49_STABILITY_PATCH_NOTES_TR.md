# Ömer Kripto Motoru V49 — Stabilite / AI / İndirme Düzeltmeleri

## Bu sürümde düzeltilenler

1. **Ömer AI sohbetinin kilitlenmesi**
   - Eski yapı `AndroidLocalAI.generate()` çağrısını WebView/UI tarafında senkron çalıştırıyordu.
   - Qwen3 4B modeli ilk yüklenirken UI thread'i bloke olabiliyordu.
   - V49'da yerel inference ayrı tekil arka plan iş parçacığına taşındı.
   - WebView'a sonuç asenkron callback ile dönüyor.
   - Sohbet ekranı artık model yüklenirken donmamalı.

2. **Modelin her soruda tekrar yüklenmesi**
   - Yerel model bellekte kontrollü süreyle önbelleğe alınıyor.
   - Aynı sohbet oturumunda her mesajda 2,5 GB civarı modeli yeniden açma maliyeti azaltıldı.
   - CPU thread sayısı 2–4 aralığına, context 2048'e, üretim token sınırı 1024'e düşürüldü; amaç telefon ısısını ve bekleme süresini azaltmak.

3. **Model indirme durumu**
   - İndirme başlar başlamaz "indirildi" gibi yanlış durum gösterilmesi engellendi.
   - Android DownloadManager'ın gerçek durumu okunuyor.
   - Uygulamada yüzde, indirilen/toplam GB ve durum gösteriliyor.
   - Devam eden indirme varsa ikinci kez yeni indirme başlatılmıyor.
   - Model zaten doğrulanmışsa tekrar indirme yapılmıyor.
   - İndirme sonrası SHA-256 doğrulaması tek seferlik tamamlanıyor; her saniye tekrar hash hesaplanmıyor.

4. **Beş ana sekme sadeleştirildi**
   - Ana
   - Fırsatlar (Spot + Grid/Bot)
   - Piyasa
   - Ömer AI
   - Daha
   - Portföy, Tahmin, Grid/Bot, Spot, Analitik, Öğrenme, Başarı ve Ayarlar "Daha" menüsünde kalıyor.

5. **Fırsatlar doğrudan erişilebilir**
   - Spot ve Grid/Bot fırsatları artık ana navigasyondan tek dokunuşla açılabiliyor.
   - Mevcut fırsat motoru korunuyor; yeni sekme mevcut sonuçları gösteriyor.

## Güvenlik / tasarım ilkeleri

- Gerçek emir gönderme yok.
- AI kendi başına risk ayarı veya üretim kodu değiştirmiyor.
- Mevcut 5 butonlu yapı korunuyor ve yalnızca daha anlaşılır hale getiriliyor.
- Ağ/teknik teşhis ayrıntıları ana kullanıcı ekranına taşınmıyor.
