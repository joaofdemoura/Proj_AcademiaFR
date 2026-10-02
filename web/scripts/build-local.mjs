// Gera local-build/ (usado por `npm run preview:local`) a partir do código atual.
import { build } from 'esbuild';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

// Ponte com a API Laravel (login, sessão e dados do painel).
await build({
  absWorkingDir: root,
  entryPoints: ['lib/panel-api.ts'],
  outfile: 'local-build/api.mjs',
  bundle: true,
  platform: 'node',
  format: 'esm',
});

await build({
  absWorkingDir: root,
  entryPoints: ['scripts/local-client.tsx'],
  outfile: 'local-build/app.js',
  bundle: true,
  platform: 'browser',
  format: 'esm',
  jsx: 'automatic',
  minify: true,
  define: { 'process.env.NODE_ENV': '"production"' },
});
