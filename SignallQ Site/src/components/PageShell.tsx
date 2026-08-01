"use client";

import { SiteNav } from "./SiteNav";
import { SiteFooter } from "./SiteFooter";
import { AdRail } from "./AdRail";
import { AdBannerWide } from "./AdBannerWide";
import clsx from "clsx";

interface PageShellProps {
  mobile?: boolean;
  ads?: boolean;
  contentMax?: string;
  children: React.ReactNode;
}

export function PageShell({
  mobile = false,
  ads = true,
  contentMax = "860px",
  children,
}: PageShellProps) {
  const shellPadding = mobile ? "py-2 px-4 pb-6" : "pt-5 px-6 pb-4";

  return (
    <div className="flex-1 flex flex-col w-full h-full min-h-screen">
      {!mobile && <SiteNav mobile={mobile} />}

      {/* Top Mobile Bar (optional, handled inside specific pages or here if needed) */}
      {mobile && <SiteNav mobile={mobile} />}

      <div
        className={clsx(
          "flex-1 w-full box-border flex items-start justify-center gap-6 mx-auto",
          shellPadding
        )}
      >
        {/* Left AdRail */}
        {!mobile && ads && <AdRail variant="a" />}

        {/* Main Content Area */}
        <div
          className="flex-1 self-stretch flex flex-col items-center gap-5 w-full"
          style={{ maxWidth: mobile ? "100%" : contentMax }}
        >
          {children}

          {/* Bottom AdBannerWide in content */}
          {ads && (
            <div className="w-full mt-auto pt-6">
              <AdBannerWide compact={mobile} />
            </div>
          )}
        </div>

        {/* Right AdRail */}
        {!mobile && ads && <AdRail variant="b" />}
      </div>

      <SiteFooter />
    </div>
  );
}
