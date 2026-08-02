import type { NextConfig } from "next";
import withSerwistInit from "@serwist/next";

const withSerwist = withSerwistInit({
  swSrc: "src/app/sw.ts",
  swDest: "public/sw.js",
  disable: process.env.NODE_ENV === "development",
});

const nextConfig: NextConfig = {
  turbopack: {},
  // /pro e /teste foram removidas na fase Fundação da reconstrução v4 —
  // redirect 301 preserva links externos/indexados já publicados.
  async redirects() {
    return [
      { source: "/pro", destination: "/", permanent: true },
      { source: "/teste", destination: "/app", permanent: true },
    ];
  },
};

export default withSerwist(nextConfig);
