import React, { useCallback, useEffect, useState } from "react";
import { SystemHealthPage } from "./SystemHealthPage";
import { AppEnvironment } from "../../types/admin";

interface SystemHealthTabProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter?: number;
}

export const SystemHealthTab: React.FC<SystemHealthTabProps> = ({
  environment,
  period,
  triggerRefreshCounter = 0,
}) => {
  return (
    <SystemHealthPage
      environment={environment}
      period={period}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
