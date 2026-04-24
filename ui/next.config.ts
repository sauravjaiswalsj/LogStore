import type { NextConfig } from "next";
import path from "node:path";

const nextConfig: NextConfig = {
  outputFileTracingRoot: path.join(process.cwd(), ".."),
  typescript: {
    ignoreBuildErrors: true
  }
};

export default nextConfig;
