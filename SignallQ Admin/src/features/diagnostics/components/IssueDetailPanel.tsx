import React from "react";
import { Terminal, Lightbulb, AlertOctagon, HelpCircle, ArrowRight, ShieldAlert, Cpu } from "lucide-react";

interface IssueDetailPanelProps {
  selectedIssueName: string | null;
  onClear: () => void;
}

interface IssueDetail {
  title: string;
  probability: string;
  technicalCause: string;
  impactMetrics: { key: string; val: string }[];
  cloudflareEdgeWorkflow: string;
  remediationRecipeAndroid: string;
}

const issueDetailsMap: Record<string, IssueDetail> = {
  "Wi-Fi fraco": {
    title: "Wi-Fi Fraco (Atenuação Local RF)",
    probability: "31% dos relatos",
    technicalCause: "Forte incidência de reflexão de ondas eletromagnéticas nas bandas de 2.4/5.0GHz provocada por barreiras de alvenaria. Alta atenuação de sinal (RSSI abaixo de -80 dBm).",
    impactMetrics: [
      { key: "Aumento de Jitter", val: "+14ms" },
      { key: "Perda de pacotes", val: "0.8% - 2.5%" },
      { key: "Queda de Througput", val: "Aprox. -65%" }
    ],
    cloudflareEdgeWorkflow: "Roteador do API Gateway intercepta solicitações em conformidade com o SSID e envia gatilho push de otimização de banda de rádio.",
    remediationRecipeAndroid: "Ativar algoritmo 'SmartWiFi' no SDK móvel para forçar a desconexão automática e migração imediata para canais 5GHz com menor atenuação."
  },
  "Bufferbloat upload": {
    title: "Bufferbloat de Upload (Saturação de Buffer local)",
    probability: "18% de degradação",
    technicalCause: "Saturação das filas FIFO nas interfaces de rede do roteador de borda do usuário resultando em atraso exacerbado quando o upload é forçado à taxa máxima.",
    impactMetrics: [
      { key: "Latência sob estresse", val: "+180ms" },
      { key: "Grade de Conexão", val: "F" },
      { key: "Estabilidade", val: "Instável" }
    ],
    cloudflareEdgeWorkflow: "Detecção síncrona nos tempos de requisições de analytics e rotas auxiliares. Identificação de jitter sob carga.",
    remediationRecipeAndroid: "Implementar algoritmo SQM (Smart Queue Management) baseado em fq_codel no roteador ou escalonar downloads/uploads no app móvel."
  },
  "DNS lento": {
    title: "Latência Elevada de DNS (Servidores Desajustados)",
    probability: "14% dos gargalos",
    technicalCause: "Uso de servidores de DNS locais de operadoras com capacidade de processamento saturada de recursivos ou alta latência de resolução de nome.",
    impactMetrics: [
      { key: "Tempo de resolução", val: ">150ms" },
      { key: "Atraso de handshake", val: "+80ms" },
      { key: "Timeouts", val: "Média de 1.2%" }
    ],
    cloudflareEdgeWorkflow: "Laudo de IA envia automaticamente sugestão em formato JSON para que o aplicativo passe a ignorar o DNS padrão da rede local.",
    remediationRecipeAndroid: "Substituir endpoints DNS de rádio de forma determinística por provedores Anycast públicos e resilientes (Cloudflare 1.1.1.1 ou Google 8.8.8.8)."
  },
  "Rede móvel congestionada": {
    title: "Congestionamento de Estação de Rádio Base celular (ERB)",
    probability: "11% de picos",
    technicalCause: "Alta densidade de usuários ativos conectados à mesma portadora/célula LTE/5G (congestionamento físico do canal do espectro de radiofrequência).",
    impactMetrics: [
      { key: "Sinal de portadora", val: "-108 dBm" },
      { key: "Velocidade Média celular", val: "5-15 Mbps" },
      { key: "Jitter celular", val: "18-28 ms" }
    ],
    cloudflareEdgeWorkflow: "Armazenamento do dump em tabela de telemetria, sinalizando áreas e coordenadas geográficas de antenas sobrecarregadas.",
    remediationRecipeAndroid: "Utilizar API móvel cellular para tentar forçar handover ou alertar o usuário para alternar para a rede Wi-Fi."
  },
  "Gateway lento": {
    title: "Latência Elevada no Primeiro Salto (Gateway Local Lento)",
    probability: "7% das ocorrências",
    technicalCause: "Alto tempo de processamento ou congestionamento de rádio entre o dispositivo e o endereço IP do gateway padrão da rede doméstica.",
    impactMetrics: [
      { key: "Ping até Gateway", val: ">45ms" },
      { key: "Packet loss local", val: "0.2%" },
      { key: "Score afetado", val: "69/100" }
    ],
    cloudflareEdgeWorkflow: "Sinalização síncrona de bypass de rede local para evitar gargalos entre o Worker e o dispositivo.",
    remediationRecipeAndroid: "Sinalizar reinício inteligente da interface Wi-Fi móvel ou recomendar a readequação física do roteador doméstico do usuário."
  }
};

