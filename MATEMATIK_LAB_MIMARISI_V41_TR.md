# Matematik Lab V41

Akış: `soru → matematik sınıflandırma → AST → sembolik çözüm → nümerik çözüm → bağımsız doğrulama → hata/uyumsuzluk analizi → doğal açıklama`.

## Neden iki doğrulama?
LLM'nin ürettiği matematiksel ifadeyi doğrudan doğru kabul etmek yerine deterministik motor hesaplar. Sembolik türev ayrıca merkez farkla örneklenir. Sonuç uyuşmazsa AI kesinlik dili kullanmaz.

## Kuantum yolu
V41 küçük matris/state-vector düzeyinde tensor product, Bell state, density matrix ve Pauli operatörlerini sağlar. Sonraki aşamada Bloch küresi, genel üniter kapılar, ölçüm kanalları, QFT/Grover ve küçük Shor simülasyonları bu arayüz üzerinden eklenebilir.

## İspat yolu
`OKM_PROOF_ENGINE` bir adapter sözleşmesidir. Mobil uygulamada Lean binary gömmek yerine ileride ayrı bir doğrulama servisi/yerel native backend bağlanabilir. Böylece UI ve AI değişmeden ispat backendi değiştirilebilir.
