import {
  LayoutDashboard,
  LineChart,
  Activity,
  Wifi,
  BrainCircuit,
  AlertTriangle,
  GitBranch,
  Settings,
  HeartPulse,
  Wrench,
  PlayCircle,
  Flame,
} from "lucide-react";
import { NavigationItem } from "./navigation";

/**
 * Mapa único ícone-nome -> componente Lucide, extraído do Sidebar em
 * 2026-07-16 pra ser reaproveitado também pelo NavRail e pelo MaisSheet do
 * BottomNav — evita duplicar o mesmo mapeamento em três componentes de nav.
 */
export const NAVIGATION_ICON_MAP: Record<NavigationItem["iconName"], React.ComponentType<{ className?: string; style?: React.CSSProperties }> | undefined> = {
  LayoutDashboard,
  LineChart,
  Activity,
  Wifi,
  Globe: undefined,
  BrainCircuit,
  AlertTriangle,
  GitBranch,
  ToggleRight: undefined,
  Settings,
  HeartPulse,
  Wrench,
  PlayCircle,
  Flame,
};
