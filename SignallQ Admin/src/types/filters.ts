/**
 * GH#552 (Fase 1) — contrato do componente de filtros globais reutilizado nas
 * 9 telas do redesenho (ver `docs_ai/_archive/2026-07-12_WIREFRAME_ADMIN_REDESIGN_552.md`).
 * Cada tela declara só os filtros que fazem sentido para sua pergunta-guia —
 * ex.: "Redes & Provedores" usa operator/region, "Releases & Qualidade" não usa
 * nenhum destes (usa seletor de versão em foco, fora deste contrato).
 */
export type GlobalFilterKey =
  | "period"
  | "version"
  | "os"
  | "networkType"
  | "operator"
  | "region"
  | "severity"
  | "provider"
  | "model"
  /** GH#552 (Fase 2) — categoria real de `SystemError.category` (app/backend/ia/integration).
   * Não usar "severity" para isso: o worker não classifica erros por severidade hoje. */
  | "category";

export interface GlobalFilterOption {
  label: string;
  value: string;
}

export interface GlobalFilterConfig {
  key: GlobalFilterKey;
  label: string;
  options: GlobalFilterOption[];
  value: string;
  onChange: (value: string) => void;
}
