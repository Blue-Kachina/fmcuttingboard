// Bundles the extension (including JSON imported from ../shared/data) into dist/extension.js.
//   node esbuild.mjs               development build
//   node esbuild.mjs --watch       rebuild on change
//   node esbuild.mjs --production  minified build used for packaging
import * as esbuild from 'esbuild';

const production = process.argv.includes('--production');
const watch = process.argv.includes('--watch');

const options = {
  entryPoints: ['src/extension.ts'],
  bundle: true,
  outfile: 'dist/extension.js',
  platform: 'node',
  format: 'cjs',
  target: 'node22', // VS Code 1.101+ runs Node.js 22.15
  external: ['vscode'],
  sourcemap: !production,
  minify: production,
  logLevel: 'info',
};

// Integration tests (integration/*.test.ts) are bundled separately into out/integration for @vscode/test-cli
const integrationOptions = {
  entryPoints: ['integration/*.test.ts'],
  bundle: true,
  outdir: 'out/integration',
  platform: 'node',
  format: 'cjs',
  target: 'node22',
  external: ['vscode', 'mocha'],
  sourcemap: true,
  logLevel: 'info',
};

if (process.argv.includes('--integration')) {
  await esbuild.build(integrationOptions);
} else if (watch) {
  const ctx = await esbuild.context(options);
  await ctx.watch();
} else {
  await esbuild.build(options);
}
