# SignallQ Diagnostic Worker

Worker Cloudflare isolado para tres frentes remotas do SignallQ:

- `GH#952`: motor remoto de diagnostico deterministico
- `GH#951`: diretorio remoto de provedores
- `GH#935`: catalogo remoto de jogos e perfis de sensibilidade

Migrado de `D:\Cloudflare\signallq-diagnostic-worker` (fora do monorepo, sem git) para
`integrations/cloudflare/signallq-diagnostic-worker/` em `gmmattey/linka-android` — ver `GH#963`.
Correcoes de paridade com o motor Android real (`GH#953`) aplicadas antes da migracao: ver
`TESTING.md` para o log completo de validacao.

## Arquitetura

O deploy continua sendo de um unico Worker, mas o codigo foi separado por dominio:

- [src/index.ts](src/index.ts): rotas HTTP, CORS e cron
- [src/diagnostic-engine.ts](src/diagnostic-engine.ts): motor remoto de diagnostico (regras + fluxo)
- [src/score-engine.ts](src/score-engine.ts): score 0-100 ponderado por tipo de conexao (porta de `ScoreEngine.kt`/`MetricClassifier.kt` do Android)
- [src/diagnostic-report.ts](src/diagnostic-report.ts): payload publico (`DiagnosticReportPayload`) e fallback minimo garantido
- [src/diagnostic-ai.ts](src/diagnostic-ai.ts): pacote de prompt da IA explicativa (single-shot, sem chat)
- [src/ruleset-store.ts](src/ruleset-store.ts): draft, publish e rollback em D1
- [src/provider-directory.ts](src/provider-directory.ts): diretorio de provedores, ingestao e review queue
- [src/game-catalog.ts](src/game-catalog.ts): catalogo remoto de jogos, perfis, ativacao e auditoria
- [src/auth.ts](src/auth.ts): auth admin e sessao
- [src/contracts.ts](src/contracts.ts): contratos compartilhados

## O que ficou pronto

- `POST /diagnostic/evaluate`
- regras bundled completas e validadas
- fluxo humano com entrada parcial e proxima acao guiada
- pacote opcional de IA single-shot pronto para plugar, sem chat
- historico/degradacao
- congestionamento de canal Wi-Fi
- decisoes compostas tipo `ISP vs problema local`
- rollout real em D1 com draft, publish e rollback
- auth de `/admin/*` com sessao `httpOnly`
- bootstrap do primeiro admin via token
- seed sync de provedores para D1
- CRUD administrativo basico de provedores
- review queue real de provedores baseada em deteccoes agregadas
- cron real para marcar provedores stale e enfileirar revisao
- lookup publico de provedores por ASN, id e busca textual
- catalogo remoto de jogos com filtro por plataforma
- perfis remotos de jogos com thresholds e politica de Wi-Fi
- ativacao e desativacao de jogos via admin sem release do app
- seed sync de jogos e perfis para D1
- auditoria de alteracoes de jogos/perfis
- testes locais cobrindo auth, ruleset, provedores e jogos

## O que ainda nao existe

- upload/download real de assets em R2
- worker consumer separado para enrichment externo
- rollout percentual remoto so para jogos
- observabilidade externa alem do basico do Wrangler

O modulo esta pronto para subir e conectar sem depender desses itens.

## Nomenclatura

- package: `signallq-diagnostic-worker`
- worker name: `signallq-diagnostic`
- D1: `signallq-diagnostic-db`
- admin: `/admin/*`
- diagnostico: `/diagnostic/*`
- provedores: `/providers/*`
- jogos: `/games/*`
- ingestao: `/ingest/*`

## Requisitos

- Node.js
- npm
- Wrangler autenticado na conta Cloudflare
- 1 banco D1
- 2 secrets para admin

## Scripts

```bash
npm install
npm run dev
npm run test
npm run typecheck
npm run verify
npm run deploy
```

## Como subir no Cloudflare

### 1. Criar o banco D1

```bash
npx wrangler d1 create signallq-diagnostic-db
```

Copie o `database_id` retornado e atualize [wrangler.toml](wrangler.toml) (hoje tem placeholder `00000000-0000-0000-0000-000000000000` — worker nunca foi deployado de verdade).

### 2. Aplicar as migrations

