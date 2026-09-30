// Ömer Kripto Motoru — Service Worker
// Amaç: uygulama kabuğunu (HTML/manifest/ikonlar) önbelleğe alarak
// - sayfa yenilendiğinde "sayfa bulunamadı" / boş ekran görülmesini engellemek
// - kısa süreli internet kopmalarında uygulamanın en azından açılabilmesini sağlamak
// ÖNEMLİ: Binance ve diğer borsa/AI API çağrıları BİLEREK önbelleğe alınmaz;
// bu istekler her zaman doğrudan ağa gider, böylece fiyatlar asla bayat önbellekten gelmez.

const CACHE_VERSION = 'okm-shell-v2';
const SHELL_FILES = [
  './OMER_KRIPTO_MOTORU.html',
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/icon-maskable-512.png',
  './icons/apple-touch-icon.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_VERSION)
      .then((cache) => cache.addAll(SHELL_FILES))
      .catch(() => {}) // dosyalardan biri eksik olsa bile kurulum başarısız sayılmasın
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_VERSION).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

function isAppShellRequest(url) {
  // Sadece bu origin'deki (aynı klasördeki) dosyalar önbelleklenir.
  return url.origin === self.location.origin;
}

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return; // POST/PUT gibi istekler dokunulmadan geçer

  let url;
  try { url = new URL(req.url); } catch { return; }

  // Borsa / dış API / websocket / kendi backend'imiz (varsa) hiçbir zaman SW üzerinden önbelleklenmez.
  if (!isAppShellRequest(url)) return;

  // Uygulama kabuğu: ağ öncelikli, ağ başarısız olursa önbellekten karşıla.
  // Böylece normalde en güncel sürüm gösterilir, kopukluk anında da uygulama açılır.
  event.respondWith(
    fetch(req)
      .then((res) => {
        const copy = res.clone();
        caches.open(CACHE_VERSION).then((cache) => cache.put(req, copy)).catch(() => {});
        return res;
      })
      .catch(() => caches.match(req).then((cached) => cached || caches.match('./OMER_KRIPTO_MOTORU.html')))
  );
});
