import React from "react";
import {
  LayoutDashboard,
  LineChart,
  Activity,
  Wifi,
  Globe,
  BrainCircuit,
  AlertTriangle,
  GitBranch,
  Settings,
  Database,
  Cpu,
} from "lucide-react";
import { NAVIGATION_ITEMS, NavigationItem } from "../../config/navigation";
import { AppEnvironment } from "../../types/admin";

interface SidebarProps {
  currentPath: string;
  onNavigate: (path: string) => void;
  environment: AppEnvironment;
  id?: string;
}

// Map strings to Lucide components directly to prevent type issue
const iconMap = {
  LayoutDashboard: LayoutDashboard,
  LineChart: LineChart,
  Activity: Activity,
  Wifi: Wifi,
  Globe: Globe,
  BrainCircuit: BrainCircuit,
  AlertTriangle: AlertTriangle,
  GitBranch: GitBranch,
  Settings: Settings,
};

export const Sidebar: React.FC<SidebarProps> = ({
  currentPath,
  onNavigate,
  environment,
  id,
}) => {
  return (
    <div
      id={id || "sidebar-container"}
      className="w-[240px] h-screen bg-[#0A0A0D] border-r border-[#262626] flex flex-col justify-between shrink-0 select-none"
    >
      {/* Top Session / Branidng */}
      <div className="flex flex-col">
        {/* Logo Section */}
        <div className="p-6 flex items-center gap-3 border-b border-[#262626]">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[#6C2BFF] to-[#38BDF8] flex items-center justify-center text-white shadow-lg shadow-[#6C2BFF]/20">
            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <div>
            <span className="font-bold text-lg tracking-tight text-white block">
              SignallQ <span className="text-[#6B7280] font-normal">Admin</span>
            </span>
          </div>
        </div>

        {/* Environment Status Badge */}
        <div className="px-6 py-4 bg-[#0A0A0D]">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#18181B] border border-[#262626] w-fit">
            <div className={`w-2 h-2 rounded-full ${environment === "production" ? "bg-[#22C55E] shadow-[0_0_8px_#22C55E]" : "bg-[#38BDF8] shadow-[0_0_8px_#38BDF8]"}`}></div>
            <span className="text-[10px] font-semibold uppercase tracking-wider text-[#9CA3AF]">
              {environment === "production" ? "Production" : "Staging"}
            </span>
          </div>
        </div>

        {/* Navigation Menus List */}
        <nav className="px-4 py-2 space-y-1">
          {NAVIGATION_ITEMS.map((item) => {
            const IconComponent = iconMap[item.iconName];
            const isActive = currentPath === item.path;
            
            return (
              <button
                key={item.path}
                onClick={() => onNavigate(item.path)}
                className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-medium font-sans border transition-all duration-150 select-none cursor-pointer ${
                  isActive
                    ? "bg-[#6C2BFF]/10 text-[#6C2BFF] border-[#6C2BFF]/20 font-medium"
                    : "text-[#9CA3AF] border-transparent hover:text-white hover:bg-[#111111]"
                }`}
              >
                <div className="flex items-center gap-3">
                  {IconComponent && (
                    <IconComponent
                      className={`w-4 h-4 shrink-0 transition-colors ${
                        isActive ? "text-[#6C2BFF]" : "text-[#9CA3AF]"
                      }`}
                    />
                  )}
                  <span>{item.name}</span>
                </div>

                {item.badge && (
                  <span className={`text-[10px] font-mono font-semibold px-2 py-0.2 rounded-md ${
                    item.badgeType === "error"
                      ? "bg-[#FF4D4F]/10 text-[#FF4D4F] border border-[#FF4D4F]/20"
                      : "bg-[#18181B] border border-[#262626] text-[#9CA3AF]"
                  }`}>
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Sidebar Footer Info */}
      <div className="p-4 border-t border-[#262626]">
        <div className="p-3 bg-[#111111] rounded-xl border border-[#262626] flex items-center gap-3">
          <div className="w-7 h-7 rounded-lg bg-[#18181B] flex items-center justify-center text-[10px] font-mono text-[#9CA3AF] border border-[#262626]">
            CF
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-semibold text-white block truncate">Cloudflare Gateway</span>
            <span className="text-[10px] text-[#22C55E] font-mono block leading-tight">ONLINE • EDGE</span>
          </div>
        </div>
      </div>
    </div>
  );
};
