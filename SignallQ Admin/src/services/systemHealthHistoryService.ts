import { apiClient } from "./apiClient";
import { mockSystemHealthHistory } from "../mocks/systemHealthHistory.mock";

export interface DailyHealthPoint {
  date: string; // YYYY-MM-DD (UTC)
  latencyP95Ms: number | null;
  uptimePercentage: number | null;
}

export interface SystemHealthHistoryResponse {
  source: string;
  period: string;
  points: DailyHealthPoint[];
}

// #788 — série diária gravada por Cron Trigger no worker (system_health_snapshots),
// não estimada no client. Dia sem nenhum snapshot "ok" de D1 vem com
// latencyP95Ms: null — o gráfico deve mostrar a lacuna, não interpolar.
export const systemHealthHistoryService = {
  async getHistory(days = 14): Promise<SystemHealthHistoryResponse> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return apiClient.simulateFetch(mockSystemHealthHistory, { endpoint: "system-health-history" });
    }

    return apiClient.request<SystemHealthHistoryResponse>("GET", `/admin/system-health/history?days=${days}`);
  },
};
