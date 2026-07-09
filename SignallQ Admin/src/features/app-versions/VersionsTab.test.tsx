import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { VersionsTab } from "./VersionsTab";

// GH#746 — smoke test: gráfico principal que faltava (sessões por versão),
// mesmo padrão de mock de service usado em AiCostPage.test.tsx.
vi.mock("../../services/appVersionsService", () => ({
  appVersionsService: {
    getAppVersions: vi.fn().mockResolvedValue({
      versions: [
        { appVersion: "0.24.2", versionCode: 59, distChannel: "play_store", buildType: "release", sessions: 812, avgScore: 81, firstSeen: 1750000000, lastSeen: 1751000000 },
        { appVersion: "0.24.1", versionCode: 58, distChannel: "play_store", buildType: "release", sessions: 340, avgScore: 76, firstSeen: 1749000000, lastSeen: 1750500000 },
      ],
      productionVersion: { appVersion: "0.24.2", versionCode: 59, distChannel: "play_store", buildType: "release", sessions: 812, avgScore: 81, firstSeen: 1750000000, lastSeen: 1751000000 },
    }),
  },
}));

vi.mock("../../integrations/integrationsService", () => ({
  integrationsService: {
    getFirebaseVersions: vi.fn().mockResolvedValue(null),
    getGooglePlayCrashAnr: vi.fn().mockResolvedValue(null),
  },
}));

describe("VersionsTab", () => {
  it("renderiza o gráfico principal de sessões por versão", async () => {
    render(<VersionsTab environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />);

    expect(await screen.findByText("Sessões por versão")).toBeInTheDocument();
  });

  it("mostra aviso de Crashlytics não configurado quando crashStats é null", async () => {
    render(<VersionsTab environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />);

    expect(await screen.findByText("Cobertura Crashlytics")).toBeInTheDocument();
    expect(screen.getAllByText("Não configurado").length).toBeGreaterThan(0);
  });
});
