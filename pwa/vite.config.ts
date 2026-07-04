import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// GH#443: o WebApp e o Console SignallQ passaram a compartilhar o mesmo projeto
// Cloudflare Pages sob /app e /console. VITE_BASE_PATH permite que o mesmo build
// gere assets com o prefixo correto sem mudar nada no dev local (que continua em '/').
const basePath = process.env.VITE_BASE_PATH || '/';

export default defineConfig({
  base: basePath,
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': '/src',
      '@shared': '/shared',
    },
  },
  server: {
    hmr: process.env.DISABLE_HMR !== 'true',
    watch: process.env.DISABLE_HMR === 'true' ? null : {},
  },
});
