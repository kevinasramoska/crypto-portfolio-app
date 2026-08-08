import { defineConfig, devices } from "@playwright/test";
import path from "node:path";

const frontendDir = __dirname;
const repoRoot = path.resolve(frontendDir, "..");
const backendDir = path.resolve(repoRoot, "backend");

export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  reporter: "list",
  use: {
    baseURL: "http://localhost:3000",
    trace: "on-first-retry",
  },
  webServer: [
    {
      command: "node ./e2e/mock-market-server.mjs",
      cwd: frontendDir,
      url: "http://127.0.0.1:9099/api/v3/simple/price?ids=bitcoin&vs_currencies=usd",
      reuseExistingServer: !process.env.CI,
    },
    {
      command: "./mvnw -Pe2e spring-boot:run -Dspring-boot.run.profiles=e2e",
      cwd: backendDir,
      env: {
        ...process.env,
        SERVER_PORT: "8080",
        CRYPTO_API_BASE_URL: "http://127.0.0.1:9099/api/v3",
        JWT_SECRET: "e2e-jwt-secret-with-at-least-32-bytes",
        PORTFOLIO_SNAPSHOT_FIXED_DELAY_MS: "3600000",
        PORTFOLIO_SNAPSHOT_INITIAL_DELAY_MS: "3600000",
      },
      url: "http://127.0.0.1:8080/api/market/supported-coins",
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: "npm run dev -- --port 3000",
      cwd: frontendDir,
      env: {
        ...process.env,
        NEXT_PUBLIC_API_BASE_URL: "http://localhost:8080/api",
      },
      url: "http://localhost:3000/login",
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
