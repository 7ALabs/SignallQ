#!/usr/bin/env node
// GH#443 / SIG-52: monta o output unificado do projeto Cloudflare Pages "signallq",
// que serve o WebApp SignallQ sob /app e o Console (Admin) sob /console a partir
// de um unico dominio (signallq.pages.dev). Este script assume que as dependencias
// (`npm ci`) ja foram instaladas em `pwa/` e em `SignallQ Admin/`.
//
// Uso local:
//   node deploy/pages/build.mjs
//
// Saida:
//   deploy/pages/dist/
//     app/...       (build do pwa/ com base '/app/')
//     console/...   (build do "SignallQ Admin/" com base '/console/')
//     _redirects    (copiado de deploy/pages/_redirects)
//     _headers      (copiado de deploy/pages/_headers)

import { execSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
// GH#443: cada app tem seu proprio _headers/_redirects (usado em preview/deploy
// standalone). No pacote unificado, so o _headers/_redirects da raiz de
// deploy/pages e lido pelo Cloudflare Pages — os das subpastas sao removidos
// para nao ficar um arquivo morto e confuso servido em /app/_headers etc.
const STALE_SUBPATH_FILES = ['_headers', '_redirects'];
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(here, '..', '..');
const pwaDir = join(repoRoot, 'pwa');
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

// 1. Build do WebApp SignallQ sob /app
run('npm run build', pwaDir, { VITE_BASE_PATH: '/app/' });

// 2. Build do Console (Admin) sob /console
run('npm run build', adminDir, { VITE_BASE_PATH: '/console/' });

// 3. Monta o diretorio unificado de deploy
rmSync(outDir, { recursive: true, force: true });
mkdirSync(outDir, { recursive: true });

cpSync(requireDist(pwaDir, 'pwa'), join(outDir, 'app'), { recursive: true });
cpSync(requireDist(adminDir, 'SignallQ Admin'), join(outDir, 'console'), { recursive: true });

for (const sub of ['app', 'console']) {
  for (const file of STALE_SUBPATH_FILES) {
    rmSync(join(outDir, sub, file), { force: true });
  }
}

copyNormalizingLineEndings(join(here, '_redirects'), join(outDir, '_redirects'));
copyNormalizingLineEndings(join(here, '_headers'), join(outDir, '_headers'));

console.log(`\n[deploy/pages] build unificado pronto em ${outDir}`);
console.log('[deploy/pages] deploy manual: npx wrangler pages deploy deploy/pages/dist --project-name signallq');
