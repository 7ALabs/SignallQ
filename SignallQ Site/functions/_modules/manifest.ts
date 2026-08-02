export type TenantManifestDefinition = {
  id: string;
  nome: string;
  colorBrand: string;
  // Ícones próprios do tenant (192/512/maskable) — opcional; sem eles, cai no ícone genérico do
  // produto (ver GENERIC_ICONS abaixo). Caminhos relativos à raiz do site publicado.
  icons?: { icon192: string; icon512: string; maskable512: string };
};

// Espelha o registry hardcoded de tenants do frontend (src/features/tecnico-virtual/tenants.ts).
// Sem registry dinâmico ainda (D1) — ver issues #27/#34, fora de escopo aqui.
const TENANTS: Record<string, TenantManifestDefinition> = {
  topfibra: { id: 'topfibra', nome: 'Top Fibra', colorBrand: '#007545' },
  ajato: { id: 'ajato', nome: 'Ajato', colorBrand: '#0057B8' },
  leste: {
    id: 'leste',
    nome: 'Leste Telecom',
    colorBrand: '#009373',
    icons: {
      icon192: '/tenants/leste/icons/icon-192.png',
      icon512: '/tenants/leste/icons/icon-512.png',
      maskable512: '/tenants/leste/icons/icon-maskable-512.png',
    },
  },
};

export function findTenantManifestDefinition(tenantId: string): TenantManifestDefinition | null {
  return TENANTS[tenantId] ?? null;
}

// Mesma ordem de precedência do resolveTenant() do frontend (ver
// src/features/tecnico-virtual/tenants.ts e docs/adr/0001), adaptada ao servidor: o pedido de
// manifest chega sempre em /manifest.webmanifest (raiz), então o path do tenant não está na
// própria URL — vem do Referer (URL do documento HTML que solicitou o manifest).
// 1) ?tenant= explícito na URL do manifest
// 2) Referer: path do documento, depois subdomínio do Referer
// 3) Host da própria requisição (subdomínio)
// Nunca lança — tenant desconhecido cai para null e o caller decide o manifest genérico.
export function resolveTenantIdFromRequest(request: Request): string | null {
  const url = new URL(request.url);

  const queryTenant = url.searchParams.get('tenant');
  if (queryTenant && findTenantManifestDefinition(queryTenant)) return queryTenant;

  const referer = request.headers.get('referer');
  if (referer) {
    try {
      const refererUrl = new URL(referer);
      const pathSegment = refererUrl.pathname.split('/')[1] ?? '';
      if (pathSegment && findTenantManifestDefinition(pathSegment)) return pathSegment;

      const refererSubdomain = refererUrl.hostname.split('.')[0] ?? '';
      if (refererSubdomain && findTenantManifestDefinition(refererSubdomain)) return refererSubdomain;
    } catch {
      // Referer malformado — ignora e segue para os próximos critérios
    }
  }

  const hostSubdomain = url.hostname.split('.')[0] ?? '';
  if (hostSubdomain && findTenantManifestDefinition(hostSubdomain)) return hostSubdomain;

  return null;
}

// Ícone genérico do produto — usado quando o tenant não tem ícone próprio ainda (ver campo
// `icons` opcional em TenantManifestDefinition).
const GENERIC_ICONS = [
  { src: '/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any maskable' },
  { src: '/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' },
];

function iconsFor(tenant: TenantManifestDefinition): Array<Record<string, string>> {
  if (!tenant.icons) return GENERIC_ICONS;
  return [
    { src: tenant.icons.icon192, sizes: '192x192', type: 'image/png', purpose: 'any' },
    { src: tenant.icons.icon512, sizes: '512x512', type: 'image/png', purpose: 'any' },
    { src: tenant.icons.maskable512, sizes: '512x512', type: 'image/png', purpose: 'maskable' },
  ];
}

export function buildManifestResponseBody(tenantId: string | null): Record<string, unknown> {
  const tenant = tenantId ? findTenantManifestDefinition(tenantId) : null;

  if (!tenant) {
    return {
      name: 'SignallQ',
      short_name: 'SignallQ',
      description: 'Diagnóstico inteligente de conectividade no navegador.',
      start_url: '/',
      scope: '/',
      id: '/',
      display: 'standalone',
      orientation: 'portrait',
      background_color: '#FFFFFF',
      theme_color: '#6C2BFF',
      lang: 'pt-BR',
      icons: GENERIC_ICONS,
    };
  }

  return {
    name: `${tenant.nome} · Técnico Virtual`,
    short_name: tenant.nome,
    description: `Diagnóstico automático de conectividade — ${tenant.nome}.`,
    start_url: `/${tenant.id}`,
    scope: '/',
    id: `/${tenant.id}`,
    display: 'standalone',
    orientation: 'portrait',
    // Mesmo par de cor do splash (EstadoSplash) — evita flash branco entre o toque no ícone
    // instalado e o app carregar.
    background_color: tenant.colorBrand,
    theme_color: tenant.colorBrand,
    lang: 'pt-BR',
    icons: iconsFor(tenant),
  };
}
