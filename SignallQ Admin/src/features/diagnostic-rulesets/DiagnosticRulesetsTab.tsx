import React from "react";
import { DiagnosticRulesetsPage } from "./DiagnosticRulesetsPage";

// Refs #1446 — wrapper fino, mesmo padrão de DiagnosticsTab/NetworksTab: App.tsx
// só conhece o Tab, a Page concentra o estado/composição real da tela.
export const DiagnosticRulesetsTab: React.FC = () => {
  return <DiagnosticRulesetsPage />;
};
