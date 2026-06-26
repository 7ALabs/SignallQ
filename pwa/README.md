# SignallQ PWA

App web progressivo do SignallQ — diagnóstico de conectividade no navegador.

## Status

Fundação inicializada em React + TypeScript + Vite. Acompanhe no Linear pelo projeto `SignallQ` com issues marcadas como `area:pwa` (ou equivalente).

## Stack

React + TypeScript + Vite + Tailwind CSS + Cloudflare Pages Functions.

## Arquitetura inicial

- Frontend chama apenas rotas locais `/api/*`.
- `/api/ai/diagnostico-conexao` faz proxy para o Worker de IA e evita CORS direto no navegador.
- `/api/admin/ingest` faz ingest server-side no SignallQ Admin sem expor `INGEST_KEY` no bundle.
- `/api/speedtest/*` fornece endpoints web sem cache para a primeira medição PWA.
- Service Worker ignora `/api/*` para não cachear medições ou diagnósticos.

## Caminhos operacionais

- GitHub: `gmmattey/linka-android/tree/main/pwa`
- Local: `C:\Projetos\SignallQ\pwa`

## Agente responsável

**Renan** — implementação, revisão e correção de código React/TypeScript.

## Squad Farol no Codex

O PWA usa Codex local como executor principal.

Custom agents:

- `pwa/.codex/agents/renan.toml`
- `pwa/.codex/agents/eitam.toml`
- `pwa/.codex/agents/henrique.toml`

Skills:

- `pwa/.agents/skills/regras-pwa/SKILL.md`
- `pwa/.agents/skills/padroes-react/SKILL.md`
- `pwa/.agents/skills/signallq-design/SKILL.md`
- `pwa/.agents/skills/checar-release/SKILL.md`
- `pwa/.agents/skills/paridade-plataformas/SKILL.md`

## Como rodar

Para trabalhar só na UI com Vite:

```powershell
npm install
npm run dev
```

Para validar também as Cloudflare Pages Functions (`/api/*`):

```powershell
npm run pages:dev
```

Para build:

```powershell
npm run build
```

Variáveis esperadas no Cloudflare Pages:

- `AI_WORKER_URL`
- `ADMIN_INGEST_URL` ou `ADMIN_WORKER_URL`
- `ADMIN_INGEST_KEY`

Para desenvolvimento local com `pages:dev`, use `.dev.vars` com os mesmos nomes. Não versione esse arquivo.
