import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig, loadEnv} from 'vite';

// GH#416: build de producao nao pode sair com mocks habilitados nem sem API real
// configurada — variavel sobrescrita no dashboard do Pages ja causou isso antes.
function assertProductionConfig(mode: string, envDir: string) {
  if (mode !== 'production') return;

  const env = loadEnv(mode, envDir, '');
  const mocksEnabled = (env.VITE_ENABLE_MOCKS ?? '').toLowerCase() === 'true';
  const apiBaseUrl = env.VITE_ADMIN_API_BASE_URL ?? '';

  if (mocksEnabled) {
    throw new Error(
      '[build:production] VITE_ENABLE_MOCKS=true nao e permitido em build de producao (GH#416).'
    );
  }
  if (!apiBaseUrl.trim()) {
    throw new Error(
      '[build:production] VITE_ADMIN_API_BASE_URL vazio em build de producao (GH#416).'
    );
  }
}

// GH#443: o Console passou a ser publicado sob /console no mesmo projeto Cloudflare
// Pages do WebApp. VITE_BASE_PATH permite gerar o build com o prefixo correto sem
// alterar o dev local (que continua em '/').
const basePath = process.env.VITE_BASE_PATH || '/';

export default defineConfig(({mode}) => {
  assertProductionConfig(mode, process.cwd());

  return {
    base: basePath,
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    // SIG-12: transpila ?? e ?. para Safari < 13.1 (iPad 2012).
    // safari12 suporta const/arrow/classes mas não nullish coalescing (adicionado em 13.1).
    build: {
      target: ['safari12'],
    },
    esbuild: {
      target: ['safari12'],
    },
    server: {
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modifyâfile watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
