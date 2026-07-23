// Resultado da consulta ao GenieACS (issue #66) — sempre retornado pelo adapter, nunca lança.
// `fibra_comprometida` já vem calculado no backend (threshold do tenant aplicado ali), para o
// frontend não precisar duplicar a lógica de limiar.
export type GenieACSResult =
  | { available: false }
  | {
      available: true;
      rx_power_dbm: number | null;
      wan_status: 'connected' | 'disconnected' | 'unknown';
      uptime_s: number | null;
      timestamp: string;
      data_model: 'tr098' | 'tr181';
      fibra_comprometida: boolean;
    };
