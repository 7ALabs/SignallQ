import { apiClient } from "./apiClient";

// Catálogo de anúncios locais (house ads) — issue #1402/#1404. Fallback exibido pelo
// Site quando o AdSense não preenche o slot (no-fill) ou não está configurado.
// Contrato real implementado pelo Camilo em
// signallq-admin-worker/src/index.ts (handleAdminLocalAdsList/CreateLocalAd/
// UpdateLocalAd/DeleteLocalAd) — campos em camelCase no JSON (coluna D1 é snake_case,
// mapeada pelo worker).

export interface LocalAd {
  id: string;
  title: string;
  description: string;
  ctaLabel: string;
  targetUrl: string;
  active: boolean;
  updatedAt: number;
}

export type LocalAdInput = Omit<LocalAd, "id" | "updatedAt">;

let mockStore: LocalAd[] = [
  {
    id: "house-1",
    title: "Teste sua velocidade com o app SignallQ",
    description: "Diagnóstico completo de rede, grátis, sem cadastro.",
    ctaLabel: "Baixar agora",
    targetUrl: "https://signallq.pages.dev",
    active: true,
    updatedAt: Date.now() / 1000,
  },
];

export const localAdsService = {
  async getAds(): Promise<LocalAd[]> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return [];
      try {
        const raw = await apiClient.request<{ ads: any[] }>("GET", "/admin/local-ads");
        return raw.ads.map((a) => ({
          id:          a.id,
          title:       a.title ?? "",
          description: a.description ?? "",
          ctaLabel:    a.ctaLabel ?? "",
          targetUrl:   a.targetUrl ?? "",
          active:      Boolean(a.active),
          updatedAt:   a.updatedAt ?? 0,
        }));
      } catch {
        return [];
      }
    }
    return JSON.parse(JSON.stringify(mockStore));
  },

  async createAd(input: LocalAdInput): Promise<boolean> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return false;
      try {
        await apiClient.request("POST", "/admin/local-ads", { ...input });
        return true;
      } catch {
        return false;
      }
    }
    mockStore = [
      ...mockStore,
      { ...input, id: `house-${Date.now()}`, updatedAt: Date.now() / 1000 },
    ];
    return true;
  },

  async updateAd(id: string, input: LocalAdInput): Promise<boolean> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return false;
      try {
        await apiClient.request("PUT", `/admin/local-ads/${id}`, { ...input });
        return true;
      } catch {
        return false;
      }
    }
    mockStore = mockStore.map((ad) =>
      ad.id === id ? { ...ad, ...input, updatedAt: Date.now() / 1000 } : ad
    );
    return true;
  },

  async deleteAd(id: string): Promise<boolean> {
    if (!apiClient.isMockEnabled()) {
      if (!import.meta.env.VITE_ADMIN_API_BASE_URL) return false;
      try {
        await apiClient.request("DELETE", `/admin/local-ads/${id}`);
        return true;
      } catch {
        return false;
      }
    }
    mockStore = mockStore.filter((ad) => ad.id !== id);
    return true;
  },
};
