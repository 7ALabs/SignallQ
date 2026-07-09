import React from "react";
import { FeatureFlagsSummaryCard } from "./components/FeatureFlagsSummaryCard";
import { TeamAccessCard } from "./components/TeamAccessCard";
import { SectionCard } from "../../components/ui/SectionCard";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { SectionIntro } from "../../components/ui/SectionIntro";

export const SettingsPage: React.FC = () => {
  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz (sem linha de fonte,
          igual ao mockup: Configurações não lista proveniência de dado). */}
      <SectionIntro
        id="settings-section-intro"
        overline="CONFIGURAÇÕES"
        question="Configurações do painel"
        description="Feature flags e acesso da equipe."
      />

      {/* Grid 2 colunas — paridade com sec-settings do mockup: Feature Flags + Acesso da Equipe */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <FeatureFlagsSummaryCard />
        <TeamAccessCard />
      </div>

      {/* Bloco: Exportações — GH#781 (ajuste fino 3, paridade mockup sec-settings).
          O worker não expõe hoje uma rota de snapshot/export do painel (CSV/PDF
          agregando as telas para prestação de contas) — estado vazio explícito
          em vez de simular um botão funcional sem endpoint por trás. */}
      <SectionCard
        title="Exportações"
        description="Baixe um snapshot deste painel para prestação de contas a parceiros."
        id="settings-exports-block"
      >
        <FeatureComingSoon
          feature="Exportar CSV / PDF do painel"
          reason="Requer rota de snapshot agregado no worker (hoje só existem exports CSV pontuais por tela, ex.: Custo de IA e Diagnósticos)"
          compact
        />
      </SectionCard>
    </div>
  );
};