```bash
npx wrangler d1 execute signallq-diagnostic-db --file=migrations/001_gh952_diagnostic_rules.sql --remote
npx wrangler d1 execute signallq-diagnostic-db --file=migrations/002_gh951_provider_directory.sql --remote
npx wrangler d1 execute signallq-diagnostic-db --file=migrations/003_admin_auth.sql --remote
npx wrangler d1 execute signallq-diagnostic-db --file=migrations/004_gh935_game_catalog.sql --remote
npx wrangler d1 execute signallq-diagnostic-db --file=migrations/005_gh956_provider_installations.sql --remote
```

### 3. Configurar secrets

```bash
npx wrangler secret put ADMIN_AUTH_PEPPER
npx wrangler secret put ADMIN_BOOTSTRAP_TOKEN
```

Uso:

- `ADMIN_AUTH_PEPPER`: pepper do hash de senha admin
- `ADMIN_BOOTSTRAP_TOKEN`: token de uso unico para criar o primeiro admin

### 4. Ajustar vars opcionais

[wrangler.toml](wrangler.toml) ja vem com:

- `DIAGNOSTIC_RULESET_JSON`
- `PROVIDER_DIRECTORY_SEED_JSON`
- `GAME_CATALOG_SEED_JSON`
- `GAME_PROFILE_SEED_JSON`
- `ALLOWED_ORIGIN` — origem liberada para CORS em `/admin/*` (GH#960)
- `cron` horario para stale/review queue
- `observability` habilitada

Uso recomendado:

- deixe `DIAGNOSTIC_RULESET_JSON = ""` para usar D1 publicado ou bundled
- deixe `PROVIDER_DIRECTORY_SEED_JSON`/`GAME_CATALOG_SEED_JSON`/`GAME_PROFILE_SEED_JSON = ""` (vazias) para usar os seeds embutidos no codigo (`SEEDED_PROVIDERS`/`BUILTIN_GAME_CATALOG`/`BUILTIN_GAME_PROFILES`) — **nunca deixe essas 3 vars como `"[]"`** (array JSON valido e vazio): antes do GH#971, `"[]"` sobrescrevia silenciosamente o fallback com um diretorio/catalogo vazio (achado durante a validacao manual da GH#953, corrigido no GH#971 com defesa em profundidade tanto no `wrangler.toml` quanto no `loadSeedProviders` de `provider-directory.ts`, que agora trata array vazio parseado como "sem seed real" tambem)
- `ALLOWED_ORIGIN` default aponta pro mesmo dominio do SignallQ Console usado pelo `signallq-admin-worker`
- so preencha as demais vars se quiser override remoto em JSON

### 5. Deploy

```bash
npm run deploy
```

## Bootstrap do primeiro admin

Depois do deploy e dos secrets:

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/auth/bootstrap \
  -H "content-type: application/json" \
  -d "{\"bootstrapToken\":\"SEU_TOKEN\",\"email\":\"admin@empresa.com\",\"password\":\"SenhaForte123\"}"
```

Comportamento:

- se nao existir nenhum admin, cria o primeiro usuario
- se ja existir admin, responde `409`

## Diagnostico remoto

### Regras e rollout

Precedencia do ruleset:

1. ruleset `PUBLISHED` no D1
2. `DIAGNOSTIC_RULESET_JSON`
3. ruleset bundled local

Isso permite rollout e rollback reais sem redeploy.

### Exemplo de chamada

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/diagnostic/evaluate \
  -H "content-type: application/json" \
  -d "{\"schemaVersion\":6,\"wifi\":{\"band\":\"2_4_GHZ\",\"has5GhzAvailable\":true},\"speed\":{\"downloadMbps\":22},\"quality\":{\"latencyMs\":24,\"jitterMs\":4}}"
```

Resposta inclui:

- `wifiResultados`
- `internetResultados`
- `mobileResultados`
- `fibraResultados`
- `dnsResultados`
- `historicoResultados`
- `wifiCanalResultados`
- `redeResultados`
- `decisao`
- `achadosSecundarios`
- `hipotesesDescartadas`
- `dadosAusentes`
- `limitacoesEquipamentoLocal`
- `recomendacoes`
- `scoreEngineResultado`
- `perfisUso`
- `gameReadiness`
- `aiAssist`
- `geradoEmMs`

Esse payload foi adaptado para o formato da tela atual de diagnostico do app, no estilo `DiagnosticReport`, para ficar plug-and-play sem precisar redesenhar a UI. Internamente, o motor continua tolerando coleta incompleta: quando faltarem sinais, ele nao inventa certeza, preenche `dadosAusentes`, reduz a conclusao quando preciso e ainda devolve orientacao humana acionavel.

## IA sem chat

Este worker nao depende de fluxo conversacional.

A IA, quando usada, entra apenas como camada opcional de pos-explicacao:

- o motor deterministico sempre decide primeiro
- a tela nao precisa perguntar nada ao usuario
- nao existe chat nem follow-up
- a IA nao pode trocar a causa raiz escolhida pelo motor

Para isso, a resposta publica ja inclui `aiAssist`:

- `shouldInvoke`: se vale a pena chamar IA neste caso
- `reason`: por que o motor considera IA util ou desnecessaria
- `mode`: sempre `single_shot_explainer`
- `systemPrompt`: prompt de sistema pronto para plugar
- `userPrompt`: payload estruturado com `DiagnosticReport` e resumo do snapshot
- `expectedOutputSchema`: campos JSON esperados da resposta da IA

Uso recomendado:

1. consumir `POST /diagnostic/evaluate`
2. renderizar a tela normal com o payload deterministico
3. se `aiAssist.shouldInvoke` for `true`, chamar seu provedor de IA em uma unica rodada
4. usar a resposta so como explicacao complementar, nunca como nova decisao tecnica

## Console/Admin

### Login

```bash
curl -i -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/auth/login \
  -H "content-type: application/json" \
  -d "{\"email\":\"admin@empresa.com\",\"password\":\"SenhaForte123\"}"
```

Guarde o cookie `session=...`.

## Provedores

### Seed inicial

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/providers/sync-seed \
  -H "Cookie: session=SEU_COOKIE"
```

### Review queue

```bash
curl https://signallq-diagnostic.<subdominio>.workers.dev/admin/providers/review-queue \
  -H "Cookie: session=SEU_COOKIE"
```

### Provedores stale

```bash
curl https://signallq-diagnostic.<subdominio>.workers.dev/admin/providers/stale \
  -H "Cookie: session=SEU_COOKIE"
```

### Upsert manual

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/providers \
  -H "content-type: application/json" \
  -H "Cookie: session=SEU_COOKIE" \
  -d "{
    \"provider\": {
      \"id\": \"provedor-x\",
      \"displayName\": \"Provedor X\",
      \"officialDomain\": \"provedorx.com.br\",
      \"providerType\": \"REGIONAL\",
      \"status\": \"DRAFT\",
      \"aliases\": [\"provedor x\"],
      \"asns\": [65001],
      \"support\": {
        \"sacPhone\": \"0800000000\",
        \"websiteUrl\": \"https://provedorx.com.br\"
      }
    }
  }"
```

### Fechar revisao

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/providers/provedor-x/review \
  -H "content-type: application/json" \
  -H "Cookie: session=SEU_COOKIE" \
  -d "{\"status\":\"VERIFIED\",\"notes\":\"Revisado manualmente\"}"
```

## Jogos plug-and-play

Este worker agora pode ser a fonte remota de catalogo para a tela Jogos do app.

### O que o app pode fazer com isso

- buscar jogos por plataforma
- ativar ou ocultar jogos sem release
- alterar perfil de sensibilidade
- alterar estrategia de teste e regiao
- usar fallback local se o worker falhar

### Seed inicial de jogos e perfis

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/games/sync-seed \
  -H "Cookie: session=SEU_COOKIE"
```

### Ler catalogo publico

```bash
curl https://signallq-diagnostic.<subdominio>.workers.dev/games/catalog
curl https://signallq-diagnostic.<subdominio>.workers.dev/games/catalog?platform=PC
curl https://signallq-diagnostic.<subdominio>.workers.dev/games/catalog/version
curl https://signallq-diagnostic.<subdominio>.workers.dev/games/profiles
```

### Buscar um jogo especifico

```bash
curl https://signallq-diagnostic.<subdominio>.workers.dev/games/catalog/valorant
```

### Criar ou atualizar perfil remoto

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/games/profiles \
  -H "content-type: application/json" \
  -H "Cookie: session=SEU_COOKIE" \
  -d "{
    \"profile\": {
      \"profileCode\": \"COMPETITIVE_EXTREME\",
      \"displayName\": \"Competitivo extremo\",
      \"latencyGoodMax\": 30,
      \"latencyAttentionMax\": 80,
      \"jitterGoodMax\": 5,
      \"jitterAttentionMax\": 20,
      \"lossGoodMax\": 0,
      \"lossAttentionMax\": 1,
      \"wifiPolicy\": \"STRICT_WIFI_5GHZ_PREFERRED\"
    }
  }"
