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

  const total = React.useMemo(
    () => data.reduce((sum, item) => sum + (item.value ?? 0), 0),
    [data]
  );

  if (!isMounted) {
    return (
      <div
        style={{ height, backgroundColor: "color-mix(in srgb, var(--sq-bg-primary) 20%, transparent)" }}
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
    <div id={id} className="flex flex-col md:flex-row items-center justify-between gap-4 py-2">
      {/* Donut */}
      <div style={{ width: "100%", maxWidth: 170, height }}>
        <ResponsiveContainer width="100%" height="100%">
          <RePieChart>
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
                <Cell key={`cell-${index}`} fill={entry.color} stroke="var(--sq-bg-card)" strokeWidth={1} />
              ))}
            </Pie>
          </RePieChart>
        </ResponsiveContainer>
      </div>

      {/* Legend */}
      <div className="flex-1 w-full space-y-2">
        {data.map((item, idx) => {
          const value = item.value ?? 0;
          const itemPercentage = total > 0 ? ((value / total) * 100).toFixed(1) : "0.0";
          return (
            <div key={idx} className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-md shrink-0" style={{ backgroundColor: item.color }} />
                <span style={{ color: "var(--sq-text-primary)" }}>{item.name}</span>
              </div>
              <div className="flex items-center gap-3 font-mono">
                <span className="font-medium text-[11px]" style={{ color: "var(--sq-text-secondary)" }}>
                  {value.toLocaleString("pt-BR")}
                </span>
                <span
                  className="font-semibold text-[11px] w-12 text-right"
                  style={{ color: "var(--sq-accent)" }}
                >
                  {itemPercentage}%
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
