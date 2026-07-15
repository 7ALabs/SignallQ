import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { VersionsTab } from "./VersionsTab";

// GH#781 — paridade com o mockup: KPIs de versão estável/rollout/crash rate/ANR
// rate, tabela "Versões em produção" e card "Notas de release recentes".
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
    // #880 (achado 3): getFirebaseVersions agora devolve {source, versions}
    // em vez de T[] | null — source distingue "sem credenciais" de "sem
    // volume ainda"/"erro", em vez de colapsar tudo num null genérico.
    getFirebaseVersions: vi.fn().mockResolvedValue({ source: "no_credentials", versions: [] }),
    getGooglePlayCrashAnr: vi.fn().mockResolvedValue(null),
    getGooglePlayTracks: vi.fn().mockResolvedValue([]),
    getGooglePlayVersions: vi.fn().mockResolvedValue([]),
  },
}));

describe("VersionsTab", () => {
  it("renderiza a tabela de versões em produção", async () => {
    render(<VersionsTab environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />);

    expect(await screen.findByText("Versões em produção")).toBeInTheDocument();
  });

  it("mostra aviso de Crashlytics não configurado quando crashStats é null", async () => {
    render(<VersionsTab environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />);

    expect(await screen.findByText("Crash rate (release atual)")).toBeInTheDocument();
    expect(screen.getAllByText("Não configurado").length).toBeGreaterThan(0);
  });

  it("mostra estado vazio explícito para notas de release quando o worker não expõe changelog", async () => {
    render(<VersionsTab environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />);

    expect(await screen.findByText("Notas de release recentes")).toBeInTheDocument();
    expect(await screen.findByText("Notas de release não disponíveis")).toBeInTheDocument();
  });
});
