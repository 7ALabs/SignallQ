import React from "react";
import { Search, RefreshCw } from "lucide-react";
import { DistChannel, BuildType } from "../../../types/diagnostics";

interface DiagnosticsFiltersProps {
  searchText: string;
  onSearchChange: (text: string) => void;
  selectedNetwork: string;
  onNetworkChange: (net: string) => void;
  selectedOperator: string;
  onOperatorChange: (op: string) => void;
  selectedScore: string;
  onScoreChange: (score: string) => void;
  selectedIssue: string;
  onIssueChange: (issue: string) => void;
  selectedAiProvider: string;
  onAiProviderChange: (provider: string) => void;
  selectedPeriod: string;
  onPeriodChange: (period: string) => void;
  selectedEnvironment: "production" | "staging";
  onEnvironmentChange: (env: "production" | "staging") => void;
  selectedDistChannel: DistChannel | "";
  onDistChannelChange: (channel: DistChannel | "") => void;
  selectedBuildType: BuildType | "";
  onBuildTypeChange: (type: BuildType | "") => void;
  onRefresh: () => void;
  isRefreshing?: boolean;
}

export const DiagnosticsFilters: React.FC<DiagnosticsFiltersProps> = ({
  searchText,
  onSearchChange,
  selectedNetwork,
  onNetworkChange,
  selectedOperator,
  onOperatorChange,
  selectedScore,
  onScoreChange,
  selectedIssue,
  onIssueChange,
  selectedAiProvider,
  onAiProviderChange,
  selectedPeriod,
  onPeriodChange,
  selectedEnvironment,
  onEnvironmentChange,
  selectedDistChannel,
  onDistChannelChange,
  selectedBuildType,
  onBuildTypeChange,
  onRefresh,
  isRefreshing = false,
}) => {
  return (
    <div className="bg-[#111111] border border-[#262626] rounded-2xl p-5 space-y-4 shadow-sm">
      {/* Prime row - search, refresh & environment */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        {/* Search Input bar */}
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
          <input
            type="text"
            value={searchText}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Pesquise por dispositivo, ID da sessão, SSID ou log de erros..."
            className="w-full bg-[#18181B] border border-[#262626] rounded-xl pl-10 pr-4 py-2.5 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-[#6C2BFF]/60 focus:ring-1 focus:ring-[#6C2BFF]/30 transition-all font-sans"
          />
        </div>

        {/* Filters control triggers */}
        <div className="flex items-center gap-3 self-end md:self-auto">
          {/* Environment Switcher */}
          <div className="flex bg-[#18181B] p-1 border border-[#262626] rounded-xl text-[10px] font-mono select-none">
            <button
              type="button"
              onClick={() => onEnvironmentChange("production")}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                selectedEnvironment === "production"
                  ? "bg-[#6C2BFF] text-white shadow-sm"
                  : "text-zinc-400 hover:text-white"
              }`}
            >
              PROD
            </button>
            <button
              type="button"
              onClick={() => onEnvironmentChange("staging")}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                selectedEnvironment === "staging"
                  ? "bg-[#Eab308] text-black shadow-sm"
                  : "text-zinc-400 hover:text-white"
              }`}
            >
              STAGING
            </button>
          </div>

          {/* Period Selector */}
          <select
            value={selectedPeriod}
            onChange={(e) => onPeriodChange(e.target.value)}
            className="bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-[#6C2BFF] transition-colors cursor-pointer font-mono font-bold"
          >
            <option value="today">HOJE</option>
            <option value="7d">7 DIAS</option>
            <option value="30d">30 DIAS</option>
          </select>

          {/* Refresh button */}
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="flex items-center justify-center p-2.5 bg-[#18181B] border border-[#262626] hover:border-zinc-700 active:bg-zinc-900 text-zinc-400 hover:text-white transition-all rounded-xl disabled:opacity-50 group"
            title="Sincronizar Telemetria"
          >
            <RefreshCw
              className={`w-4 h-4 ${isRefreshing ? "animate-spin text-[#6C2BFF]" : "group-hover:rotate-45 transition-transform"}`}
            />
          </button>
        </div>
      </div>

      {/* Advanced filters selectors row */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-8 gap-3 pt-1 border-t border-[#262626]/40 select-none">
        {/* Network Selection */}
        <div className="space-y-1">
          <label className="text-[10px] font-mono uppercase tracking-wider text-zinc-500 font-bold block">
            Tipo de Rede
          </label>
          <select
            value={selectedNetwork}
            onChange={(e) => onNetworkChange(e.target.value)}
            className="w-full bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-zinc-350 focus:outline-none focus:border-[#6C2BFF] cursor-pointer"
          >
            <option value="all">Sinal (Todos)</option>
            <option value="wifi">Wi-Fi</option>
            <option value="mobile">Rede móvel (Celular)</option>
            <option value="fiber">Fibra / Banda banda larga</option>
            <option value="ethernet">Ethernet</option>
          </select>
        </div>

        {/* Carrier Selection */}
        <div className="space-y-1">
          <label className="text-[10px] font-mono uppercase tracking-wider text-zinc-500 font-bold block">
            Operadora
          </label>
          <select
            value={selectedOperator}
            onChange={(e) => onOperatorChange(e.target.value)}
            className="w-full bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-zinc-350 focus:outline-none focus:border-[#6C2BFF] cursor-pointer"
          >
            <option value="all">Filtro de Opera.</option>
            <option value="Vivo">Vivo</option>
            <option value="Claro">Claro</option>
            <option value="TIM">TIM</option>
          </select>
        </div>

        {/* Score Threshold */}
        <div className="space-y-1">
          <label className="text-[10px] font-mono uppercase tracking-wider text-zinc-500 font-bold block">
            Score da Rede
          </label>
          <select
            value={selectedScore}
            onChange={(e) => onScoreChange(e.target.value)}
            className="w-full bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-zinc-350 focus:outline-none focus:border-[#6C2BFF] cursor-pointer"
          >
            <option value="all">Score (Filtro)</option>
            <option value="poor">Crítico (&lt; 60)</option>
            <option value="medium">Instável (60 - 80)</option>
            <option value="excellent">Excelente (&gt; 80)</option>
          </select>
        </div>

        {/* Identified Issue */}
        <div className="space-y-1">
          <label className="text-[10px] font-mono uppercase tracking-wider text-zinc-500 font-bold block">
            Problema Mapeado
          </label>
          <select
            value={selectedIssue}
            onChange={(e) => onIssueChange(e.target.value)}
            className="w-full bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-zinc-350 focus:outline-none focus:border-[#6C2BFF] cursor-pointer"
          >
            <option value="all">Sem problemas</option>
            <option value="wifi_signal_weak">Wi-Fi fraco</option>
            <option value="bufferbloat_upload">Bufferbloat upload</option>
            <option value="dns_latency_high">DNS lento</option>
            <option value="mobile_congestion_suspected">Rede móvel congestionada</option>
            <option value="gateway_slow">Gateway lento</option>
            <option value="upload_bottleneck">Upload bottleneck</option>
          </select>
        </div>

        {/* Distribution Channel */}
        <div className="space-y-1">
          <label className="text-[10px] font-mono uppercase tracking-wider text-zinc-500 font-bold block">
            Canal de Dist.
          </label>
          <select
            value={selectedDistChannel}
            onChange={(e) => onDistChannelChange(e.target.value as DistChannel | "")}
            className="w-full bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-zinc-350 focus:outline-none focus:border-[#6C2BFF] cursor-pointer"
          >
            <option value="">Todos</option>
            <option value="firebase_app_distribution">Firebase App Distribution</option>
            <option value="sideload">APK / Sideload</option>
            <option value="play_store">Play Store</option>
          </select>
        </div>

        {/* Build Type */}
        <div className="space-y-1">
          <label className="text-[10px] font-mono uppercase tracking-wider text-zinc-500 font-bold block">
            Tipo de Build
          </label>
          <select
            value={selectedBuildType}
            onChange={(e) => onBuildTypeChange(e.target.value as BuildType | "")}
            className="w-full bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-zinc-350 focus:outline-none focus:border-[#6C2BFF] cursor-pointer"
          >
            <option value="">Todos</option>
            <option value="release">Release</option>
            <option value="debug">Debug</option>
          </select>
        </div>

        {/* AI Provider */}
        <div className="space-y-1 col-span-2 sm:col-span-1 lg:col-span-2">
          <label className="text-[10px] font-mono uppercase tracking-wider text-zinc-500 font-bold block">
            Provedor IA Laudos
          </label>
          <select
            value={selectedAiProvider}
            onChange={(e) => onAiProviderChange(e.target.value)}
            className="w-full bg-[#18181B] border border-[#262626] rounded-xl px-3 py-2 text-xs text-zinc-350 focus:outline-none focus:border-[#6C2BFF] cursor-pointer"
          >
            <option value="all">Todos os provedores</option>
            <option value="gemini_flash">Gemini 1.5 Flash</option>
            <option value="cloudflare_qwen">Qwen 2.5 Edge</option>
            <option value="openai">OpenAI GPT-4o Mini</option>
            <option value="local_fallback">Fallback local</option>
          </select>
        </div>
      </div>
    </div>
  );
};
