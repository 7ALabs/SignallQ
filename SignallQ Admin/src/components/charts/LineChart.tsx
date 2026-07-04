import React from "react";
import { alpha } from "../../utils/color";
import {
  LineChart as ReLineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

interface LineChartProps {
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

export const LineChart: React.FC<LineChartProps> = ({
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
        <ReLineChart
          data={data}
          margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
        >
          <defs>
            {series.map((s, idx) => (
              <linearGradient key={idx} id={`gradient-${s.key}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={s.color} stopOpacity={0.2} />
                <stop offset="95%" stopColor={s.color} stopOpacity={0} />
              </linearGradient>
            ))}
          </defs>
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
            domain={["auto", "auto"]}
            allowDataOverflow={false}
            tickFormatter={(v: number) =>
              Number.isFinite(v)
                ? v >= 1000
                  ? `${(v / 1000).toFixed(1)}k`
                  : String(Math.round(v))
                : ""
            }
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
            <Line
              key={s.key}
              type="monotone"
              dataKey={s.key}
              name={s.name}
              stroke={s.color}
              strokeWidth={2}
              dot={{ r: 3, strokeWidth: 1 }}
              activeDot={{ r: 5, strokeWidth: 0, fill: s.color }}
            />
          ))}
        </ReLineChart>
      </ResponsiveContainer>
    </div>
  );
};
