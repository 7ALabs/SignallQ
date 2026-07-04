import { AppVersionUsage, AppVersionsResponse } from "../services/appVersionsService";

// Distribuição plausível para um app em fase de beta fechado: a maioria das sessões
// concentrada nas 2-3 builds mais recentes (curva de adoção real, não degrau uniforme),
// mistura de canal Play Store (produção) e Firebase App Distribution (beta interno).
const versions: AppVersionUsage[] = [
  {
    appVersion: "0.21.0",
    versionCode: 52,
    distChannel: "play_store",
    buildType: "release",
    sessions: 842,
    avgScore: 74,
    firstSeen: 1751500800,
    lastSeen: 1751932800,
  },
  {
    appVersion: "0.20.2",
    versionCode: 50,
    distChannel: "play_store",
    buildType: "release",
    sessions: 1963,
    avgScore: 71,
    firstSeen: 1750291200,
    lastSeen: 1751932000,
  },
  {
    appVersion: "0.20.1",
    versionCode: 49,
    distChannel: "firebase_app_distribution",
    buildType: "release",
    sessions: 118,
    avgScore: 69,
    firstSeen: 1750032000,
    lastSeen: 1750723200,
  },
  {
    appVersion: "0.20.0",
    versionCode: 48,
    distChannel: "play_store",
    buildType: "release",
    sessions: 743,
    avgScore: 68,
    firstSeen: 1749427200,
    lastSeen: 1750809600,
  },
  {
    appVersion: "0.19.0",
    versionCode: 45,
    distChannel: "play_store",
    buildType: "release",
    sessions: 214,
    avgScore: 65,
    firstSeen: 1748822400,
    lastSeen: 1750204800,
  },
];

export const mockAppVersions: AppVersionsResponse = {
  versions,
  productionVersion: versions.find((v) => v.distChannel === "play_store") ?? null,
};
