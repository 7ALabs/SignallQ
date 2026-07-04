const STATIC_CACHE = 'signallq-pwa-static-v2';
// GH#443: nomes relativos ao escopo do worker (self.registration.scope), nao mais
// caminhos absolutos a partir da raiz do dominio — o PWA passou a ser publicado
// sob /app no Cloudflare Pages, entao a raiz do dominio nao e mais a raiz do app.
const STATIC_ASSET_NAMES = ['', 'index.html', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png'];
const DYNAMIC_PATH_PREFIXES = ['/api/'];

function scopedUrl(name) {
  return new URL(name, self.registration.scope).toString();
}

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => cache.addAll(STATIC_ASSET_NAMES.map(scopedUrl))),
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== STATIC_CACHE).map((key) => caches.delete(key)))),
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (DYNAMIC_PATH_PREFIXES.some((prefix) => url.pathname.startsWith(prefix))) return;
  if (event.request.method !== 'GET') return;

  event.respondWith(
    caches.match(event.request).then((cached) => {
      return (
        cached ??
        fetch(event.request)
          .then((response) => {
            if (!response.ok || response.type === 'opaque') return response;
            const clone = response.clone();
            caches.open(STATIC_CACHE).then((cache) => cache.put(event.request, clone));
            return response;
          })
          .catch(() => caches.match(scopedUrl('index.html')))
      );
    }),
  );
});
