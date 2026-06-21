import React from "react";
import { AiCostPage } from "./AiCostPage";
import { AppEnvironment } from "../../types/admin";

interface AiCostTabProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

export const AiCostTab: React.FC<AiCostTabProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  return (
    <AiCostPage
      environment={environment}
      period={period}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};

