import { apiClient } from "./apiClient";
import { mockCloudflareUsage } from "../mocks/cloudflareUsage.mock";

export type CloudflareUsageSource = "graphql" | "not_configured" | "error";

export interface CloudflareResourceUsage {
  available: boolean;
  used: number | null;
  limit: number | null;
  percentage: number | null;
  unit: "requests" | "rows" | "bytes";
  reason?: string;
}

export interface CloudflareUsageResponse {
  source: CloudflareUsageSource;
  timestamp: string;
  resources: {
    workersRequestsDay: CloudflareResourceUsage;
    d1RowsReadDay: CloudflareResourceUsage;
    d1RowsWrittenDay: CloudflareResourceUsage;
    d1StorageTotal: CloudflareResourceUsage;
  };
}

// #883 — cada número vem de GET /admin/cloudflare-usage, que por sua vez consulta
// a GraphQL Analytics API do Cloudflare (conta inteira, plano gratuito). Sem
// CLOUDFLARE_API_TOKEN configurado no worker, a resposta já vem com
// available: false + motivo em cada recurso — sem mock silencioso em produção.
export const cloudflareUsageService = {
  async getUsage(): Promise<CloudflareUsageResponse> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return apiClient.simulateFetch(mockCloudflareUsage, { endpoint: "cloudflare-usage" });
    }

    return apiClient.request<CloudflareUsageResponse>("GET", "/admin/cloudflare-usage");
  },
};