```

### Criar ou atualizar jogo remoto

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/games/catalog \
  -H "content-type: application/json" \
  -H "Cookie: session=SEU_COOKIE" \
  -d "{
    \"game\": {
      \"gameId\": \"rocket-league\",
      \"displayName\": \"Rocket League\",
      \"slug\": \"rocket-league\",
      \"active\": true,
      \"profileCode\": \"COMPETITIVE_EXTREME\",
      \"testStrategy\": \"REGIONAL_ESTIMATE\",
      \"regionCode\": \"SOUTH_AMERICA\",
      \"resultLabel\": \"Estimativa para Rocket League\",
      \"providerNetworkMode\": \"fallback_regional\",
      \"platforms\": [\"PC\", \"PS5\", \"XBOX\"],
      \"sortOrder\": 15
    }
  }"
```

### Desativar ou reativar jogo sem release

```bash
curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/games/catalog/valorant/deactivate \
  -H "Cookie: session=SEU_COOKIE"

curl -X POST https://signallq-diagnostic.<subdominio>.workers.dev/admin/games/catalog/valorant/activate \
  -H "Cookie: session=SEU_COOKIE"
```

### Auditoria

```bash
curl https://signallq-diagnostic.<subdominio>.workers.dev/admin/games/audit \
  -H "Cookie: session=SEU_COOKIE"
```

