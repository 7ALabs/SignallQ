import React from "react";
import { OverviewPage } from "./OverviewPage";
import { AppEnvironment } from "../../types/admin";

interface OverviewTabProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

export const OverviewTab: React.FC<OverviewTabProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  return (
    <OverviewPage
      environment={environment}
      period={period}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
