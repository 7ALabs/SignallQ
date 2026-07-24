import React from "react";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { CategoryTabs } from "../../components/ui/CategoryTabs";
import { QualitySection } from "./components/QualitySection";
import { ReviewsSection } from "./components/ReviewsSection";
import { AppEnvironment } from "../../types/admin";

type GooglePlayCategory = "qualidade" | "avaliacoes";

interface GooglePlayPageProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

/**
 * GH#1341/#1342 — plano de UX Google Play/Firebase (`docs/product/plano-ux-google-play-firebase.md`),
 * itens 2.1 (hierarquia), 2.2 (TermHint), 2.3 (Avaliações). Escopo desta rodada: só as duas
 * categorias com endpoint real em produção (Qualidade/Vitals e Avaliações) — Distribuição,
 * Aquisição, Monetização e Finanças ficam para quando os contratos do Camilo existirem.
 */
export const GooglePlayPage: React.FC<GooglePlayPageProps> = ({ triggerRefreshCounter }) => {
  const [category, setCategory] = React.useState<GooglePlayCategory>("qualidade");

  return (
    <div className="space-y-6">
      <SectionIntro
        id="google-play-section-intro"
        overline="PLATAFORMAS · GOOGLE PLAY"
        question="Como o app está indo na Google Play?"
        description="Qualidade (Android Vitals) e Avaliações — direto da Android Publisher API e da Play Developer Reporting API, via Admin Worker."
        source="FONTE · GOOGLE PLAY CONSOLE"
      />

      <CategoryTabs<GooglePlayCategory>
        id="google-play-category-tabs"
        categories={[
          { key: "qualidade", label: "Qualidade" },
          { key: "avaliacoes", label: "Avaliações" },
        ]}
        active={category}
        onChange={setCategory}
      />

      {category === "qualidade" && <QualitySection triggerRefreshCounter={triggerRefreshCounter} />}
      {category === "avaliacoes" && <ReviewsSection triggerRefreshCounter={triggerRefreshCounter} />}
    </div>
  );
};
