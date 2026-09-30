# Ömer Kripto Motoru V34 — Profesyonel Agent Mimarisi

V34, sohbet içindeki kodu doğrudan çalıştırmak yerine kontrollü bir ajan döngüsüne dönüştürür:

1. **Intent / kod algılama** — fenced veya inline kod, dil ve dosya ipuçları ayrıştırılır.
2. **Salt-okuma keşif** — uygulama haritası, kod sembol haritası, hedef arama, AI/piyasa/sağlık bağlamı.
3. **Plan** — V4 JSON planı; hangi araçların hangi sırayla kullanılacağı açıkça belirtilir.
4. **Tool Registry** — yalnızca beyaz liste araçları çalışabilir.
5. **Transaction** — yazma işlemi öncesi yedek alınır.
6. **Patch** — CSS/DOM/izinli ayar/izinli uygulama çağrıları dışında keyfi kod çalıştırılmaz.
7. **Verification** — her aracın sonucu kaydedilir; başarısız zorunlu adım işlemi durdurur.
8. **Rollback** — başarısız işlem önceki yedeğe döner.
9. **Journal / öğrenme** — plan, araç sonucu, hata ve geri alma olayları yerel geçmişe yazılır.

## Araçlar

### Read-only
- `app_map`
- `code_map`
- `code_search`
- `ui_snapshot`
- `ai_context`
- `market_snapshot`
- `exchange_snapshot`
- `strategy_history`
- `system_health`

### Write
- `patch_css`
- `set_text`
- `set_class`
- `set_attr`
- `set_setting`
- `call_tool`
- `rollback`

Her write aracı mevcut V2 güvenlik/backup katmanından geçer.

## Yerel LLM

Android tarafında `LocalAiEngine` iki üretim yüzeyi sunar:
- normal metin üretimi
- `generateStructured()` ile ajan planı gibi JSON çıktısı

Gerçek native runtime bulunmadığında uygulama bunu aktifmiş gibi göstermez. Native runtime bağlandığında structured üretim, llama.cpp'nin JSON/tool-call/grammar yaklaşımıyla aynı sözleşmeye bağlanabilir.

## Neden repo-map / code-map?

Büyük kod tabanını modele komple vermek yerine önce sembol ve bağımlılık haritası çıkarılır, sonra yalnızca ilgili parçalar istenir. Bu yaklaşım Aider'ın repository map yaklaşımıyla uyumludur.

## Güvenlik sınırı

Ajan şunları yapamaz:
- `eval`, `Function` veya keyfi JavaScript çalıştırmak
- keyfi ağ isteği açmak
- dosya/veri silmek
- API anahtarı istemek/okumak
- gerçek emir göndermek
- kullanıcı istemeden risk limitlerini değiştirmek
- bilinmeyen fonksiyon çağırmak

Bu mimari bir **coding agent** katmanıdır; APK'nın kendi Kotlin/NDK kaynaklarını canlı olarak değiştirdiğini iddia etmez. Kaynak seviyesinde değişiklik için yeni build gerekir.
