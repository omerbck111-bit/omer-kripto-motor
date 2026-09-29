# Sistem Doğrulama Laboratuvarı V43

## Test katmanları

### 1. Preflight
- Ortak modül sözleşmesi
- localStorage JSON bütünlüğü
- duplicate ID
- dinamik kod çalıştırma izi

### 2. AI
- Türkçe anlamlandırma
- görev sınıflandırma
- AI orchestrator yönlendirmesi

### 3. Math
- ifade değerlendirme
- sembolik türev
- bağımsız türev doğrulaması
- integral
- kök bulma
- matris
- tensor product
- Bell state
- limit
- denklem çözümü
- binom olasılığı

### 4. State
- transaction snapshot
- geri yükleme davranışı
- kalıcı veri bütünlüğü

### 5. Core
Tüm kayıtlı testleri tek pakette çalıştırır.

### 6. End-to-end senaryolar
- UI → AI → Math
- UI → AI → Market
- Delete → Rollback
- AI → System Health
- Turkish → Code Intent

## AI ile ilişki

AI laboratuvar raporunu yorumlayabilir. AI'ya verilen rapor `readOnly=true`, `autoModify=false`, `rollbackRequiredForChanges=true` politikası taşır. Böylece AI sistemi açıklayabilir ve hata için yol gösterebilir ancak test sonucunu bahane ederek sessiz üretim değişikliği yapamaz.

## Genişletilebilirlik

`OKM_SYSTEM_LAB.registerTest(name, fn, meta)` ile yeni test eklenebilir.

`OKM_SYSTEM_LAB.registerSuite(name, cases, meta)` ile yeni test paketi eklenebilir.

Bu sayede V44+ sürümlerinde yeni piyasa motorları, yeni AI sağlayıcıları, yeni matematik yetenekleri veya yeni Android köprüleri eklendiğinde laboratuvarın çekirdeğini değiştirmeden testler eklenebilir.
