import React from "react";
import { ErrorsPage } from "./ErrorsPage";
import { AppEnvironment } from "../../types/admin";

interface ErrorsTabProps {
  environment: AppEnvironment;
  period: string;
  onEnvironmentChange: (env: AppEnvironment) => void;
  onPeriodChange: (p: string) => void;
  triggerRefreshCounter: number;
}

export const ErrorsTab: React.FC<ErrorsTabProps> = ({
  environment,
  period,
  onEnvironmentChange,
  onPeriodChange,
  triggerRefreshCounter,
}) => {
  return (
    <ErrorsPage
      environment={environment}
      period={period}
      onEnvironmentChange={onEnvironmentChange}
      onPeriodChange={onPeriodChange}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
