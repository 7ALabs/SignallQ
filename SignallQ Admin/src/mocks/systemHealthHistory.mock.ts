import { SystemHealthHistoryResponse } from "../services/systemHealthHistoryService";

// #788 — mock reflete um estado plausível de painel em produção real (leve
// variação de latência, um dia com uptime abaixo de 100%), não uma linha reta
// artificial. Datas terminam hoje, 14 dias pra trás.
function buildPoints(): SystemHealthHistoryResponse["points"] {
  const latencies = [38, 41, 35, 52, 44, 39, 61, 47, 33, 40, 58, 42, 36, 45];
  const uptimes   = [100, 100, 100, 96.7, 100, 100, 100, 98.5, 100, 100, 100, 100, 100, 100];
  const points = [];
  for (let i = 13; i >= 0; i--) {
    const d = new Date();
    d.setUTCDate(d.getUTCDate() - i);
    points.push({
      date: d.toISOString().slice(0, 10),
      latencyP95Ms: latencies[13 - i],
      uptimePercentage: uptimes[13 - i],
    });
  }
  return points;
}

export const mockSystemHealthHistory: SystemHealthHistoryResponse = {
  source: "d1",
  period: "14d",
  points: buildPoints(),
};
