import { defineConfig } from "drizzle-kit";

export default defineConfig({
  out: "../banco/drizzle",
  schema: "./db/schema.ts",
  dialect: "sqlite",
});
