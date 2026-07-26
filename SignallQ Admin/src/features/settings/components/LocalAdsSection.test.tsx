import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { LocalAdsSection } from "./LocalAdsSection";
import { localAdsService } from "../../../services/localAdsService";

// Issue #1402/#1404 — CRUD do catálogo de anúncios locais no Console. Mocka o service
// diretamente (mesmo padrão de NetworksOperatorsPage.test.tsx / DiagnosticsPage.test.tsx).
vi.mock("../../../services/localAdsService", () => ({
  localAdsService: {
    getAds: vi.fn(),
    createAd: vi.fn(),
    updateAd: vi.fn(),
    deleteAd: vi.fn(),
  },
}));

const AD_MOCK = {
  id: "house-1",
  title: "Teste sua velocidade com o app SignallQ",
  description: "Diagnóstico completo de rede, grátis.",
  ctaLabel: "Baixar agora",
  targetUrl: "https://signallq.pages.dev",
  active: true,
  updatedAt: 1700000000,
};

describe("LocalAdsSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(localAdsService.getAds).mockResolvedValue([AD_MOCK]);
    vi.mocked(localAdsService.createAd).mockResolvedValue(true);
    vi.mocked(localAdsService.updateAd).mockResolvedValue(true);
    vi.mocked(localAdsService.deleteAd).mockResolvedValue(true);
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("lista os anúncios existentes com status ativo/inativo", async () => {
    render(<LocalAdsSection />);
    expect(await screen.findByText(AD_MOCK.title)).toBeInTheDocument();
    expect(screen.getByText("ativo")).toBeInTheDocument();
  });

  it("mostra estado vazio quando não há anúncios cadastrados", async () => {
    vi.mocked(localAdsService.getAds).mockResolvedValue([]);
    render(<LocalAdsSection />);
    expect(await screen.findByText("Nenhum anúncio local cadastrado.")).toBeInTheDocument();
  });

  it("cria um novo anúncio pelo formulário", async () => {
    render(<LocalAdsSection />);
    await screen.findByText(AD_MOCK.title);

    fireEvent.click(screen.getByText("Novo anúncio"));
    fireEvent.change(screen.getByPlaceholderText(/Teste sua velocidade/), {
      target: { value: "Novo house ad" },
    });
    fireEvent.change(screen.getByPlaceholderText("https://…"), {
      target: { value: "https://example.com" },
    });
    fireEvent.click(screen.getByText("Salvar"));

    await waitFor(() =>
      expect(localAdsService.createAd).toHaveBeenCalledWith(
        expect.objectContaining({ title: "Novo house ad", targetUrl: "https://example.com" })
      )
    );
  });

  it("bloqueia salvar sem título/URL preenchidos", async () => {
    render(<LocalAdsSection />);
    await screen.findByText(AD_MOCK.title);

    fireEvent.click(screen.getByText("Novo anúncio"));
    fireEvent.click(screen.getByText("Salvar"));

    expect(await screen.findByText(/obrigatórios/)).toBeInTheDocument();
    expect(localAdsService.createAd).not.toHaveBeenCalled();
  });

  it("alterna ativo/inativo de um anúncio existente", async () => {
    render(<LocalAdsSection />);
    await screen.findByText(AD_MOCK.title);

    fireEvent.click(screen.getByLabelText(`Desativar ${AD_MOCK.title}`));

    await waitFor(() =>
      expect(localAdsService.updateAd).toHaveBeenCalledWith(
        AD_MOCK.id,
        expect.objectContaining({ active: false })
      )
    );
  });

  it("exclui um anúncio existente após confirmar", async () => {
    render(<LocalAdsSection />);
    await screen.findByText(AD_MOCK.title);

    fireEvent.click(screen.getByLabelText(`Excluir ${AD_MOCK.title}`));

    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining(AD_MOCK.title));
    await waitFor(() => expect(localAdsService.deleteAd).toHaveBeenCalledWith(AD_MOCK.id));
  });

  it("não exclui quando a confirmação é recusada", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    render(<LocalAdsSection />);
    await screen.findByText(AD_MOCK.title);

    fireEvent.click(screen.getByLabelText(`Excluir ${AD_MOCK.title}`));

    expect(localAdsService.deleteAd).not.toHaveBeenCalled();
  });
});
