import React from "react";
import { ExtendedSettingsPayload } from "../../../services/adminSettingsService";
import { SectionCard } from "../../../components/ui/SectionCard";
import { Cpu, Terminal } from "lucide-react";

interface DiagnosticPipelineSettingsProps {
  settings: ExtendedSettingsPayload;
  onChange: (updates: Partial<ExtendedSettingsPayload>) => void;
}

export const DiagnosticPipelineSettings: React.FC<DiagnosticPipelineSettingsProps> = ({ settings, onChange }) => {
  return (
    <SectionCard
      title="Pipeline de Coleta de Diagnósticos"
      description="Políticas de cronograma para os gatilhos silenciosos na camada móvel Android e roteamento central."
      id="diagnostic-pipeline-settings"
    >
      <div className="space-y-4 font-sans text-xs">
        {/* Interval between tests */}
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-zinc-400 font-medium block">Intervalo de Varredura Silenciosa (Segundos)</label>
            <input
              type="number"
              value={settings.speedtestIntervalSeconds}
              onChange={(e) => onChange({ speedtestIntervalSeconds: parseInt(e.target.value) || 300 })}
              className="w-full bg-[#161619] border border-[#262626] rounded-xl px-3.5 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
              placeholder="e.g. 300"
            />
            <span className="text-[10px] text-zinc-550 block font-sans">
              Com que frequência o SDK Android executa silenciosamente diagnósticos de latência / download se o sinal cair.
            </span>
          </div>

          <div className="space-y-1.5">
            <label className="text-zinc-400 font-medium block">Limite Diário de Consumo de Rede para SpeedTest (MB)</label>
            <input
              type="number"
              value={settings.maxSpeedTestDataDailyMb ?? 250}
              onChange={(e) => onChange({ maxSpeedTestDataDailyMb: parseInt(e.target.value) || 250 })}
              className="w-full bg-[#161619] border border-[#262626] rounded-xl px-3.5 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
              placeholder="e.g. 250"
            />
            <span className="text-[10px] text-zinc-550 block font-sans">
              Teto diário de dados móveis que o app pode consumir realizando testes de velocidade no background para evitar estouro da franquia celular do usuário.
            </span>
          </div>
        </div>

        {/* Cloudflare Worker Endpoint */}
        <div className="space-y-1.5">
          <label className="text-zinc-400 font-medium block">Endpoint Serverless Central (Edge Worker)</label>
          <input
            type="text"
            value={settings.cloudflareWorkerEndpoint}
            onChange={(e) => onChange({ cloudflareWorkerEndpoint: e.target.value })}
            className="w-full bg-[#161619] border border-[#262626] rounded-xl px-3.5 py-2.5 text-white font-mono text-xs focus:outline-none focus:border-purple-500 transition-colors"
            placeholder="https://example.workers.dev"
          />
          <span className="text-[10px] text-zinc-550 block font-sans">
            Para onde as interfaces móveis enviam os pacotes estruturados JSON de telemetria de conectividade.
          </span>
        </div>

        {/* Flag for collecting console logs checkbox */}
        <div className="flex items-start gap-3 p-3.5 bg-zinc-950 border border-[#232326] rounded-xl select-none">
          <input
            type="checkbox"
            id="androidLogsCollectionEnabled"
            checked={settings.androidLogsCollectionEnabled}
            onChange={(e) => onChange({ androidLogsCollectionEnabled: e.target.checked })}
            className="mt-0.5 rounded text-purple-600 bg-[#161619] border-[#262626] focus:ring-purple-500 cursor-pointer h-4 w-4"
          />
          <div>
            <label htmlFor="androidLogsCollectionEnabled" className="text-white font-semibold cursor-pointer block select-none">
              Coletar Erros e Dumps do Console Android
            </label>
            <span className="text-[10.5px] text-zinc-400 block mt-0.5 leading-snug font-sans">
              Anexa os últimos 50 logs de verbose/warning de rádio do Logcat ao arquivo JSON enviado aos Workers caso ocorra crash.
            </span>
          </div>
        </div>
      </div>
    </SectionCard>
  );
};
