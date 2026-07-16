import React from "react";
import { SectionGroupHeader } from "./SectionGroupHeader";
import { OverviewMetricGrid } from "./OverviewMetricGrid";
import { SessionsBarChart } from "./SessionsBarChart";
import { ScreenSessionsDonut } from "./ScreenSessionsDonut";
import { FirebaseCrashlyticsSummary } from "../../../integrations/firebase/firebase.types";
import { GooglePlayRatingSummary } from "../../../integrations/google-play/googlePlay.types";
import { ScreenNavigationMetric } from "../../../types/productAnalytics";

interface AppSectionProps {
  activeUsersToday: number | null;
  sessions7d: number | null;
  firebaseCrashlytics: FirebaseCrashlyticsSummary | null;
  playStoreRating: GooglePlayRatingSummary | null;
  timelineData: any[];
  screenNavigation: ScreenNavigationMetric[];
}

// Seção "App" (primária) do Centro de Controle — spec Lia,
// Md3DashboardContent.dc.html:18-59. Grid de 4 KPIs + row (gráfico de
// sessões 2fr | donut de sessões por tela 1fr). No mobile o gráfico de barras
// é omitido por decisão de layout do protótipo (Md3DashboardContentMobile.dc.html:34-50)
// — sobra só o card do donut, em largura cheia.
export const AppSection: React.FC<AppSectionProps> = ({
  activeUsersToday,
  sessions7d,
  firebaseCrashlytics,
  playStoreRating,
  timelineData,
  screenNavigation,
}) => {
  return (
    <div className="flex flex-col gap-3.5">
      <SectionGroupHeader label="App" dotColor="var(--primary)" id="overview-section-app" />

      <OverviewMetricGrid
        activeUsersToday={activeUsersToday}
        sessions7d={sessions7d}
        firebaseCrashlytics={firebaseCrashlytics}
        playStoreRating={playStoreRating}
      />

      <div className="grid grid-cols-1 lg:grid-cols-[2fr_1fr] gap-4">
        <div className="hidden lg:block">
          <SessionsBarChart timelineData={timelineData} />
        </div>
        <ScreenSessionsDonut screens={screenNavigation} />
      </div>
    </div>
  );
};
