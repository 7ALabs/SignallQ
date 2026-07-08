import React from "react";
import { OverviewPage } from "./OverviewPage";
import { AppEnvironment } from "../../types/admin";

interface OverviewTabProps {
  environment: AppEnvironment;
  period: string;
  onPeriodChange: (p: string) => void;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

export const OverviewTab: React.FC<OverviewTabProps> = ({
  environment,
  period,
  onPeriodChange,
  onNavigate,
  triggerRefreshCounter,
}) => {
  return (
    <OverviewPage
      environment={environment}
      period={period}
      onPeriodChange={onPeriodChange}
      onNavigate={onNavigate}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
