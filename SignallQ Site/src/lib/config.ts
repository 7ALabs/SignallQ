// Configuração central do site público SignallQ.
// Nenhum valor sensível é exposto aqui além de identificadores públicos
// (publisher id do AdSense) — nada de segredos (INGEST_KEY nunca entra aqui,
// vive só no Pages Function server-side, ver functions/api/track.ts).
//
// `process.env.NEXT_PUBLIC_*` permite configurar por ambiente sem editar código
// (ex.: variável de ambiente do projeto na Vercel) — sem override, os defaults
// abaixo mandam o site mostrar estados claros de "ainda não configurado" em vez
// de link quebrado ou anúncio vazio.

export const SIGNALLQ_BETA_DOWNLOAD_URL: string =
  process.env.NEXT_PUBLIC_SIGNALLQ_BETA_DOWNLOAD_URL ||
  'https://play.google.com/store/apps/details?id=io.signallq.app&hl=en-US&ah=CaFxCv25P6rZGNKL-Jy-IZbxwmw'

// Grupo oficial de testadores fechados. É um destino público, por isso pode
// constar no bundle; não contém convite individual nem credencial.
export const SIGNALLQ_TEST_GROUP_URL: string =
  process.env.NEXT_PUBLIC_SIGNALLQ_TEST_GROUP_URL || 'https://groups.google.com/g/testadores-signallq'

// Página de teste fechado (opt-in) da Play Store — só acessível a quem já
// entrou no grupo de testadores acima. Distinta da ficha pública do app
// (SIGNALLQ_BETA_DOWNLOAD_URL), que ainda não está publicada em produção.
export const SIGNALLQ_CLOSED_TESTING_URL: string =
  process.env.NEXT_PUBLIC_SIGNALLQ_CLOSED_TESTING_URL || 'https://play.google.com/apps/testing/io.signallq.app'

// Publisher ID do Google AdSense — gate de carregamento de `AdSenseScript`
// (só carrega o script real com a env var configurada E consentimento aceito,
// ver `src/lib/adConsent.ts`). Sem posição de anúncio decidida ainda (fase
// Fundação, reconstrução v4) — nenhuma tela reserva espaço visual de anúncio.
export const ADSENSE_PUBLISHER_ID: string = process.env.NEXT_PUBLIC_ADSENSE_PUBLISHER_ID || ''

// Motor de medição real. Isolado aqui para poder trocar por um endpoint
// próprio (ex.: o motor do app SignallQ hospedado na Cloudflare) sem tocar
// na interface — troque só estas duas constantes.
export const SPEEDTEST_DOWNLOAD_URL: string =
  process.env.NEXT_PUBLIC_SPEEDTEST_DOWNLOAD_URL || 'https://speed.cloudflare.com/__down'
export const SPEEDTEST_UPLOAD_URL: string =
  process.env.NEXT_PUBLIC_SPEEDTEST_UPLOAD_URL || 'https://speed.cloudflare.com/__up'
export const SPEEDTEST_SERVER_LABEL: string =
  process.env.NEXT_PUBLIC_SPEEDTEST_SERVER_LABEL || 'speed.cloudflare.com (rede Cloudflare)'
export const SPEEDTEST_LATENCY_URL: string =
  process.env.NEXT_PUBLIC_SPEEDTEST_LATENCY_URL ||
  'https://signallq-game-latency-probe.giammattey-luiz.workers.dev/probe'

// Proxy server-side de telemetria (Pages Function) — nunca chama o admin-worker
// direto do navegador (exigiria expor a INGEST_KEY no client).
export const TELEMETRY_ENDPOINT = '/api/track'

// Proxy server-side da lista de espera do SignallQ PRO — mesmo motivo do endpoint acima.
export const WAITLIST_ENDPOINT = '/api/waitlist'

