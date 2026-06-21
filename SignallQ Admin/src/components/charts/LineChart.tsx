import React from "react";
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
      <div style={{ height }} className="w-full flex items-center justify-center bg-zinc-950/20 rounded-xl animate-pulse">
        <span className="text-[10px] text-zinc-500 font-mono tracking-widest uppercase">Processando Grid...</span>
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
          <CartesianGrid strokeDasharray="3 3" stroke="#222225" />
          <XAxis
            dataKey={xAxisKey}
            stroke="#6B7280"
            fontSize={10}
            fontFamily="JetBrains Mono"
            tickLine={false}
            axisLine={false}
            dy={8}
          />
          <YAxis
            stroke="#6B7280"
            fontSize={10}
            fontFamily="JetBrains Mono"
            tickLine={false}
            axisLine={false}
            dx={-8}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: "#111111",
              border: "1px solid #262626",
              borderRadius: "10px",
              fontFamily: "Inter, sans-serif",
              fontSize: "12px",
              color: "#F3F4F6",
            }}
            itemStyle={{ color: "#F3F4F6", fontFamily: "Inter" }}
            labelStyle={{ color: "#9CA3AF", fontFamily: "JetBrains Mono", fontSize: "10px" }}
          />
          <Legend
            verticalAlign="top"
            height={36}
            iconType="circle"
            iconSize={8}
            wrapperStyle={{
              fontSize: "11px",
              fontFamily: "Inter",
              color: "#9CA3AF",
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