export const IssueDetailPanel: React.FC<IssueDetailPanelProps> = ({ selectedIssueName, onClear }) => {
  const defaultIssueName = selectedIssueName || "Wi-Fi fraco";
  const detail = issueDetailsMap[defaultIssueName] || issueDetailsMap["Wi-Fi fraco"];

  return (
    <div className="bg-[#111111] border border-[#262626] rounded-2xl p-5 shadow-sm h-full flex flex-col justify-between">
      <div>
        <div className="flex items-center justify-between pb-4 border-b border-[#262626] mb-5 select-none">
          <div className="flex items-center gap-2">
            <span className="p-1 px-2 rounded-md bg-[#FF4D4F]/10 border border-[#FF4D4F]/20 text-[#FF4D4F] font-mono text-[10px] uppercase font-bold">
              Diagnóstico Fatores
            </span>
            <h4 className="text-xs font-semibold font-mono uppercase tracking-wider text-zinc-400">
              Escrutínio Operacional
            </h4>
          </div>
          {selectedIssueName && (
            <button
              onClick={onClear}
              className="text-[10px] text-zinc-500 hover:text-white uppercase transition-colors font-mono"
            >
              Limpar seleção [x]
            </button>
          )}
        </div>

        <div className="space-y-4 font-sans text-xs">
          {/* Issue Title Box */}
          <div className="p-3 bg-[#18181B] border border-[#262626] rounded-xl relative overflow-hidden select-none">
            <div className="absolute top-0 right-0 w-24 h-24 bg-red-500/5 rounded-full filter blur-xl pointer-events-none" />
            <div className="text-[10px] text-[#FF4D4F] font-mono uppercase font-bold">ALERTA OPERACIONAL AUTOMÁTICO</div>
            <h5 className="font-semibold text-white text-sm font-sans mt-0.5">{detail.title}</h5>
            <span className="text-[10px] font-mono text-zinc-400 block mt-1">Impacto: {detail.probability}</span>
          </div>

          {/* Root cause */}
          <div className="space-y-1 select-none">
            <div className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-zinc-500" />
              <span>Causa Raiz Física Identificada</span>
            </div>
            <p className="text-zinc-350 leading-relaxed text-[11px] font-sans">
              {detail.technicalCause}
            </p>
          </div>

          {/* Impact table metrics */}
          <div className="space-y-2 select-none">
            <div className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider text-zinc-500 font-bold">
              Impacto Direto de Desempenho
            </div>
            <div className="grid grid-cols-3 gap-2.5">
              {detail.impactMetrics.map((met, idx) => (
                <div key={idx} className="bg-[#161619] border border-[#2d2d31]/50 p-2.5 rounded-lg text-center">
                  <span className="text-[9px] text-zinc-500 font-sans block truncate">{met.key}</span>
                  <span className="text-xs font-bold font-mono text-[#FF4D4F] block mt-0.5">{met.val}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Edge worker workflow */}
          <div className="space-y-1">
            <div className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1.5 select-none">
              <Cpu className="w-3.5 h-3.5 text-purple-400" />
              <span>Rotina do Gateway (Cloudflare Edge)</span>
            </div>
            <div className="p-3 bg-[#0a0a0c] border border-[#262626] rounded-xl font-mono text-[10px] text-zinc-400 leading-relaxed max-h-24 overflow-y-auto">
              {detail.cloudflareEdgeWorkflow}
            </div>
          </div>

          {/* SDK mitigation recipe */}
          <div className="space-y-1">
            <div className="text-[10px] text-zinc-550 font-mono uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1.5 select-none">
              <Terminal className="w-3.5 h-3.5 text-[#22C55E]" />
              <span>Diretriz de Mitigação (App Android)</span>
            </div>
            <div className="p-3 bg-[#0a0a0c] border border-dashed border-[#22C55E]/20 text-[#22C55E] rounded-xl font-sans text-[11px] leading-relaxed">
              {detail.remediationRecipeAndroid}
            </div>
          </div>
        </div>
      </div>

      <div className="mt-5 pt-4 border-t border-[#262626] flex items-center justify-between text-[10px] font-mono text-zinc-540 select-none">
        <span>Sincronizado via laudos Gemini</span>
        <span className="flex items-center gap-1 text-purple-400 font-semibold cursor-pointer hover:underline">
          Abrir documentação <ArrowRight className="w-3 h-3" />
        </span>
      </div>
    </div>
  );
};
