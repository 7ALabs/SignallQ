import React from "react";
import { DiagnosticDivergencesPage } from "./DiagnosticDivergencesPage";

// Refs #1446 (shadow mode) — mesmo padrão fino de DiagnosticRulesetsTab: App.tsx só
// conhece o Tab, a Page concentra estado/composição real da tela.
export const DiagnosticDivergencesTab: React.FC = () => {
  return <DiagnosticDivergencesPage />;
};
