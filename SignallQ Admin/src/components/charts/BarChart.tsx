import React from "react";
import { alpha } from "../../utils/color";
import {
  BarChart as ReBarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

interface BarChartProps {
  data: any[];
  xAxisKey: string;
  series: {
    key: string;
    name: string;
    color: string;
  }[];
  height?: number;
  id?: string;
}

export const BarChart: React.FC<BarChartProps> = ({
  data,
  xAxisKey,
  series,
  height = 240,
  id,
}) => {
  const [isMounted, setIsMounted] = React.useState(false);

  React.useEffect(() => {
    setIsMounted(true);
  }, []);

  if (!isMounted) {
    return (
      <div
        style={{ height, backgroundColor: alpha("var(--sq-bg-primary)", 20) }}
        className="w-full flex items-center justify-center rounded-xl animate-pulse"
      >
        <span
          className="text-[10px] font-mono tracking-widest uppercase"
          style={{ color: "var(--sq-text-tertiary)" }}
        >
          Processando Grid...
        </span>
      </div>
    );
  }

  return (
    <div id={id} style={{ width: "100%", height }}>
      <ResponsiveContainer width="100%" height="100%">
        <ReBarChart
          data={data}
          margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="var(--sq-border-subtle)" />
          <XAxis
            dataKey={xAxisKey}
            stroke="var(--sq-text-tertiary)"
            fontSize={11}
            fontFamily="var(--sq-font-sans)"
            tickLine={false}
            axisLine={false}
            dy={8}
          />
          <YAxis
            stroke="var(--sq-text-tertiary)"
            fontSize={11}
            fontFamily="var(--sq-font-sans)"
            tickLine={false}
            axisLine={false}
            dx={-8}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: "var(--sq-bg-card)",
              border: "1px solid var(--sq-border)",
              borderRadius: "10px",
              fontFamily: "var(--sq-font-sans)",
              fontSize: "12px",
              color: "var(--sq-text-primary)",
            }}
            itemStyle={{ color: "var(--sq-text-primary)", fontFamily: "var(--sq-font-sans)" }}
            labelStyle={{ color: "var(--sq-text-secondary)", fontFamily: "var(--sq-font-sans)", fontSize: "11px" }}
          />
          <Legend
            verticalAlign="top"
            height={36}
            iconType="circle"
            iconSize={8}
            wrapperStyle={{
              fontSize: "11px",
              fontFamily: "var(--sq-font-sans)",
              color: "var(--sq-text-secondary)",
            }}
          />
          {series.map((s) => (
            <Bar
              key={s.key}
              dataKey={s.key}
              name={s.name}
              fill={s.color}
              radius={[4, 4, 0, 0]}
              maxBarSize={32}
            />
          ))}
        </ReBarChart>
      </ResponsiveContainer>
    </div>
  );
};
