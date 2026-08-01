"use client";

import { SiteNav } from "./SiteNav";
import { SiteFooter } from "./SiteFooter";
import { AdRail } from "./AdRail";
import { AdBannerWide } from "./AdBannerWide";
import clsx from "clsx";

interface PageShellProps {
  ads?: boolean;
  contentMax?: string;
  children: React.ReactNode;
}

export function PageShell({
  ads = true,
  contentMax = "860px",
  children,
}: PageShellProps) {
  return (
    <div className="flex-1 flex flex-col w-full h-full min-h-screen">
      <SiteNav />

      <div
        className={clsx(
          "flex-1 w-full box-border flex items-start justify-center gap-6 mx-auto",
          "py-2 px-4 pb-6 lg:pt-5 lg:px-6 lg:pb-4"
        )}
      >
        {/* Left AdRail — a própria AdRail se esconde abaixo de `lg` via CSS */}
        {ads && <AdRail variant="a" />}

        {/* Main Content Area */}
        <div
          className="flex-1 self-stretch flex flex-col items-center gap-5 w-full"
          style={{ maxWidth: contentMax }}
        >
          {children}

          {/* Bottom AdBannerWide in content */}
          {ads && (
            <div className="w-full mt-auto pt-6">
              <AdBannerWide />
            </div>
          )}
        </div>

        {/* Right AdRail */}
        {ads && <AdRail variant="b" />}
      </div>

      <SiteFooter />
    </div>
  );
}
