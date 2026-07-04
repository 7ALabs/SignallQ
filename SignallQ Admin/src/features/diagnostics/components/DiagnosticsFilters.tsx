import React from "react";
import { Search, RefreshCw } from "lucide-react";
import { DistChannel, BuildType, DataPlatform } from "../../../types/diagnostics";
import { AppEnvironment } from "../../../types/admin";

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
  selectedVersion: string;
  onVersionChange: (version: string) => void;
  availableVersions: string[];
  selectedPeriod: string;
  onPeriodChange: (period: string) => void;
  selectedEnvironment: AppEnvironment;
  onEnvironmentChange: (env: AppEnvironment) => void;
  selectedDistChannel: DistChannel | "";
  onDistChannelChange: (channel: DistChannel | "") => void;
  selectedBuildType: BuildType | "";
  onBuildTypeChange: (type: BuildType | "") => void;
  selectedPlatform: DataPlatform | "";
  onPlatformChange: (platform: DataPlatform | "") => void;
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
  selectedVersion,
  onVersionChange,
  availableVersions,
  selectedPeriod,
  onPeriodChange,
  selectedEnvironment,
  onEnvironmentChange,
  selectedDistChannel,
  onDistChannelChange,
  selectedBuildType,
  onBuildTypeChange,
  selectedPlatform,
  onPlatformChange,
  onRefresh,
  isRefreshing = false,
}) => {
  return (
    <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[8px] p-5 space-y-4">
      {/* Prime row - search, refresh & environment */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        {/* Search Input bar */}
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--text-tertiary)]" />
          <input
            type="text"
            value={searchText}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Pesquise por dispositivo, ID da sessão, SSID ou log de erros..."
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl pl-10 pr-4 py-2.5 text-xs text-[var(--text-primary)] placeholder-[var(--text-tertiary)] focus:outline-none focus:border-[var(--primary)]/60 focus:ring-1 focus:ring-[var(--primary)]/30 transition-all font-sans"
          />
        </div>

        {/* Filters control triggers */}
        <div className="flex items-center gap-3 self-end md:self-auto">
          {/* Environment Switcher */}
          <div className="flex bg-[var(--bg-surface)] p-1 border border-[var(--border)] rounded-xl text-[10px] font-sans select-none">
            <button
              type="button"
              onClick={() => onEnvironmentChange("production")}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                selectedEnvironment === "production"
                  ? "bg-[var(--primary)] text-white shadow-sm"
                  : "text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
              }`}
            >
              PROD
            </button>
            <button
              type="button"
              onClick={() => onEnvironmentChange("staging")}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                selectedEnvironment === "staging"
                  ? "bg-[var(--attention)] text-black shadow-sm"
                  : "text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
              }`}
            >
              STAGING
            </button>
          </div>

          {/* Period Selector */}
          <select
            value={selectedPeriod}
            onChange={(e) => onPeriodChange(e.target.value)}
            className="bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-primary)] focus:outline-none focus:border-[var(--primary)] transition-colors cursor-pointer font-sans font-bold"
          >
            <option value="today">HOJE</option>
            <option value="7d">7 DIAS</option>
            <option value="30d">30 DIAS</option>
          </select>

          {/* Refresh button */}
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="flex items-center justify-center p-2.5 bg-[var(--bg-surface)] border border-[var(--border)] hover:border-zinc-700 active:bg-zinc-900 text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-all rounded-xl disabled:opacity-50 group"
            title="Sincronizar Telemetria"
          >
            <RefreshCw
              className={`w-4 h-4 ${isRefreshing ? "animate-spin text-[var(--primary)]" : "group-hover:rotate-45 transition-transform"}`}
            />
          </button>
        </div>
      </div>

      {/* Advanced filters selectors row */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-9 gap-3 pt-1 border-t border-[var(--border)]/40 select-none">
        {/* Network Selection */}
        <div className="space-y-1">
          <label className="text-[10px] font-sans uppercase tracking-wider text-[var(--text-tertiary)] font-bold block">
            Tipo de Rede
          </label>
          <select
            value={selectedNetwork}
            onChange={(e) => onNetworkChange(e.target.value)}
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-secondary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer"
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
          <label className="text-[10px] font-sans uppercase tracking-wider text-[var(--text-tertiary)] font-bold block">
            Operadora
          </label>
          <select
            value={selectedOperator}
            onChange={(e) => onOperatorChange(e.target.value)}
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-secondary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer"
          >
            <option value="all">Filtro de Opera.</option>
            <option value="Vivo">Vivo</option>
            <option value="Claro">Claro</option>
            <option value="TIM">TIM</option>
          </select>
        </div>

        {/* Score Threshold */}
        <div className="space-y-1">
          <label className="text-[10px] font-sans uppercase tracking-wider text-[var(--text-tertiary)] font-bold block">
            Score da Rede
          </label>
          <select
            value={selectedScore}
            onChange={(e) => onScoreChange(e.target.value)}
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-secondary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer"
          >
            <option value="all">Score (Filtro)</option>
            <option value="poor">Crítico (&lt; 60)</option>
            <option value="medium">Instável (60 - 80)</option>
            <option value="excellent">Excelente (&gt; 80)</option>
          </select>
        </div>

        {/* Identified Issue */}
        <div className="space-y-1">
          <label className="text-[10px] font-sans uppercase tracking-wider text-[var(--text-tertiary)] font-bold block">
            Problema Mapeado
          </label>
          <select
            value={selectedIssue}
            onChange={(e) => onIssueChange(e.target.value)}
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-secondary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer"
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

        {/* Origem do dado (GH#442) */}
        <div className="space-y-1">
          <label className="text-[10px] font-sans uppercase tracking-wider text-[var(--text-tertiary)] font-bold block">
            Origem
          </label>
          <select
            value={selectedPlatform}
            onChange={(e) => onPlatformChange(e.target.value as DataPlatform | "")}
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-secondary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer"
          >
            <option value="">Todas</option>
            <option value="android">Android</option>
            <option value="web">WebApp</option>
          </select>
        </div>

        {/* Distribution Channel */}
        <div className="space-y-1">
          <label className="text-[10px] font-sans uppercase tracking-wider text-[var(--text-tertiary)] font-bold block">
            Canal de Dist.
          </label>
          <select
            value={selectedDistChannel}
            onChange={(e) => onDistChannelChange(e.target.value as DistChannel | "")}
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-secondary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer"
          >
            <option value="">Todos</option>
            <option value="firebase_app_distribution">Firebase App Distribution</option>
            <option value="sideload">APK / Sideload</option>
            <option value="play_store">Play Store</option>
          </select>
        </div>

        {/* Build Type */}
        <div className="space-y-1">
          <label className="text-[10px] font-sans uppercase tracking-wider text-[var(--text-tertiary)] font-bold block">
            Tipo de Build
          </label>
          <select
            value={selectedBuildType}
            onChange={(e) => onBuildTypeChange(e.target.value as BuildType | "")}
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-secondary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer"
          >
            <option value="">Todos</option>
            <option value="release">Release</option>
            <option value="debug">Debug</option>
          </select>
        </div>

        {/* App Version */}
        <div className="space-y-1 col-span-2 sm:col-span-1 lg:col-span-2">
          <label className="text-[10px] font-sans uppercase tracking-wider text-[var(--text-tertiary)] font-bold block">
            Versão do App
          </label>
          <select
            value={selectedVersion}
            onChange={(e) => onVersionChange(e.target.value)}
            className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-secondary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer"
          >
            <option value="all">Todas as versões</option>
            {availableVersions.map((v) => (
              <option key={v} value={v}>{v}</option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
};
