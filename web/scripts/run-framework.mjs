import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { readExecutionProfile } from "./execution-profile.mjs";

const [command, ...args] = process.argv.slice(2);
if (!["dev", "build", "start"].includes(command)) throw new Error("Expected dev, build or start.");
const managedLinux = readExecutionProfile() === "managed-linux";

if (managedLinux && command === "build") {
  const result = spawnSync("bash", [
    fileURLToPath(new URL("./build-verified.sh", import.meta.url)), ...args,
  ], { stdio: "inherit" });
  if (result.error) throw result.error;
  process.exit(result.status ?? 1);
}

if (managedLinux && command === "start") {
  await import("./sites-env.mjs");
  const cli = new URL("../node_modules/wrangler/bin/wrangler.js", import.meta.url);
  process.argv = [process.execPath, fileURLToPath(cli), "dev", "--config", "dist/server/wrangler.json",
    "--local", "--persist-to", ".wrangler/state", "--ip", "127.0.0.1", "--inspector-port", "0",
    ...(process.env.LARAVEL_API_URL ? ["--var", `LARAVEL_API_URL:${process.env.LARAVEL_API_URL}`] : []), ...args];
  await import(cli.href);
} else {
// Import in this process so the preview owner retains its PID and signals.
const cli = new URL(managedLinux
  ? "../node_modules/vite/bin/vite.js"
  : "../node_modules/vinext/dist/cli.js", import.meta.url);
process.argv = [process.execPath, fileURLToPath(cli), command,
  ...(!managedLinux && command === "dev" ? ["--port", "5173"] : []),
  ...(!managedLinux && command === "start" ? ["--hostname", "127.0.0.1"] : []), ...args];
await import(cli.href);

}
