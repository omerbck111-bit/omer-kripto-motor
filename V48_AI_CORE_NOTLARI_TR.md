# Ömer Kripto Motoru V48 — AI Core

Bu sürüm ilk AI çekirdek düzeltmesidir.

## Değişiklikler
- Ömer AI için daha güçlü kimlik ve davranış sistemi eklendi.
- Kendi kimliği, uygulama içi durum ve yerel bağlam sorularında web araması engellendi.
- Web yalnızca güncel/dış dünya bilgisi açıkça gerektiğinde devreye giriyor.
- Yerel model veya gateway başarısız olduğunda sohbet artık otomatik olarak internet arama sonuçlarına düşmüyor.
- Yerel model yoksa kullanıcıya model kurulumunun gerekli olduğu açıkça bildiriliyor.
- Qwen/llama.cpp system prompt'u doğal Türkçe sohbet ve uygulama bağlamı için güçlendirildi.
- Java ve Kotlin JVM hedefleri 17 olarak hizalandı.
- AndroidX etkinleştirildi.
- WebView arka plan rengi Kotlin tip hatası düzeltildi.
- Uygulama sürümü 48.0.0 / versionCode 48.
- Launcher ikonu eklendi.
- GitHub Actions workflow Node 24 uyumlu action sürümlerine ve ubuntu-24.04'e taşındı.

## Önemli
Yerel doğal sohbet için GGUF modelinin cihazda bulunması gerekir. Uygulamadaki Ömer AI / Modeli İndir bölümünden önerilen Qwen3-4B-Q4_K_M modeli kurulmalıdır.
