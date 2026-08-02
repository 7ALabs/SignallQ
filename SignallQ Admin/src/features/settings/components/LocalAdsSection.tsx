import React, { useEffect, useState } from "react";
import { Megaphone, Pencil, Trash2, ToggleLeft, ToggleRight, Plus, X } from "lucide-react";
import { localAdsService, LocalAd, LocalAdInput } from "../../../services/localAdsService";
import { SectionCard } from "../../../components/ui/SectionCard";

const FORM_VAZIO: LocalAdInput = { title: "", description: "", ctaLabel: "", targetUrl: "", active: true };

/**
 * CRUD do catálogo de anúncios locais (house ads) — issue #1402/#1404. Exibido pelo
 * Site quando o AdSense entra em no-fill. Painel interno do Console: segue o padrão
 * visual já existente (SectionCard + lista, mesmo espírito de FeatureFlagsTab), não é
 * a superfície que o protótipo do Luiz vai redesenhar (essa é o card no Site).
 */
export const LocalAdsSection: React.FC = () => {
  const [ads, setAds] = useState<LocalAd[]>([]);
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [formAberto, setFormAberto] = useState(false);
  const [form, setForm] = useState<LocalAdInput>(FORM_VAZIO);

  const carregar = () => {
    setLoading(true);
    localAdsService
      .getAds()
      .then((data) => setAds(data))
      .catch(() => setAds([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    carregar();
  }, []);

  const abrirNovo = () => {
    setEditandoId(null);
    setForm(FORM_VAZIO);
    setFormAberto(true);
    setErro(null);
  };

  const abrirEdicao = (ad: LocalAd) => {
    setEditandoId(ad.id);
    setForm({ title: ad.title, description: ad.description, ctaLabel: ad.ctaLabel, targetUrl: ad.targetUrl, active: ad.active });
    setFormAberto(true);
    setErro(null);
  };

  const fecharForm = () => {
    setFormAberto(false);
    setEditandoId(null);
    setForm(FORM_VAZIO);
    setErro(null);
  };

  const salvar = async () => {
    if (!form.title.trim() || !form.targetUrl.trim()) {
      setErro("Título e URL de destino são obrigatórios.");
      return;
    }
    setSalvando(true);
    setErro(null);
    const ok = editandoId
      ? await localAdsService.updateAd(editandoId, form)
      : await localAdsService.createAd(form);
    setSalvando(false);
    if (!ok) {
      setErro("Falha ao salvar. Tente novamente.");
      return;
    }
    fecharForm();
    carregar();
  };

  const excluir = async (ad: LocalAd) => {
    // Achado da Marina (revisão de 2026-07-25, issue #1402/#1405): exclusão disparava
    // direto no clique, sem confirmação — risco real pro Luiz apagar por engano um
    // anúncio já curado, sem undo (mesmo padrão de confirmação de ação destrutiva já usado
    // em `ToolsPage.tsx`/`IntegrationsSettings.tsx`).
    if (!window.confirm(`Excluir o anúncio "${ad.title}"? Essa ação não pode ser desfeita.`)) return;
    setSalvando(true);
    const ok = await localAdsService.deleteAd(ad.id);
    setSalvando(false);
    if (ok) carregar();
    else setErro("Falha ao excluir. Tente novamente.");
  };

  const alternarAtivo = async (ad: LocalAd) => {
    setSalvando(true);
    const ok = await localAdsService.updateAd(ad.id, {
      title: ad.title,
      description: ad.description,
      ctaLabel: ad.ctaLabel,
      targetUrl: ad.targetUrl,
      active: !ad.active,
    });
    setSalvando(false);
    if (ok) carregar();
    else setErro("Falha ao atualizar. Tente novamente.");
  };

  return (
    <SectionCard
      title="Anúncios locais (house ads)"
      description="Catálogo sorteado no Site quando o AdSense não preenche o slot de Resultado (no-fill) — endpoint público GET /local-ads."
      id="local-ads-section"
      actions={
        !formAberto && (
          <button
            type="button"
            onClick={abrirNovo}
            className="flex items-center gap-1.5 text-[11px] font-sans font-semibold px-3 py-1.5 rounded-lg bg-[var(--primary)] text-white hover:opacity-90 transition-opacity cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5" />
            Novo anúncio
          </button>
        )
      }
    >
      <div className="space-y-4 font-sans text-xs">
        {formAberto && (
          <div className="border border-[var(--border)] rounded-xl p-4 space-y-3 bg-[var(--bg-base)]">
            <div className="flex items-center justify-between">
              <span className="text-[12px] font-semibold text-[var(--text-primary)]">
                {editandoId ? "Editar anúncio" : "Novo anúncio"}
              </span>
              <button type="button" onClick={fecharForm} aria-label="Cancelar" className="cursor-pointer">
                <X className="w-4 h-4 text-[var(--text-tertiary)]" />
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label className="space-y-1 block">
                <span className="text-[var(--text-secondary)]">Título</span>
                <input
                  value={form.title}
                  onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                  className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-3 py-2 text-[var(--text-primary)]"
                  placeholder="Ex.: Teste sua velocidade com o app SignallQ"
                />
              </label>
              <label className="space-y-1 block">
                <span className="text-[var(--text-secondary)]">CTA (texto do botão)</span>
                <input
                  value={form.ctaLabel}
                  onChange={(e) => setForm((f) => ({ ...f, ctaLabel: e.target.value }))}
                  className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-3 py-2 text-[var(--text-primary)]"
                  placeholder="Ex.: Baixar agora"
                />
              </label>
              <label className="space-y-1 block sm:col-span-2">
                <span className="text-[var(--text-secondary)]">Descrição</span>
                <input
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                  className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-3 py-2 text-[var(--text-primary)]"
                  placeholder="Ex.: Diagnóstico completo de rede, grátis."
                />
              </label>
              <label className="space-y-1 block sm:col-span-2">
                <span className="text-[var(--text-secondary)]">URL de destino</span>
                <input
                  value={form.targetUrl}
                  onChange={(e) => setForm((f) => ({ ...f, targetUrl: e.target.value }))}
                  className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-3 py-2 text-[var(--text-primary)]"
                  placeholder="https://…"
                />
              </label>
            </div>

            <label className="flex items-center gap-2 cursor-pointer w-fit">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
              />
              <span className="text-[var(--text-secondary)]">Ativo (entra no sorteio do Site)</span>
            </label>

            {erro && <p className="text-red-400 text-[11px]">{erro}</p>}

            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={fecharForm}
                className="text-[11px] font-semibold px-3 py-1.5 rounded-lg border border-[var(--border)] text-[var(--text-secondary)] cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={salvar}
                disabled={salvando}
                className="text-[11px] font-semibold px-3 py-1.5 rounded-lg bg-[var(--primary)] text-white disabled:opacity-50 cursor-pointer"
              >
                {salvando ? "Salvando…" : "Salvar"}
              </button>
            </div>
          </div>
        )}

        {loading && (
          <div className="space-y-2">
            {Array.from({ length: 2 }).map((_, i) => (
              <div key={i} className="h-16 bg-[var(--bg-surface-muted)] rounded-lg animate-pulse" />
            ))}
          </div>
        )}

        {!loading && ads.length === 0 && (
          <div className="flex flex-col items-center justify-center py-10 border border-dashed border-[var(--border)] rounded-xl text-[var(--text-tertiary)]">
            <Megaphone className="w-6 h-6 mb-2 opacity-40" />
            <p>Nenhum anúncio local cadastrado.</p>
          </div>
        )}

        {!loading && ads.length > 0 && (
          <ul className="space-y-2">
            {ads.map((ad) => (
              <li
                key={ad.id}
                className="flex items-center justify-between gap-3 border border-[var(--border)] rounded-xl p-3"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-[var(--text-primary)] truncate">{ad.title}</span>
                    <span
                      className={`shrink-0 text-[9px] uppercase tracking-wider px-1.5 py-0.5 rounded-md ${
                        ad.active
                          ? "bg-emerald-950/60 text-emerald-400 border border-emerald-800/40"
                          : "bg-zinc-900 text-[var(--text-tertiary)] border border-zinc-800"
                      }`}
                    >
                      {ad.active ? "ativo" : "inativo"}
                    </span>
                  </div>
                  <p className="text-[11px] text-[var(--text-secondary)] truncate">{ad.description}</p>
                </div>

                <div className="flex items-center gap-1 shrink-0">
                  <button
                    type="button"
                    onClick={() => alternarAtivo(ad)}
                    aria-label={`${ad.active ? "Desativar" : "Ativar"} ${ad.title}`}
                    disabled={salvando}
                    className="cursor-pointer disabled:opacity-50"
                  >
                    {ad.active ? (
                      <ToggleRight className="w-6 h-6 text-[var(--primary)]" />
                    ) : (
                      <ToggleLeft className="w-6 h-6 text-zinc-600" />
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={() => abrirEdicao(ad)}
                    aria-label={`Editar ${ad.title}`}
                    className="p-1.5 cursor-pointer text-[var(--text-tertiary)] hover:text-[var(--text-primary)]"
                  >
                    <Pencil className="w-3.5 h-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => excluir(ad)}
                    aria-label={`Excluir ${ad.title}`}
                    disabled={salvando}
                    className="p-1.5 cursor-pointer text-[var(--text-tertiary)] hover:text-red-400 disabled:opacity-50"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </SectionCard>
  );
};
