# Decisão — Modelo de dados de avaliações completas do Google Play (épico #1341)

- **Status:** ativo
- **Última validação:** 2026-07-24
- **Fonte de verdade:** este documento
- **Escopo:** `integrations/cloudflare/signallq-admin-worker` — migration 017 (`google_play_reviews`),
  `schema.sql`, `handleGooglePlaySync`
- **Responsável:** Claudete (decisão), Luiz (execução — coordenador direto desta fase do catálogo,
  Camilo segue fora desta frente)
- **Precede:** `DECISAO_MODELO_DADOS_INTEGRACOES_PLAY_FIREBASE_2026-07-24.md` (critério de
  admin_settings vs. série temporal vs. tabela dedicada, aplicado aqui a um terceiro caso: lista de
  registros identificáveis)

## Contexto

`handleGooglePlaySync` (`src/index.ts:2376`) já chama `reviews.list` da Android Publisher API v3,
mas descarta tudo exceto `starRating`, usado só pra calcular uma média em `google_play_sync`
(`admin_settings`). O épico #1341 pede nota, comentário, idioma, versão, dispositivo, histórico,
resposta do desenvolvedor e status de tratamento — nenhum dos dois padrões já usados no catálogo
serve: não é estado pontual de config (`admin_settings`, chave única) nem série temporal
(`integration_metric_snapshots`, migration 016) — é uma lista de registros identificáveis, cada um
com `reviewId` próprio, que pode ser atualizado (edição de texto, resposta do dev adicionada
depois).

## Resposta às 4 perguntas

**1) Tabela própria — sim.** `google_play_reviews`, chave primária `review_id` (o `reviewId` da
API). Padrão de estilo seguido é o de `play_console_tracks` (migration 012): tabela dedicada e
enxuta, colunas tipadas em vez de payload JSON — aqui o schema de uma review é homogêneo o
suficiente (mesmos campos pra toda review) pra não precisar de JSON genérico.

**2) UPDATE em lugar (estado atual), não histórico de versão — confirmado o instinto.** Dois
motivos, não só preferência de produto: (a) os épicos não pedem histórico de edição, só o estado
atual pra UI; (b) mais importante — **a própria API não expõe histórico de edição da review**,
`reviews.list` retorna o `userComment` mais recente e, se houver, o `developerComment` mais
recente. Não existe fonte de dado real pra reconstruir "versões anteriores" do texto; guardar
histórico seria inventar granularidade que a API não entrega. UPDATE por `review_id` é o único
modelo sustentável.

**Ressalva que o pedido original não cobriu — status de tratamento é campo admin-side, não vem da
API.** `handling_status` (`pending | replied | dismissed`) é preenchido/alterado pelo admin no
Console, não pelo sync do Google. Isso importa pro upsert: `INSERT OR REPLACE` (usado hoje em
`writeGooglePlaySyncState`) resetaria `handling_status` pro default a cada sync — errado. Usar
`INSERT ... ON CONFLICT(review_id) DO UPDATE SET <colunas da API>` excluindo explicitamente
`handling_status` e `first_synced_at` do `SET` (exemplo completo comentado na própria migration).

**3) DDL — ver `migrations/017_gh1341_google_play_reviews.sql`** (já aplicado em `schema.sql`
também). Colunas: `review_id` (PK), `rating`, `comment_text`, `language`, `device`,
`android_os_version`, `app_version_code`, `app_version_name`, `review_last_modified`,
`developer_reply_text`/`developer_reply_at` (nuláveis — nem toda review tem resposta),
`handling_status` (default `pending`), `first_synced_at`/`last_synced_at`. Índices em `rating`
(filtro de UI por nota), `last_synced_at` (ordenação padrão) e `handling_status` (fila "pendente de
resposta").

**4) Paginação de `reviews.list` — existe, precisa ser tratada.** A API aceita `token` (query
param) pra página seguinte e a resposta inclui `tokenPagination.nextPageToken` quando há mais
resultados. Confiança alta, mas não é 100% verificada empiricamente neste catálogo — recomendação:
na implementação, logar/inspecionar `tokenPagination` na primeira chamada real (`maxResults=100` já
em uso) e confirmar se `reviews.length === 100` de fato acompanha `nextPageToken` presente; se não
bater, ajustar antes de assumir paginação incompleta silenciosamente perdendo reviews antigas.
Loop de paginação: repetir a chamada com `token=<nextPageToken>` até a resposta não trazer mais
`tokenPagination.nextPageToken` (ou até um teto de segurança de páginas, pra não rodar
indefinidamente se a API se comportar de forma inesperada).

## Ação

1. Aplicar `migrations/017_gh1341_google_play_reviews.sql` (`npx wrangler d1 execute
   signallq-admin-db --file=migrations/017_gh1341_google_play_reviews.sql --remote`) — já escrita,
   pronta pra rodar.
2. Reescrever `handleGooglePlaySync` pra persistir cada review via upsert (ver exemplo de SQL na
   migration) em vez de só calcular a média — `ratingAverage`/`reviewsSampled` em
   `google_play_sync` (`admin_settings`) podem continuar existindo como cache rápido pro endpoint
   `/status`, mas passam a ser derivados/mantidos em paralelo à tabela, não a única persistência.
3. Tratar paginação (`token`/`tokenPagination.nextPageToken`) no loop de sync — confirmar
   comportamento real na primeira execução contra a API antes de considerar fechado.
4. Endpoint(s) novo(s) de leitura/listagem de `google_play_reviews` (filtro por rating/status,
   paginação de UI) e de atualização de `handling_status` ficam fora desta decisão de modelo de
   dados — desenhar quando a Lia tiver o requisito de tela pronto.

## Quando reabrir essa decisão

- Se a API mudar e passar a expor histórico de edição real — reavaliar UPDATE-em-lugar vs. tabela
  de histórico.
- Se o volume de reviews justificar particionamento/arquivamento (não esperado no volume atual do
  app).
