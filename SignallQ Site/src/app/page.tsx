"use client";

import { useState } from "react";
import { PageShell } from "@/components/PageShell";
import { SpeedDial } from "@/components/SpeedDial";
import { MetricCard } from "@/components/MetricCard";
import { EmptyState } from "@/components/EmptyState";
import { NoticeBar } from "@/components/NoticeBar";
import { KeyValueList } from "@/components/KeyValueList";
import { useSpeedTest, FasePainel, ProblemPhase } from "@/hooks/useSpeedTest";
import { classifyDownload } from "@/lib/classification";
import { fractionForLatency, fractionForThroughput } from "@/lib/gaugeMath";

const RUNNING_PHASES: FasePainel[] = [
  "preparando",
  "latencia",
  "download",
  "upload",
  "processando",
];
const PROBLEM_PHASES: ProblemPhase[] = [
  "sem-conexao",
  "conexao-interrompida",
  "endpoint-indisponivel",
  "erro-inesperado",
  "cancelado",
  "bloqueado-outra-aba",
];

const DIAG_ITEMS = [
  { icon: "speed", title: "Velocidade e latência" },
  { icon: "hourglass_bottom", title: "Latência sob carga" },
  { icon: "dns", title: "Servidores DNS" },
  { icon: "sports_esports", title: "Modo gamer" },
];

const USE_CASES = [
  { icon: "travel_explore", label: "Navegação", verdict: "Boa", color: "var(--success)" },
  { icon: "movie", label: "Streaming", verdict: "Boa", color: "var(--success)" },
  { icon: "videocam", label: "Videochamadas", verdict: "Boa", color: "var(--success)" },
  { icon: "sports_esports", label: "Jogos online", verdict: "Boa", color: "var(--success)" },
];

const PROBLEMAS = {
  "sem-conexao": {
    icon: "wifi_off",
    title: "Sem conexão",
    message: "Não conseguimos detectar uma conexão com a internet neste aparelho.",
    actionIcon: "refresh",
    actionLabel: "Tentar novamente",
    color: "var(--error)",
  },
  cancelado: {
    icon: "cancel",
    title: "Teste cancelado",
    message: "Você cancelou a medição antes do fim.",
    actionIcon: "refresh",
    actionLabel: "Tentar novamente",
    color: "var(--text-secondary)",
  },
  "bloqueado-outra-aba": {
    icon: "tab",
    title: "Teste em andamento em outra aba",
    message:
      "Evitamos rodar duas medições ao mesmo tempo no mesmo navegador. Você pode iniciar mesmo assim, se preferir.",
    actionIcon: "play_arrow",
    actionLabel: "Iniciar mesmo assim",
    color: "var(--text-secondary)",
  },
  "conexao-interrompida": {
    icon: "sync_problem",
    title: "Conexão interrompida",
    message:
      "A conexão caiu no meio da medição e não foi possível concluir com confiança.",
    actionIcon: "refresh",
    actionLabel: "Tentar novamente",
    color: "var(--error)",
  },
  "endpoint-indisponivel": {
    icon: "cloud_off",
    title: "Servidor indisponível",
    message:
      "Não foi possível contatar o servidor de medição agora. Tente novamente em instantes.",
    actionIcon: "refresh",
    actionLabel: "Tentar novamente",
    color: "var(--error)",
  },
  "erro-inesperado": {
    icon: "error",
    title: "Erro inesperado",
    message: "Algo deu errado durante a medição.",
    actionIcon: "refresh",
    actionLabel: "Tentar novamente",
    color: "var(--error)",
  },
};

