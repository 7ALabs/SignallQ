import React from "react";
import { DiagnosticsPage } from "./DiagnosticsPage";
import { AppEnvironment } from "../../types/admin";

interface DiagnosticsTabProps {
  environment: AppEnvironment;
  period: string;
  onEnvironmentChange: (env: AppEnvironment) => void;
  onPeriodChange: (p: string) => void;
  triggerRefreshCounter: number;
  onNavigate?: (path: string) => void;
}

export const DiagnosticsTab: React.FC<DiagnosticsTabProps> = ({
  environment,
  period,
  onEnvironmentChange,
  onPeriodChange,
  triggerRefreshCounter,
  onNavigate,
}) => {
  return (
    <DiagnosticsPage
      environment={environment}
      period={period}
      triggerRefreshCounter={triggerRefreshCounter}
      onNavigate={onNavigate}
    />
  );
};
