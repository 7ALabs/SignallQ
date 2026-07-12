import React from "react";
import { NetworksOperatorsPage } from "./NetworksOperatorsPage";
import { AppEnvironment } from "../../types/admin";

interface NetworksTabProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

// GH#552 (Fase 2) — "Redes & Provedores": fusão de `networks/` + `operators/`
// (ver docs_ai/_archive/2026-07-12_WIREFRAME_ADMIN_REDESIGN_552.md). Rota `/operators`
// aponta pro mesmo componente em App.tsx até que nada mais linke pro slug antigo.
export const NetworksTab: React.FC<NetworksTabProps> = ({
  environment,
  period,
  onNavigate,
  triggerRefreshCounter,
}) => {
  return (
    <NetworksOperatorsPage
      environment={environment}
      period={period}
      onNavigate={onNavigate}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