export default function Home() {
  const [modo, setModo] = useState<"rapido" | "completo" | "triplo">("rapido");
  const {
    phase,
    liveValue,
    phaseResults,
    result,
    cancelTest,
    retry,
    forceStart,
  } = useSpeedTest(modo);

  const isIdle = phase === "idle";
  const isRunning = RUNNING_PHASES.includes(phase);
  const isResult =
    phase === "concluido" ||
    phase === "parcial" ||
    phase === "inconclusivo" ||
    phase === "contaminado";
  const isPartial = phase === "parcial" || phase === "inconclusivo";
  const isProblem = PROBLEM_PHASES.includes(phase as ProblemPhase);

  // Calc Fraction and Color
  let fraction = 0;
  let phaseColor = "var(--accent)";
  let dialNumber = "—";
  let dialUnit = "";
  let dialLabel = "";

  if (phase === "latencia") {
    fraction = fractionForLatency(liveValue);
    phaseColor = "var(--phase-latencia)";
    dialNumber = liveValue ? Math.round(liveValue).toString() : "—";
    dialUnit = "ms";
    dialLabel = "Latência";
  } else if (phase === "download") {
    fraction = fractionForThroughput(liveValue);
    phaseColor = "var(--phase-download)";
    dialNumber = liveValue ? liveValue.toFixed(1) : "0.0";
    dialUnit = "Mbps";
    dialLabel = "Download";
  } else if (phase === "upload") {
    fraction = fractionForThroughput(liveValue);
    phaseColor = "var(--phase-upload)";
    dialNumber = liveValue ? liveValue.toFixed(1) : "0.0";
    dialUnit = "Mbps";
    dialLabel = "Upload";
  } else if (phase === "processando") {
    fraction = 1;
    phaseColor = "var(--accent)";
  }

  const p = isProblem
    ? PROBLEMAS[phase as keyof typeof PROBLEMAS]
    : PROBLEMAS["erro-inesperado"];

  return (
    <PageShell ads={!isResult}>
      {isIdle && (
        <div className="flex flex-col items-center gap-[6px] text-center">
          <h1 className="m-0 font-bold text-[30px] leading-[1.2] text-[color:var(--text-primary)]">
            Teste de velocidade
          </h1>
          <p className="m-0 max-w-[460px] font-normal text-[14px] leading-[1.43] text-[color:var(--text-secondary)]">
            Download, upload e latência medidos no seu navegador.
          </p>
        </div>
      )}

      {(isIdle || isRunning) && (
        <div className="w-full max-w-[440px] flex flex-col items-center gap-5 mt-2">
          <SpeedDial
            value={dialNumber}
            fraction={fraction}
            phaseColor={phaseColor}
            isRunning={isRunning}
            mobile={false}
          >
            {isIdle && (
              <div className="absolute left-1/2 bottom-[26px] -translate-x-1/2 flex flex-col items-center gap-[10px]">
                <div
                  onClick={forceStart}
                  className="h-[44px] px-[26px] rounded-full flex items-center gap-2 bg-[color:var(--accent)] shadow-[0_14px_30px_color-mix(in_srgb,_var(--accent)_45%,_transparent),_0_2px_6px_rgba(0,0,0,.25)] whitespace-nowrap cursor-pointer hover:scale-105 transition-transform"
                >
                  <span className="material-symbols-outlined text-[18px] text-[color:var(--on-accent)]">
                    speed
                  </span>
                  <span className="font-semibold text-[16px] leading-[1.15] text-[color:var(--on-accent)]">
                    Iniciar teste
                  </span>
                </div>
                <span className="font-normal text-[12px] leading-[1.3] text-[color:var(--text-tertiary)]">
                  {modo === "rapido" ? "Rápido · ~20 s" : "Completo · ~40 s"}
                </span>
              </div>
            )}

            {isRunning && (
              <div className="absolute left-0 right-0 bottom-1 flex flex-col items-center">
                <div className="flex items-center gap-[6px]">
                  <span
                    className="material-symbols-outlined text-[18px]"
                    style={{ color: phaseColor }}
                  >
                    {phase === "latencia"
                      ? "network_ping"
                      : phase === "download"
                      ? "arrow_downward"
                      : "arrow_upward"}
                  </span>
                  <span className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-secondary)] tracking-[.3px] uppercase">
                    {dialLabel}
                  </span>
                </div>
                <div className="font-bold text-[54px] leading-none text-[color:var(--text-primary)] tabular-nums">
                  {dialNumber}
                </div>
                <div className="font-medium text-[14px] leading-[1.43] text-[color:var(--text-secondary)]">
                  {dialUnit}
                </div>
              </div>
            )}
          </SpeedDial>

          <div className="w-full max-w-[520px] flex">
            <div className="flex-1 flex items-center justify-center gap-[10px] py-3 px-4">
              <span className="material-symbols-outlined text-[20px] text-[color:var(--text-secondary)]">
                dns
              </span>
              <span className="font-medium text-[14px] leading-[1.43] text-[color:var(--text-primary)]">
                Rede Cloudflare
              </span>
            </div>
          </div>

          {isIdle && (
            <>
              <div className="w-full max-w-[220px] flex border border-[color:var(--border)] rounded-full p-[2px]">
                <div
                  className={`flex-1 text-center rounded-full py-2 px-3 font-medium text-[12px] leading-[1.33] cursor-pointer transition-colors ${
                    modo === "rapido"
                      ? "bg-[color:var(--accent)] text-[color:var(--on-accent)]"
                      : "text-[color:var(--text-primary)] hover:bg-[color:var(--bg-secondary)]"
                  }`}
                  onClick={() => setModo("rapido")}
                >
                  Rápido
                </div>
                <div
                  className={`flex-1 text-center rounded-full py-2 px-3 font-medium text-[12px] leading-[1.33] cursor-pointer transition-colors ${
                    modo === "completo"
                      ? "bg-[color:var(--accent)] text-[color:var(--on-accent)]"
                      : "text-[color:var(--text-primary)] hover:bg-[color:var(--bg-secondary)]"
                  }`}
                  onClick={() => setModo("completo")}
                >
                  Completo
                </div>
              </div>
              <span className="font-medium text-[12px] leading-[1.33] text-[color:var(--accent)] cursor-pointer hover:underline">
                Como medimos sua conexão
              </span>
            </>
          )}
        </div>
      )}

      {isIdle && (
        <div className="w-full flex flex-col gap-4 pt-3">
          <div className="flex flex-col gap-[2px] text-center">
            <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--accent)] tracking-[.3px] uppercase">
              Diagnóstico, não só velocidade
            </div>
            <h2 className="m-0 font-bold text-[20px] leading-[1.3] text-[color:var(--text-primary)]">
              O SignallQ explica por que, não só quanto
            </h2>
          </div>

          <div className="flex flex-wrap justify-center gap-2">
            {DIAG_ITEMS.map((d, i) => (
              <div
                key={i}
                className="flex items-center gap-2 rounded-full py-2 pr-[14px] pl-[10px] bg-[color:var(--bg-secondary)] shadow-[0_4px_14px_rgba(0,0,0,.16)]"
              >
                <span className="material-symbols-outlined text-[16px] text-[color:var(--accent)]">
                  {d.icon}
                </span>
                <span className="font-medium text-[13px] leading-[1.3] text-[color:var(--text-primary)] whitespace-nowrap">
                  {d.title}
                </span>
              </div>
            ))}
          </div>

          <div className="flex flex-wrap items-center gap-4 rounded-[20px] py-5 px-[22px] box-border bg-[linear-gradient(135deg,_color-mix(in_srgb,_var(--accent)_16%,_var(--bg-secondary)),_var(--bg-secondary))] shadow-[0_16px_40px_rgba(0,0,0,.22),_inset_0_1px_0_rgba(255,255,255,.05)]">
            <div className="flex-[1_1_260px] min-w-0 flex flex-col gap-1">
              <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase">
                Só no app SignallQ
              </div>
              <div className="font-semibold text-[16px] leading-[1.35] text-[color:var(--text-primary)]">
                Wi-Fi cômodo a cômodo, sinal móvel e mais
              </div>
              <div className="font-normal text-[13px] leading-[1.45] text-[color:var(--text-secondary)]">
                O navegador não lê rádio Wi-Fi nem sinal 4G/5G. O app mede isso
                e mostra os dispositivos conectados à sua rede.
              </div>
            </div>
          </div>
        </div>
      )}

      {isRunning && (
        <>
          <div className="w-full max-w-[520px] flex border border-[color-mix(in_srgb,_var(--border)_22%,_transparent)] rounded-2xl overflow-hidden">
            <div className="flex-1 flex flex-col items-center gap-1 py-[14px] px-2">
              <span className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase">
                Latência
              </span>
              <span
                className="font-medium text-[14px] leading-[1.43]"
                style={{
                  color:
                    phase === "latencia"
                      ? "var(--phase-latencia)"
                      : "var(--text-primary)",
                }}
              >
                {phaseResults.latencia ? Math.round(phaseResults.latencia) + " ms" : "—"}
              </span>
            </div>
            <div className="flex-1 flex flex-col items-center gap-1 py-[14px] px-2 border-l border-[color-mix(in_srgb,_var(--border)_22%,_transparent)]">
              <span className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase">
                Download
              </span>
              <span
                className="font-medium text-[14px] leading-[1.43]"
                style={{
                  color:
                    phase === "download"
                      ? "var(--phase-download)"
                      : phaseResults.download
                      ? "var(--text-primary)"
                      : "var(--text-tertiary)",
                }}
              >
                {phaseResults.download ? phaseResults.download.toFixed(1) + " Mbps" : "Aguardando"}
              </span>
            </div>
            <div className="flex-1 flex flex-col items-center gap-1 py-[14px] px-2 border-l border-[color-mix(in_srgb,_var(--border)_22%,_transparent)]">
              <span className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase">
                Upload
              </span>
              <span
                className="font-medium text-[14px] leading-[1.43]"
                style={{
                  color:
                    phase === "upload"
                      ? "var(--phase-upload)"
                      : phaseResults.upload
                      ? "var(--text-primary)"
                      : "var(--text-tertiary)",
                }}
              >
                {phaseResults.upload ? phaseResults.upload.toFixed(1) + " Mbps" : "Aguardando"}
              </span>
            </div>
          </div>
          <button
            onClick={cancelTest}
            className="h-[40px] flex items-center gap-[6px] bg-transparent"
          >
            <span className="material-symbols-outlined text-[18px]">
              close
            </span>
            <span className="font-medium text-[14px] leading-[1.43] text-[color:var(--text-primary)]">
              Cancelar teste
            </span>
          </button>
        </>
      )}

      {isProblem && (
        <EmptyState
          icon={p.icon}
          title={p.title}
          message={p.message}
          actionIcon={p.actionIcon}
          actionLabel={p.actionLabel}
          color={p.color}
          onAction={phase === "bloqueado-outra-aba" ? forceStart : retry}
        />
      )}

      {isResult && result && (
        <div className="w-full max-w-[680px] flex flex-col gap-6">
          <div className="flex items-center justify-center gap-2">
            <span
              className="material-symbols-outlined text-[18px]"
              style={{ color: isPartial ? "var(--warning)" : "var(--success)" }}
            >
              {isPartial ? "warning" : "check_circle"}
            </span>
            <span
              className="font-medium text-[12px] leading-[1.33]"
              style={{ color: isPartial ? "var(--warning)" : "var(--success)" }}
            >
              {isPartial ? "Resultado parcial" : "Medição completa"}
            </span>
            <span className="font-normal text-[12px] leading-[1.33] text-[color:var(--text-tertiary)]">
              • {new Date(result.timestamp).toLocaleString("pt-BR")}
            </span>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <MetricCard
              icon="arrow_downward"
              label="Download"
              value={result.download.mbps ? result.download.mbps.toFixed(1) : "—"}
              unit="Mbps"
              colorClass="text-[color:var(--success)]"
            />
            <MetricCard
              icon="arrow_upward"
              label="Upload"
              value={result.upload?.mbps ? result.upload.mbps.toFixed(1) : "—"}
              unit="Mbps"
              colorClass="text-[color:var(--success)]"
            />
            <MetricCard
              icon="network_ping"
              label="Latência"
              value={result.latency.ms ? Math.round(result.latency.ms).toString() : "—"}
              unit="ms"
              colorClass="text-[color:var(--success)]"
            />
          </div>

          {isPartial && (
            <NoticeBar
              title="Resultado parcial."
              message="Alguma fase não terminou. Use apenas as métricas disponíveis."
              type="warning"
            />
          )}

          <div className="rounded-[20px] p-[22px] box-border bg-[linear-gradient(160deg,_color-mix(in_srgb,_var(--accent)_10%,_var(--bg-secondary)),_var(--bg-secondary))] shadow-[0_16px_36px_rgba(0,0,0,.2)]">
            <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--accent)] tracking-[.3px] uppercase">
              Interpretação SignallQ
            </div>
            <div className="mt-[6px] font-semibold text-[22px] leading-[1.27] text-[color:var(--text-primary)]">
              Conexão {classifyDownload(result.download.mbps || 0).label.toLowerCase()}
            </div>
            <div className="mt-1 font-normal text-[14px] leading-[1.43] text-[color:var(--text-secondary)]">
              Sua velocidade de download permite realizar a maioria das atividades básicas da internet.
            </div>
            
            <div className="mt-[14px] grid grid-cols-2 md:grid-cols-4 gap-[10px]">
              {USE_CASES.map((u, i) => (
                <div
                  key={i}
                  className="flex flex-col items-center justify-center gap-1 min-h-[86px] rounded-[12px] p-3 box-border bg-[color:var(--bg-primary)]"
                >
                  <span
                    className="material-symbols-outlined text-[20px]"
                    style={{ color: u.color }}
                  >
                    {u.icon}
                  </span>
                  <span className="font-normal text-[12px] leading-[1.33] text-[color:var(--text-secondary)] text-center">
                    {u.label}
                  </span>
                  <span
                    className="font-medium text-[12px] leading-[1.33]"
                    style={{ color: u.color }}
                  >
                    {u.verdict}
                  </span>
                </div>
              ))}
            </div>

            <p className="mt-3 mb-0 font-normal text-[12px] leading-[1.33] text-[color:var(--text-tertiary)]">
              Esta é uma leitura das métricas desta medição, não uma certificação da velocidade contratada.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 rounded-[24px] p-6 box-border bg-[rgba(30,41,59,.45)] border border-[rgba(255,255,255,.08)] shadow-[0_14px_34px_rgba(0,0,0,.2)]">
            <KeyValueList
              title="Contexto"
              items={[
                { label: "Infraestrutura", value: "Rede Cloudflare" },
                { label: "Duração", value: "24 s" },
                { label: "Data e hora local", value: new Date(result.timestamp).toLocaleString("pt-BR") },
              ]}
            />
            <KeyValueList
              title="Detalhes técnicos"
              items={[
                { label: "Jitter", value: result.jitter?.ms ? `${result.jitter.ms.toFixed(1)} ms` : "—" },
                { label: "Bufferbloat", value: "—" },
                { label: "Estabilidade", value: "—" },
                { label: "DNS (DoH)", value: "—" },
              ]}
            />
            <p className="md:col-span-2 mt-1 mb-0 font-normal text-[12px] leading-[1.33] text-[color:var(--text-tertiary)]">
              O navegador não confirma provedor, localização, nem lê sinal Wi-Fi ou 4G/5G — isso é diagnóstico do app.
            </p>
          </div>

          <a href="/como-medimos" className="self-center font-medium text-[14px] leading-[1.43] text-[color:var(--accent)] no-underline hover:underline">
            Entenda como o teste mede sua conexão
          </a>

          <div className="flex gap-3 w-full">
            <button
              onClick={retry}
              className="flex-1 h-[46px] flex items-center justify-center gap-2 rounded-[var(--radius-button)] bg-[color:var(--accent)] hover:brightness-110 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-[20px] text-[color:var(--on-accent)]">
                refresh
              </span>
              <span className="font-medium text-[14px] leading-[1.43] text-[color:var(--on-accent)]">
                Testar novamente
              </span>
            </button>
            <a href="/historico" className="flex-1 h-[46px] flex items-center justify-center gap-2 rounded-[var(--radius-button)] border border-[color:var(--border)] no-underline hover:bg-[color:var(--bg-secondary)] transition-colors cursor-pointer">
              <span className="material-symbols-outlined text-[20px] text-[color:var(--accent)]">
                history
              </span>
              <span className="font-medium text-[14px] leading-[1.43] text-[color:var(--accent)]">
                Ver histórico
              </span>
            </a>
          </div>

          <div className="flex items-center justify-center gap-[10px]">
            <span className="font-normal text-[13px] leading-[1.4] text-[color:var(--text-secondary)]">
              Diagnóstico de Wi-Fi e sinal móvel: só no app.
            </span>
            <img src="/assets/google-play-badge.png" alt="Baixar o app SignallQ" className="h-[32px] w-auto block" />
          </div>

          <div className="flex flex-wrap justify-center gap-[10px]">
            <div className="flex items-center gap-[6px] h-[36px] px-2 cursor-pointer hover:bg-[color:var(--bg-secondary)] rounded-full transition-colors">
              <span className="material-symbols-outlined text-[16px] text-[color:var(--accent)]">
                share
              </span>
              <span className="font-medium text-[12px] leading-[1.33] text-[color:var(--accent)]">
                Compartilhar
              </span>
            </div>
            <div className="flex items-center gap-[6px] h-[36px] px-2 cursor-pointer hover:bg-[color:var(--bg-secondary)] rounded-full transition-colors">
              <span className="material-symbols-outlined text-[16px] text-[color:var(--accent)]">
                content_copy
              </span>
              <span className="font-medium text-[12px] leading-[1.33] text-[color:var(--accent)]">
                Copiar resumo
              </span>
            </div>
          </div>

          {phase === "inconclusivo" && (
            <div className="flex flex-col gap-[10px] rounded-[16px] p-5 box-border bg-[color:var(--bg-secondary)]">
              <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--accent)] tracking-[.3px] uppercase">
                Diagnóstico guiado
              </div>
              <div className="font-semibold text-[16px] leading-[1.38] text-[color:var(--text-primary)]">
                Isso acontece em qual conexão?
              </div>
              <div className="flex gap-2">
                <div className="flex-1 text-center rounded-[12px] p-3 bg-[color:var(--bg-primary)] font-normal text-[14px] leading-[1.43] text-[color:var(--text-primary)] cursor-pointer hover:brightness-95 transition-all">
                  Wi-Fi
                </div>
                <div className="flex-1 text-center rounded-[12px] p-3 bg-[color:var(--bg-primary)] font-normal text-[14px] leading-[1.43] text-[color:var(--text-primary)] cursor-pointer hover:brightness-95 transition-all">
                  Cabo de rede
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </PageShell>
  );
}
