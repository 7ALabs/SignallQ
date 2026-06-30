# SignallQ PWA

App web progressivo do SignallQ — diagnóstico de conectividade no navegador.

## Status

Fundação inicializada em React + TypeScript + Vite. Acompanhe no Linear pelo projeto `SignallQ` com issues marcadas como `area:pwa` (ou equivalente).

Em 2026-06-30, a validação local de release passou com `npm run typecheck`, `npm test`, `npm run build` e `npm run pages:dev`. O script `npm run lint` continua ausente nesta etapa do projeto. O deploy remoto segue bloqueado até existir um projeto Cloudflare Pages `signallq-pwa` na conta autenticada usada pelo Wrangler.

## Stack

React + TypeScript + Vite + Tailwind CSS + Cloudflare Pages Functions.

## Arquitetura inicial

- Frontend chama apenas rotas locais `/api/*`.
- `/api/ai/diagnostico-conexao` faz proxy para o Worker de IA e evita CORS direto no navegador.
- `/api/admin/ingest` faz ingest server-side no SignallQ Admin sem expor `INGEST_KEY` no bundle.
- `/api/speedtest/*` fornece endpoints web sem cache para a primeira medição PWA.
- Service Worker ignora `/api/*` para não cachear medições ou diagnósticos.
- Contratos compartilhados ficam em `shared/`; handlers Pages ficam finos e chamam módulos em `functions/_modules/`.

## PWA e offline parcial

- `public/manifest.webmanifest` define nome, escopo, modo standalone, cores e ícones iniciais.
- `public/sw.js` cacheia apenas o shell e assets estáticos versionáveis.
- Rotas `/api/*` nunca são atendidas pelo cache do service worker.
- Offline parcial significa abrir a tela já carregada; SpeedTest, IA e ingest Admin continuam exigindo rede.
- Os ícones atuais são placeholders operacionais até o pacote visual final.

## Tokens e componentes base

- Tokens CSS ficam em `src/styles/tokens.css`.
- Estilos globais e layout base ficam em `src/styles/global.css`.
- Componentes base atuais:
  - `MetricCard`
  - `PrimaryButton`
  - `StatusBadge`
  - `RecommendationBlock`

Use tokens `--sq-*` antes de criar novos valores soltos de cor, spacing, radius ou tipografia.

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

Para validar o pacote local completo:

```powershell
npm run verify
```

Para typecheck:

```powershell
npm run typecheck
```

Para testes automatizados:

```powershell
npm test
```

Para publicar no Cloudflare Pages quando o projeto remoto existir:

```powershell
npm run pages:deploy
```

## Backend, Swagger e Postman

Para validar as Cloudflare Pages Functions localmente:

```powershell
npm run pages:dev
```

Com o servidor local ativo, abra:

- Swagger UI: `http://localhost:8788/docs/swagger.html`
- OpenAPI servido: `http://localhost:8788/docs/openapi.yaml`
- OpenAPI canônico no repo: `docs/openapi.yaml`

Para validar pelo Postman instalado:

1. Importe `postman/signallq-pwa-backend.postman_collection.json`.
2. Importe `postman/signallq-pwa-local.postman_environment.json`.
3. Se o `wrangler pages dev` subir em outra porta, ajuste `baseUrl`.
4. Execute a collection inteira.

Os testes de `AI Diagnosis - missing server env` e `Admin Ingest - missing server env` esperam `503` quando `.dev.vars` não está configurado. Com secrets reais configurados, esses casos devem ser ajustados ou executados em ambiente local sem secrets.

Variáveis esperadas no Cloudflare Pages:

- `AI_WORKER_URL`
- `ADMIN_INGEST_URL` ou `ADMIN_WORKER_URL`
- `ADMIN_INGEST_KEY`

Para desenvolvimento local com `pages:dev`, use `.dev.vars` com os mesmos nomes. Não versione esse arquivo.

## CI/CD e deploy

- Workflow PWA: `.github/workflows/pwa-ci.yml`
- Validação de PR: `npm ci`, `npm run verify`
- Preview deploy: tentado em PR quando `CLOUDFLARE_API_TOKEN` e `CLOUDFLARE_ACCOUNT_ID` estiverem configurados
- Production deploy: tentado em `main` com `npm run pages:deploy`
- Headers estáticos de segurança: `public/_headers`

Status real em 2026-06-30:

- conta Wrangler autenticada: `Giammattey, Luiz F.`
- projeto Pages `signallq-pwa`: não encontrado nessa conta
- consequência: preview, produção, domínio final e HTTPS não puderam ser validados como prontos nesta rodada

Consulte também:

- `docs/ci-cd.md`
- `docs/deploy-status.md`
- `docs/qa-evidence.md`
