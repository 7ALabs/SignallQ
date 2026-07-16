#!/usr/bin/env node
// GH#443 / SIG-52: monta o output do projeto Cloudflare Pages "signallq",
// que serve o Console (Admin) sob /console (signallq.pages.dev). Este script
// assume que as dependencias (`npm ci`) ja foram instaladas em
// `SignallQ Admin/`.
//
// O PWA (`pwa/`) foi descontinuado e removido do monorepo; este script nao
// builda mais nada sob /app.
//
// Uso local:
//   node deploy/pages/build.mjs
//
// Saida:
//   deploy/pages/dist/
//     console/...   (build do "SignallQ Admin/" com base '/console/')
//     _redirects    (copiado de deploy/pages/_redirects)
//     _headers      (copiado de deploy/pages/_headers)

import { execSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
// GH#443: cada app tem seu proprio _headers/_redirects (usado em preview/deploy
// standalone). No pacote unificado, so o _headers/_redirects da raiz de
// deploy/pages e lido pelo Cloudflare Pages — o das subpastas e removido
// para nao ficar um arquivo morto e confuso servido em /console/_headers etc.
const STALE_SUBPATH_FILES = ['_headers', '_redirects'];
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(here, '..', '..');
const adminDir = join(repoRoot, 'SignallQ Admin');
const outDir = join(here, 'dist');

function run(cmd, cwd, extraEnv = {}) {
  console.log(`\n$ ${cmd}  (cwd: ${cwd})`);
  execSync(cmd, { cwd, stdio: 'inherit', env: { ...process.env, ...extraEnv } });
}

// Cloudflare Pages nao processa corretamente _redirects/_headers com CRLF.
// No Windows, git costuma fazer checkout com CRLF mesmo sem essa intencao —
// normaliza pra LF na copia em vez de depender de config de git do ambiente.
function copyNormalizingLineEndings(src, dest) {
  const content = readFileSync(src, 'utf8').replace(/\r\n/g, '\n');
  writeFileSync(dest, content, 'utf8');
}

function requireDist(appDir, label) {
  const dist = join(appDir, 'dist');
  if (!existsSync(dist)) {
    throw new Error(`[deploy/pages] build de ${label} nao gerou ${dist}. Abortando.`);
  }
  return dist;
}

// 1. Build do Console (Admin) sob /console
run('npm run build', adminDir, { VITE_BASE_PATH: '/console/' });

// 2. Monta o diretorio de deploy
rmSync(outDir, { recursive: true, force: true });
mkdirSync(outDir, { recursive: true });

cpSync(requireDist(adminDir, 'SignallQ Admin'), join(outDir, 'console'), { recursive: true });

for (const file of STALE_SUBPATH_FILES) {
  rmSync(join(outDir, 'console', file), { force: true });
}

// Pages Functions (ex: functions/api/[[path]].ts, proxy same-origin pro Admin
// Worker) precisam ser compiladas num _worker.js na raiz do pacote de deploy.
// GH#{pendente}: `wrangler pages deploy <dir>` nao estava auto-detectando/
// compilando um functions/ solto dentro do diretorio de deploy neste ambiente
// (confirmado: nenhum log de "Compiled Worker" aparecia, e toda rota /api/*
// caia em 405 Method Not Allowed em producao — a Function nunca rodava).
// Compilar explicitamente com `wrangler pages functions build` e gerar
// _worker.js + _routes.json resolve isso de forma determinística.
const adminFunctionsDir = join(adminDir, 'functions');
if (existsSync(adminFunctionsDir)) {
  const workerBuildDir = join(here, '.worker-build');
  rmSync(workerBuildDir, { recursive: true, force: true });
  run(
    `npx wrangler pages functions build "${adminFunctionsDir}" --outdir="${workerBuildDir}" --build-output-directory="${outDir}" --output-routes-path="${join(outDir, '_routes.json')}"`,
    repoRoot
  );
  cpSync(join(workerBuildDir, 'index.js'), join(outDir, '_worker.js'));
  rmSync(workerBuildDir, { recursive: true, force: true });
}

copyNormalizingLineEndings(join(here, '_redirects'), join(outDir, '_redirects'));
copyNormalizingLineEndings(join(here, '_headers'), join(outDir, '_headers'));

console.log(`\n[deploy/pages] build unificado pronto em ${outDir}`);
console.log('[deploy/pages] deploy manual: npx wrangler pages deploy deploy/pages/dist --project-name signallq');
