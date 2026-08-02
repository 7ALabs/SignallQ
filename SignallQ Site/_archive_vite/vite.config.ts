/// <reference types="vitest/config" />
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      // Registro manual via useRegisterSW em src/components/PwaUpdatePrompt.tsx —
      // sem isso o plugin injetaria um segundo script de registro no index.html.
      injectRegister: false,
      // public/manifest.json já é a fonte de verdade (link manual em index.html);
      // não deixar o plugin gerar um manifest.webmanifest paralelo.
      manifest: false,
      devOptions: { enabled: false },
      includeAssets: [
        'icons/favicon-16.png',
        'icons/favicon-32.png',
        'icons/apple-touch-icon.png',
        'icons/icon-192.png',
        'icons/icon-512.png',
        'icons/icon-192-maskable.png',
        'icons/icon-512-maskable.png',
      ],
      workbox: {
        skipWaiting: true,
        clientsClaim: true,
        cleanupOutdatedCaches: true,
        navigateFallback: '/index.html',
        // Técnico Virtual (Leste Telecom, repo signallq-agent) é publicado sob /leste neste mesmo
        // projeto Pages. Sem esse denylist, o navigateFallback do service worker intercepta
        // qualquer navegação em /leste/* e serve o index.html cacheado DESTE site — o React Router
        // daqui não conhece a rota e mostra "página não encontrada" antes mesmo de bater na rede
        // (bug real observado em produção, 2026-07-22).
        // Cobre tanto /leste (sem barra, antes do 308 do Cloudflare) quanto /leste/... —
        // sem o `$` sozinho, a rota bare "/leste" caía no fallback antes mesmo do redirect
        // de borda acontecer (bug reobservado em produção, 2026-07-22).
        navigateFallbackDenylist: [/^\/leste$/, /^\/leste\//],
        globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],
        // Regra não-negociável (issue #1184): o teste de velocidade real e a
        // telemetria nunca podem ser servidos pelo cache do service worker —
        // senão o teste passaria a medir a rede do cache, não a conexão real.
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/speed\.cloudflare\.com\/__down/,
            handler: 'NetworkOnly',
          },
          {
            urlPattern: /^https:\/\/speed\.cloudflare\.com\/__up/,
            handler: 'NetworkOnly',
          },
          {
            // Same-origin: precisa casar por pathname, não por regex ancorado em
            // "/" — a URL testada pelo Workbox é o href absoluto
            // ("https://.../api/track"), então `/^\/api\//` nunca bateria.
            urlPattern: ({ url }) => url.pathname.startsWith('/api/track') || url.pathname.startsWith('/api/waitlist'),
            handler: 'NetworkOnly',
          },
          {
            // Defesa extra: o navegador nunca chama o admin-worker direto (só via
            // /api/track proxy server-side), mas o padrão fica registrado aqui
            // também para o caso de alguma chamada futura vir a expor a URL.
            urlPattern: /^https:\/\/signallq-admin[^/]*\.workers\.dev\//,
            handler: 'NetworkOnly',
          },
          {
            // Páginas institucionais (quem-somos, pro, privacidade, termos) e o
            // shell JS/CSS já vão para o precache (globPatterns acima); só as
            // fontes do Google (carregadas via <link> cross-origin, fora do
            // pipeline de build do Vite) precisam de runtime caching explícito.
            urlPattern: /^https:\/\/fonts\.(googleapis|gstatic)\.com\//,
            handler: 'StaleWhileRevalidate',
            options: { cacheName: 'google-fonts' },
          },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
})
