import React from "react";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { CategoryTabs } from "../../components/ui/CategoryTabs";
import { ManagementSection } from "./components/ManagementSection";
import { RemoteConfigSection } from "./components/RemoteConfigSection";
import { AppCheckSection } from "./components/AppCheckSection";
import { AppDistributionSection } from "./components/AppDistributionSection";
import { FcmDeliverySection } from "./components/FcmDeliverySection";
import { AppEnvironment } from "../../types/admin";

type FirebaseCategory = "management" | "remote-config" | "app-check" | "app-distribution" | "fcm";

interface FirebasePageProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

/**
 * GH#1343/#1344 — plano de UX Google Play/Firebase (`docs/product/plano-ux-google-play-firebase.md`),
 * item 2 "Inventário técnico". Primeira fatia real da página Firebase: as 5 integrações que já
 * têm endpoint real em produção (PR #1346) — Management, Remote Config, App Check, App
 * Distribution, FCM delivery. Comportamento/Estabilidade/Performance/Mensageria (Analytics,
 * Crashlytics, Performance Monitoring, A/B Testing, In-App Messaging) seguem fora de escopo até
 * o contrato correspondente existir.
 */
export const FirebasePage: React.FC<FirebasePageProps> = ({ triggerRefreshCounter }) => {
  const [category, setCategory] = React.useState<FirebaseCategory>("management");

  return (
    <div className="space-y-6">
      <SectionIntro
        id="firebase-section-intro"
        overline="PLATAFORMAS · FIREBASE"
        question="Como o projeto Firebase do SignallQ está configurado?"
        description="Management, Remote Config, App Check, App Distribution e FCM — direto das APIs de administração do Google Cloud, via Admin Worker."
        source="FONTE · FIREBASE CONSOLE"
      />

      <CategoryTabs<FirebaseCategory>
        id="firebase-category-tabs"
        categories={[
          { key: "management", label: "Management" },
          { key: "remote-config", label: "Remote Config" },
          { key: "app-check", label: "App Check" },
          { key: "app-distribution", label: "App Distribution" },
          { key: "fcm", label: "FCM" },
        ]}
        active={category}
        onChange={setCategory}
      />

      {category === "management" && <ManagementSection triggerRefreshCounter={triggerRefreshCounter} />}
      {category === "remote-config" && <RemoteConfigSection triggerRefreshCounter={triggerRefreshCounter} />}
      {category === "app-check" && <AppCheckSection triggerRefreshCounter={triggerRefreshCounter} />}
      {category === "app-distribution" && <AppDistributionSection triggerRefreshCounter={triggerRefreshCounter} />}
      {category === "fcm" && <FcmDeliverySection triggerRefreshCounter={triggerRefreshCounter} />}
    </div>
  );
};
