import { CloudflareUsageResponse } from "../services/cloudflareUsageService";

// #883 — mock reflete estado plausível de conta em desenvolvimento (uso baixo,
// bem longe do teto do free tier), não um cenário de estouro forçado.
export const mockCloudflareUsage: CloudflareUsageResponse = {
  source: "graphql",
  timestamp: new Date().toISOString(),
  resources: {
    workersRequestsDay: { available: true, used: 2836, limit: 100_000, percentage: 3, unit: "requests" },
    d1RowsReadDay:      { available: true, used: 8073, limit: 5_000_000, percentage: 1, unit: "rows" },
    d1RowsWrittenDay:   { available: true, used: 1514, limit: 100_000, percentage: 2, unit: "rows" },
    d1StorageTotal:     { available: true, used: 532_480, limit: 5 * 1024 * 1024 * 1024, percentage: 0, unit: "bytes" },
  },
};