## Rotas

### Health

- `GET /health`

### Diagnostico

- `POST /diagnostic/evaluate`

### Admin auth

- `POST /admin/auth/bootstrap`
- `POST /admin/auth/login`
- `POST /admin/auth/logout`
- `GET /admin/auth/me`
- `POST /admin/auth/users`

### Admin diagnostico

- `GET /admin/diagnostic/rulesets`
- `GET /admin/diagnostic/rulesets/:version`
- `POST /admin/diagnostic/rulesets`
- `POST /admin/diagnostic/rulesets/:version/publish`
- `POST /admin/diagnostic/rulesets/:version/rollback`
- `POST /admin/diagnostic/rulesets/validate`
- `POST /admin/diagnostic/simulate`

### Admin provedores

- `POST /admin/providers/sync-seed`
- `GET /admin/providers/review-queue`
- `GET /admin/providers/stale`
- `POST /admin/providers`
- `POST /admin/providers/:providerId/review`

### Publico provedores

- `GET /providers/by-asn/:asn`
- `GET /providers/search?q=...`
- `GET /providers/:providerId`
- `GET /providers/:providerId/support`

### Admin jogos

- `POST /admin/games/sync-seed`
- `POST /admin/games/catalog`
- `POST /admin/games/catalog/:gameId/activate`
- `POST /admin/games/catalog/:gameId/deactivate`
- `POST /admin/games/profiles`
- `GET /admin/games/audit`

### Publico jogos

- `GET /games/catalog`
- `GET /games/catalog/version`
- `GET /games/catalog/:gameId`
- `GET /games/profiles`

### Ingestao

- `POST /ingest/provider-detection`

## CORS

Decisao GH#960: o worker e consumido tanto pelo app Android (server-to-server, nao sujeito a
CORS) quanto potencialmente por um futuro painel administrativo em browser sob `/admin/*` — mesmo
padrao ja usado pelo `signallq-admin-worker`, que serve o SignallQ Console. `Access-Control-*` e
aplicado globalmente (todas as rotas, nao so `/admin/*`) via `env.ALLOWED_ORIGIN`, no ponto de
saida (`fetch`), preflight `OPTIONS` respondido em `route()`.

## Cron

O cron configurado em [wrangler.toml](wrangler.toml) roda a cada hora e faz:

- marca provedores como `STALE` quando `next_review_at` venceu
- agrega deteccoes elegiveis e enfileira revisao em `provider_enrichment_jobs`

## Validacao local

Executado e validado nesta pasta:

```bash
npm run verify
```

Estado atual:

- `55/55` testes passando (32 originais + 23 de fronteira/regressao da GH#953)
- `tsc --noEmit` passando
- validacao manual via `wrangler dev --local` + D1 local + `curl` — ver [TESTING.md](TESTING.md)
