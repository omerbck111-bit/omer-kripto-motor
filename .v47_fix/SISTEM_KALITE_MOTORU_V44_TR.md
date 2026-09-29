# Ömer Kripto Motoru V44 — Arka Plan Kalite Motoru

## Amaç
V43 Sistem Doğrulama Laboratuvarı'nı yalnızca tek seferlik test çalıştıran bir yapı olmaktan çıkarıp, uygulama yaşam döngüsüne bağlı sürekli kalite katmanına dönüştürür.

## Katmanlar
1. **V43 deterministik laboratuvarı:** sözleşmeler, matematik, Türkçe, transaction, orchestrator, persistence ve güvenlik.
2. **V44 property/fuzz katmanı:** rastgele ama tekrarlanabilir girdilerle matematik, Türkçe yönlendirme, transaction ve concurrency özellikleri.
3. **Model tabanlı regresyon:** küçük işlem dizileri ve durum geçişleri ile geri alma/snapshot davranışı.
4. **Watchdog:** uygulama görünürken başlangıçta, resume/online olaylarında ve periyodik aralıklarla hafifletilmiş kalite taraması.
5. **Android arka plan kapısı:** WorkManager ile WebView çalıştırmadan paketlenmiş HTML bütünlüğü + ağ erişimi kontrol edilir; tam JS laboratuvarı uygulama tekrar öne geldiğinde çalıştırılır.
6. **AI bağlamı:** kalite raporu AI'ya salt-okunur bağlam olarak verilir. AI test başarısızlığını açıklayabilir; laboratuvar sonucu üzerinden kendi kendine üretim değişikliği yapamaz.

## Neden WorkManager?
Android'in arka plan çalışma kısıtları nedeniyle sürekli servis yerine WorkManager kullanılır. Ağ gerektiren periyodik kalite kapısı için uygun ve sistem tarafından yönetilen yöntemdir.

## Property testleri
Harici bir npm bağımlılığı zorunlu tutulmadı. Küçük deterministik PRNG ile cihaz içinde düşük maliyetli property/fuzz kontrolleri uygulanır. İleride build/test ortamı kurulunca fast-check CI tarafına eklenebilir.

## Güvenlik
- `eval` / `new Function` yok.
- Test motoru üretim durumunu sessizce değiştirmez.
- Destructive işlemler transaction/snapshot gerektirir.
- Arka plan işçisi JavaScript/WebView çalıştırmaz.
- Piyasa verisi veya gerçek işlem emri kalite testi için kullanılmaz.

## Genişletme sözleşmesi
Yeni modül ekleyen geliştirici:
- `OKM_SYSTEM_LAB.registerTest(...)` ile birim/entegrasyon testi,
- `registerSuite(...)` ile test paketi,
- gerektiğinde V44 `propertyTests` benzeri invariant testleri,
- UI/uygulama katmanı için senaryo,
- başarısızlık durumunda güvenli rollback yolu
sağlamalıdır.

## Test katmanları
Android tarafında unit → component → feature → application → release-candidate ayrımı korunacak şekilde tasarlanmıştır. UI/E2E gerektiğinde gerçek cihaz/emülatör testleri ayrı çalıştırılmalıdır; WebView içindeki V44 laboratuvarı bunun yerine geçmez.
