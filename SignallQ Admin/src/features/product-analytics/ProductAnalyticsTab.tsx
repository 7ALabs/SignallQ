import React from "react";
import { ProductAnalyticsPage } from "./ProductAnalyticsPage";
import { AppEnvironment } from "../../types/admin";

interface ProductAnalyticsTabProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

export const ProductAnalyticsTab: React.FC<ProductAnalyticsTabProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  return (
    <ProductAnalyticsPage
      environment={environment}
      period={period}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
