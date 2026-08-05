# P2 — Separação de ambiente para o Admin Worker e D1

- **Status:** plano técnico, sem operação remota executada.
- **Escopo:** `integrations/cloudflare/signallq-admin-worker`.
- **Motivo:** o `wrangler.toml` atual declara um único binding `DB` para
  `signallq-admin-db`. O campo `environment` nos registros permite filtro lógico,
  mas desenvolvimento e produção ainda compartilham a mesma base D1.

## Estado confirmado no código

1. O Worker usa o binding único `DB`.
2. `environment`, `dist_channel` e `build_type` já acompanham os eventos de
   ingest; isso não isola dados, credenciais, migrações nem impacto de testes.
3. Não existe `[env.development]` nem outro binding D1 no `wrangler.toml`.

## Mudança proposta, dependente de autorização do Luiz

Criar um D1 de desenvolvimento distinto e configurar um ambiente Wrangler
`development` com `DB` apontando exclusivamente para ele. Produção mantém o
binding e o database id atuais. O código TypeScript não precisa mudar porque o
contrato continua sendo `Env.DB`; a separação é de configuração e recurso remoto.

Pré-requisitos externos:

1. Luiz autoriza criação do recurso D1 e confirma conta Cloudflare e convenção de
   nome, por exemplo `signallq-admin-dev-db`.
2. Acesso Cloudflare com permissão para criar D1, aplicar migrations e publicar
   Worker em ambiente não produtivo.
3. Decisão explícita sobre segredos de desenvolvimento: valores próprios, nunca
   cópia de `INGEST_KEY`, `ADMIN_SECRET` ou credenciais de produção.

## Sequência executável após autorização

1. Criar o D1 de desenvolvimento e registrar o `database_id` retornado.
2. Adicionar `[env.development]` e `[[env.development.d1_databases]]` ao
   `wrangler.toml`, preservando o binding `DB` e usando apenas o novo id.
3. Aplicar todas as migrations versionadas ao novo banco, em ordem, e registrar
   os hashes dos arquivos efetivamente aplicados.
4. Configurar somente secrets de desenvolvimento no ambiente `development`.
5. Publicar somente `--env development`; não executar `wrangler deploy` sem
   ambiente explícito.
6. Validar `GET /health`, ingest autenticado com chave de desenvolvimento e
   consultas de contagem no D1 de desenvolvimento.
7. Confirmar que os ids de D1 de produção e desenvolvimento são diferentes e
   que nenhuma escrita de teste apareceu na produção.

## Evidências de aceite

- Saída de criação com id do D1 de desenvolvimento (sem secrets).
- Diff do `wrangler.toml` mostrando bindings distintos.
- Lista e SHA-256 das migrations aplicadas, com resultado por migration.
- Logs/HTTP de smoke do ambiente `development`.
- Consulta de contagem em ambos os D1 comprovando ausência de dados de teste na
  produção.

## Limites

Este plano não cria D1, não adiciona secrets, não aplica migration, não publica
Worker e não altera o binding de produção. A separação não deve ser simulada por
apenas filtrar a coluna `environment`.
