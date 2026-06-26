import React from "react";
import {
  PieChart as RePieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

interface DonutChartData {
  name: string;
  value: number;
  color: string;
}

interface DonutChartProps {
  data: DonutChartData[];
  height?: number;
  id?: string;
}

export const DonutChart: React.FC<DonutChartProps> = ({
  data,
  height = 200,
  id,
}) => {
  const [isMounted, setIsMounted] = React.useState(false);

  React.useEffect(() => {
    setIsMounted(true);
  }, []);

  // Calculate percentage total
  const total = React.useMemo(() => data.reduce((sum, item) => sum + (item.value ?? 0), 0), [data]);

  if (!isMounted) {
    return (
      <div style={{ height }} className="w-full flex items-center justify-center bg-zinc-950/20 rounded-xl animate-pulse">
        <span className="text-[10px] text-zinc-500 font-mono tracking-widest uppercase">Processando Grid...</span>
      </div>
    );
  }

  return (
    <div id={id} className="flex flex-col md:flex-row items-center justify-between gap-4 py-2">
      {/* Circle Container */}
      <div style={{ width: "100%", maxWidth: 170, height }}>
        <ResponsiveContainer width="100%" height="100%">
          <RePieChart>
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
            />
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={55}
              outerRadius={75}
              paddingAngle={4}
              dataKey="value"
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} stroke="#111111" strokeWidth={1} />
              ))}
            </Pie>
          </RePieChart>
        </ResponsiveContainer>
      </div>

      {/* Side Legend Tracker */}
      <div className="flex-1 w-full space-y-2 font-sans">
        {data.map((item, idx) => {
          const value = item.value ?? 0;
          const itemPercentage = total > 0 ? ((value / total) * 100).toFixed(1) : "0.0";
          return (
            <div key={idx} className="flex items-center justify-between text-xs font-sans">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-md shrink-0" style={{ backgroundColor: item.color }} />
                <span className="text-neutral-300 font-sans">{item.name}</span>
              </div>
              <div className="flex items-center gap-3 font-mono">
                <span className="text-zinc-550 font-medium text-[11px]">{value.toLocaleString("pt-BR")}</span>
                <span className="text-indigo-400 font-semibold text-[11px] w-12 text-right">{itemPercentage}%</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
