import { execFile } from "node:child_process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

const input = process.env.OPENAPI_URL ?? "http://localhost:8080/v3/api-docs";
const output = "src/lib/api/generated.ts";

try {
  await execFileAsync(
    process.execPath,
    [
      "./node_modules/openapi-typescript/bin/cli.js",
      input,
      "-o",
      output,
    ],
    { stdio: "inherit" },
  );
} catch (error) {
  console.error(`Failed to generate API client from ${input}.`);
  throw error;
}
