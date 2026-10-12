import type { NextConfig } from "next";

const gatewayOrigin = process.env.GATEWAY_ORIGIN?.replace(/\/$/, "") ?? "http://127.0.0.1:8080";
const isDevelopment = process.env.NODE_ENV === "development";
const environmentSpecificConfig: NextConfig = isDevelopment
  ? {
      async rewrites() {
        return [{ source: "/api/:path*", destination: `${gatewayOrigin}/api/:path*` }];
      },
    }
  : { output: "export" };

const nextConfig: NextConfig = {
  ...environmentSpecificConfig,
  agentRules: false,
  trailingSlash: true,
  images: {
    unoptimized: true,
  },
  turbopack: {
    root: process.cwd(),
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
