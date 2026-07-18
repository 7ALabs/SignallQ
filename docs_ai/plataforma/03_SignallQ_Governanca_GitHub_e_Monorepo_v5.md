# Governança de Engenharia — SignallQ Platform

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** `v3__SignallQ_Platform_Governanca_GitHub_e_Monorepo_v3.txt`

Operação segura no GitHub e no monorepo `signallq-platform`. Regras práticas para desenvolver SignallQ, SignallQ Pro, Portal, SignallQ Admin e SignallQ Nethal no mesmo repositório sem quebrar os demais produtos. Esta v5 traz o documento (órfão desde a v3) ao pacote atual: corrige a grafia de Nethal, alinha a matriz de dependências e CI à árvore canônica e adiciona governança para migrations D1, RBAC do Admin e contratos versionados.

> **Fonte de nomes e decisões transversais:** `00_CANONICO_v5.md`. Em qualquer divergência, o canônico prevalece.

## Estado atual vs. Alvo

A governança abaixo descreve o regime-alvo do monorepo `signallq-platform` (ALVO — ainda não existe). Hoje o código vive em `gmmattey/linka-android` (ATUAL) mais repos separados (`nethal`, `linka-webapp`, `linka-speedtest`). Enquanto o monorepo não é criado, estas regras valem como contrato a implementar na Fase 1 do plano de migração; a proteção de main e o CI por impacto são pré-requisitos das fases seguintes.

Correção de nomenclatura desta versão: toda menção da v3 a "NetHAL", "NetHAL Lab" ou `nethal-lab` passa a **SignallQ Nethal** / `apps/signallq-nethal` (canônico §2). Não usar `NetHAL`, `nethal-lab`, `nethal-lab-internal`.

**Identificadores técnicos a nunca renomear:** `io.signallq.app`, repo `gmmattey/linka-android`, DB `linkaKotlin.db`, DataStore `linkaPreferencias`, canais `linka_*`.

---

## 1. Objetivo e escopo

Definir como trabalhar no `signallq-platform` com segurança, rastreabilidade e releases independentes. Vale para pessoas e agentes de IA que alterem qualquer parte do ecossistema.

- Evitar que uma alteração no SignallQ Pro quebre o aplicativo gratuito.
- Evitar que mudanças em bibliotecas compartilhadas sejam integradas sem validar todos os consumidores.
- Permitir desenvolvimento paralelo com escopo claro e baixo conflito.
- Preservar a estabilidade da main e manter produção sempre publicável.
- Separar migração, desenvolvimento de produto e experimentos de laboratório.

**Decisão central:** um único repositório não significa um único aplicativo, build ou release. Cada produto mantém fronteiras, pipeline, versão e artefato próprios. As regras existem para reduzir retrabalho, regressão e mudanças oportunistas — não para criar burocracia.

## 2. Fronteiras dos produtos

Cada aplicativo é uma unidade de produto; bibliotecas compartilhadas são unidades de capacidade. Um produto pode consumir uma capacidade, mas não pode depender diretamente da implementação interna de outro produto. Referência de estrutura: árvore canônica `00_CANONICO_v5.md` §4 (`apps/`, `mobile/`, `domain/`, `network-hardware/`, `backend/`, `web/`, `packages/`).

## 3. Modelo de dependências

As dependências apontam para capacidades compartilhadas. Um aplicativo nunca importa classes internas de outro aplicativo.

Regras obrigatórias:

- Módulos de aplicativo podem depender de bibliotecas; bibliotecas não dependem de aplicativos.
- Domínio profissional (Pro) não pode ser dependência transitiva do Consumer.
- Drivers experimentais só podem ser consumidos pelo SignallQ Nethal.
- APIs públicas dos módulos devem ser pequenas, documentadas e cobertas por testes.
- `implementation` deve ser preferido a `api` para impedir vazamento de dependências.

Matriz de dependências (alinhada à árvore canônica):

| Produto | Pode depender de | Não deve depender de |
|---|---|---|
| Android Consumer | `mobile/core`, `mobile/features` (compartilháveis), `domain/diagnostics`, `domain/reporting` | domínio Pro, billing Pro, telas Pro, drivers experimentais |
| Android Pro | `mobile/core`, `mobile/features`, `domain/*` (incl. customer/visit/billing/entitlement), drivers aprovados | anúncios B2C, onboarding consumidor, monetização do gratuito |
| SignallQ Nethal | `network-hardware` completo e drivers experimentais | features de negócio do Consumer ou do Pro |
| Portal (web) | `web/packages`, `packages/api-contracts`, `packages/shared-types`, design tokens e APIs | código Android/Kotlin específico de plataforma |
| Admin Web | `packages/api-contracts`, `packages/telemetry-schema`, APIs administrativas | UI ou estado local dos aplicativos móveis |

Fluxo de dependência resumido:

```
android-consumer -> mobile/core + mobile/features + domain/diagnostics + domain/reporting
android-pro      -> mobile/core + mobile/features + domain/* + drivers aprovados
signallq-nethal  -> network-hardware
portal-web       -> web/packages + packages/api-contracts + web/packages/speedtest
admin-web        -> packages/api-contracts + packages/telemetry-schema + backend/workers/admin (via API)
```

## 4. Estratégia de branches

Todo trabalho nasce em branch curta e específica. A main recebe somente mudanças revisadas e validadas.

| Tipo | Padrão | Exemplo |
|---|---|---|
| Nova funcionalidade | `feat/<área>-<objetivo>` | `feat/pro-clientes` |
| Correção | `fix/<produto>-<problema>` | `fix/consumer-wifi-scan` |
| Refatoração | `refactor/<módulo>-<objetivo>` | `refactor/nethal-driver-contract` |
| Documentação | `docs/<assunto>` | `docs/platform-governance` |
| Migração | `migration/<origem>-<destino>` | `migration/linka-webapp-monorepo` |
| Experimento | `experiment/<área>-<hipótese>` | `experiment/tplink-stok-driver` |

Branches proibidas: `ajustes`, `melhorias`, `develop`, `nova-versao`, `teste`. Escondem o escopo e viram depósito de mudanças desconexas.

## 5. Pull requests

Cada PR resolve um problema ou entrega uma capacidade identificável. PR grande reduz revisão, aumenta conflito e dificulta rastrear regressões.

| Critério | Regra |
|---|---|
| Escopo | Uma mudança principal. Refatorações auxiliares só quando necessárias à entrega |
| Descrição | Contexto, solução, áreas alteradas, riscos, testes executados e evidências |
| Tamanho | Preferir mudanças pequenas e encadeadas. Dividir migrações em fases |
| Arquivos fora do escopo | Removidos do PR ou justificados explicitamente |
| Merge | Squash merge por padrão; merge commit apenas em migrações onde preservar histórico importa |
| Estado | Draft enquanto incompleto; Ready for review somente com testes executados |

Modelo mínimo de descrição:

```
## Contexto
## O que mudou
## Áreas afetadas
## Riscos
## Testes executados
## Evidências
## Fora de escopo
```

## 6. CI orientado por impacto

Os workflows detectam os diretórios modificados e executam apenas os pipelines necessários. Mudança local testa o produto local; mudança compartilhada testa todos os consumidores.

| Área alterada | Pipelines obrigatórios |
|---|---|
| `apps/android-consumer/**` | Build, unitários, lint e testes de UI do Consumer |
| `apps/android-pro/**` | Build, unitários, lint e testes de UI do Pro |
| `apps/portal-web/**` | Typecheck, lint, Vitest, build PWA e smoke test |
| `apps/admin-web/**` | Typecheck, lint, testes e build do Admin |
| `network-hardware/**` | Nethal Core, drivers alterados, SignallQ Nethal e consumidores aprovados |
| `mobile/features/measurements/**` | Android Consumer e Android Pro |
| `mobile/core/designsystem/**` | Consumer, Pro e comparação visual de screenshots |
| `domain/**` | Todos os apps que consomem o domínio afetado |
| `backend/workers/**` | Worker alterado, contratos e consumidores da API |
| `backend/d1/migrations/**` | Migration forward, rollback lógico, dados de teste e compatibilidade |
| `packages/api-contracts/**` | Todos os clientes que usam o contrato |
| `packages/telemetry-schema/**` | Produtores e consumidores de eventos (apps, workers, Admin) |

**Regra de ouro:** mudou um módulo compartilhado? Não basta testar o módulo. Todos os aplicativos que dependem dele precisam compilar e passar seus testes de contrato.

## 7. Proteção da main

A main representa o estado integrado e deve estar sempre apta a gerar os artefatos de produção.

- Pull request obrigatório para toda alteração.
- Checks obrigatórios conforme a matriz de impacto.
- Branch atualizada antes do merge quando houver mudanças conflitantes.
- Force push e exclusão da main bloqueados.
- Conversas de revisão resolvidas antes do merge.
- Aprovação obrigatória em contratos, segurança, drivers, banco e pipelines de release.
- Deploy de produção somente a partir de tag ou workflow manual autorizado.

CODEOWNERS recomendado:

```
/apps/android-consumer/   @gmmattey
/apps/android-pro/        @gmmattey
/network-hardware/        @gmmattey
/backend/                 @gmmattey
/backend/d1/migrations/   @gmmattey
/packages/api-contracts/  @gmmattey
/packages/telemetry-schema/ @gmmattey
/.github/workflows/       @gmmattey
```

## 8. Contratos e compatibilidade

O risco principal do monorepo é alterar uma capacidade compartilhada e quebrar consumidores silenciosamente. Isso é tratado como mudança de contrato.

- Modelos públicos precisam ser versionados ou evoluídos de forma retrocompatível.
- Antes de remover uma API, marcar como deprecated e migrar os consumidores.
- Banco de dados exige migration testada; nunca alterar schema destrutivamente em produção.
- Schemas JSON e OpenAPI devem ter testes de contrato e exemplos válidos.
- Drivers Nethal expõem capabilities; aplicativos não conhecem detalhes do firmware.
- Regras remotas mantêm fallback local compatível com a versão do aplicativo.

**Mudança incompatível:** quando a quebra for necessária, nasce como nova versão de contrato e migra consumidores em PRs coordenados. Não se altera a interface e "vê depois o que quebra".

### 8.1 Contratos versionados (`packages/api-contracts` e `packages/telemetry-schema`)

Artefatos novos do escopo v4 que a v3 não cobria. Ambos são ALVO (ainda não existem — canônico §8, item 7) e passam a ter governança própria:

- **`packages/api-contracts`** — contratos HTTP entre apps/Portal/Admin e os Workers. Toda mudança é versionada; a remoção de campo passa por deprecation; o CI roda testes de contrato contra todos os clientes listados. Nenhum cliente acessa D1 diretamente — sempre via contrato.
- **`packages/telemetry-schema`** — envelope e catálogo de eventos em `dot.case` (canônico §3), com nomes de propriedades em `snake_case` e `source` na allowlist (`android_consumer`, `android_pro`, `portal_web`, `admin_web`, `signallq_nethal`, `worker`). Mudança de evento exige atualização do schema no mesmo PR; produtores e consumidores são revalidados pelo CI. A convivência com a telemetria Firebase atual (ATUAL) é temporária e explícita.

### 8.2 Migrations D1 (expand-migrate-contract)

Governança de banco que a v3 não detalhava. Migrations vivem em `backend/d1/migrations`. Toda alteração de schema segue **expand → migrate → contract**:

1. **Expand** — adicionar colunas/tabelas novas de forma retrocompatível, sem remover nada.
2. **Migrate** — backfill e mudança de escrita dos consumidores para o novo formato, com a janela de rollout convivendo com o schema antigo.
3. **Contract** — remover o antigo somente depois que nenhum consumidor ativo o usa.

Regras: migration forward e rollback lógico testados; isolamento por `professional_id` no modelo Pro; nunca alteração destrutiva em produção; segredos e dados de cliente nunca em fixtures ou seeds versionados. O CI de `backend/d1/migrations/**` roda dry-run de migração antes do deploy.

## 9. Feature flags e código incompleto

Funcionalidades podem ser integradas desligadas quando isso reduz branches longas e conflitos. Mas a main não aceita código que não compila, não passa testes ou deixa fluxo quebrado.

| Permitido | Não permitido |
|---|---|
| Feature completa, testada e desligada por flag | Usar flag para esconder tela quebrada ou código sem teste |
| Contrato preparado para ativação remota | Misturar flag de release com regra de arquitetura |
| Driver experimental acessível apenas no SignallQ Nethal | Liberar driver experimental automaticamente no Pro |
| Migração incremental com comportamento antigo preservado | Manter duas fontes da verdade indefinidamente |

## 10. RBAC e segurança do Admin

Governança do Admin que a v3 não cobria. `apps/admin-web` é interno e nunca compartilha sessão ou privilégios com contas dos aplicativos. Todo acesso passa por `backend/workers/admin` com **RBAC**, escopos mínimos e trilha de auditoria; o navegador nunca acessa D1 diretamente.

- Autenticação, autorização e auditoria administrativas têm políticas próprias, separadas das contas de técnicos e consumidores.
- Toda operação sensível (visualização de registro sensível, exportação, mudança de config/flag) gera evento de auditoria (`admin.*` no catálogo canônico).
- Tabelas de suporte já existem no D1 do Admin (ATUAL): `admin_users`, `admin_sessions`, `auth_rate_limit`, `feature_flags`, `feature_flag_audit`.
- Segredos nunca entram no repositório, fixtures, screenshots ou logs do CI.

## 11. Releases independentes

Cada produto possui versão, tag, artefato, credenciais e canal de distribuição próprios.

- Uma mudança no Portal não incrementa automaticamente a versão Android.
- Cada workflow lê apenas os segredos necessários ao seu produto.
- Ambientes dev, staging e produção usam bindings e credenciais distintos.
- Publicação nunca ocorre só porque um PR foi aberto; ocorre após merge e gatilho definido.

Tags canônicas (`00_CANONICO_v5.md` §7):

| Produto | Tag | Artefato / destino |
|---|---|---|
| SignallQ (Consumer) | `consumer/android/vX.Y.Z` | AAB para Play Console do SignallQ |
| SignallQ Pro | `pro/android/vX.Y.Z` | AAB para Play Console do SignallQ Pro |
| Portal | `portal-web/vX.Y.Z` | Cloudflare Pages |
| Admin | `admin-web/vX.Y.Z` | Cloudflare Pages / ambiente protegido |
| SignallQ Nethal | `signallq-nethal/vX.Y.Z` | APK interno / Firebase App Distribution |
| Worker | `worker-<nome>/vX.Y.Z` | Cloudflare Workers |

`versionName` fica em `0.x.y` enquanto em trilha de teste; `1.0.0` reservado ao primeiro publish em `production`.

## 12. Trabalho paralelo com agentes de IA

Todo agente recebe um **contrato de execução**. O agente não pode decidir sozinho expandir o escopo porque encontrou código feio em outro módulo.

| Campo obrigatório | Exemplo |
|---|---|
| Objetivo | Adicionar cadastro local de clientes no SignallQ Pro |
| Escopo permitido | `mobile/features/customers/**` e `apps/android-pro/**` |
| Áreas proibidas | `apps/android-consumer/**`, `network-hardware/**` e `backend/**` |
| Contratos preservados | `CustomerRepository` e schema `customer` v1 |
| Testes obrigatórios | `:mobile:features:customers:test` e `:apps:android-pro:test` |
| Saída esperada | Código, testes, evidências e PR pequeno |
| Melhorias oportunistas | Registrar issue; não alterar no mesmo PR |

**Regra para agentes:** ao encontrar problema fora do escopo, registrar como achado ou issue. Não "aproveitar" para refatorar outro produto, renomear arquivos ou reorganizar documentação sem autorização.

## 13. Mudanças críticas de autenticação e cobrança

Alterações em autenticação, assinatura, entitlements, Pix, recibos ou migrações D1 têm impacto transversal e exigem revisão reforçada.

- PRs de autenticação e cobrança não podem ser aprovados apenas por teste visual.
- Segredos nunca entram no repositório, fixtures, screenshots ou logs do CI.
- Mudança de schema exige migration versionada e contrato retrocompatível durante a janela de rollout.

| Área alterada | Validação obrigatória |
|---|---|
| `backend/workers/auth` | Login Google/local, refresh, revogação, rate limit e recuperação |
| `backend/d1/migrations` | Migration forward, rollback lógico, dados de teste e compatibilidade |
| `domain/billing` / billing worker | Play Billing, entitlement, grace period e expiração |
| `domain/appointment` | Consumer não afetado; Pro e integração calendário passam |
| billing/pix | Vetores BR Code, QR legível, copia-e-cola e proteção de dados |
| receipts | Numeração, imutabilidade, cancelamento, substituição e PDF |

Estratégia de rollout por feature:

| Feature | Estratégia |
|---|---|
| Login local | Rollout interno → alpha → percentual crescente |
| Assinatura | Sandbox/licence testers → alpha → produção |
| Google Agenda | Opt-in e revogável; fallback para intent |
| Pix e recibo | Disponível offline; telemetria sem dados sensíveis |
| Link público WhatsApp | Rate limit, captcha/antiabuso e rollout posterior |

## 14. Definition of Done ampliada

- Nenhum dado sensível aparece em logs, analytics ou crash reports.
- Fluxo funciona com conta Google e conta local.
- Plano Free e Pro respeitam limites e preservam dados ao expirar.
- Agenda possui fallback funcional sem integração Google.
- QR Code e código Pix são equivalentes e validados com vetores conhecidos.
- Recibo não pode ser editado após emissão.
- Documentação, contratos e migrations foram atualizados no mesmo PR.

## 15. Migração dos repositórios atuais

A consolidação acontece por etapas. O repositório antigo continua oficial até o novo pipeline reproduzir build, testes e publicação com segurança.

| Repositório | Estratégia |
|---|---|
| `linka-android` | Migrar primeiro; torna-se Android Consumer e origem dos módulos compartilhados |
| `nethal` | Migrar preservando o SignallQ Nethal e a fronteira entre drivers aprovados e experimentais |
| `linka-webapp` | Migrar como base do Portal com pipeline próprio |
| `linka-speedtest` | Comparar, extrair motor útil, eliminar duplicações e arquivar |
| `orbit-project` | Continuar separado; pode operar o monorepo, mas não integra o produto |

Ordem operacional: criar `signallq-platform` com estrutura, proteção e CI inicial; migrar um repositório por vez preservando histórico; validar build e comportamento equivalentes; congelar novas features no antigo; permitir apenas hotfix emergencial na janela de transição; publicar pelo novo pipeline; arquivar o antigo e atualizar o README com o novo endereço. Plano completo em fases 0–9 no `02_SignallQ_Platform_Especificacao_Tecnica_v5.md`.

## 16. Critérios de aceite da governança

A governança será considerada implantada quando:

- A main estiver protegida e sem escrita direta.
- Os workflows forem acionados por impacto de diretório.
- Mudanças em módulos compartilhados validarem todos os consumidores.
- Cada produto possuir pipeline e versão independentes.
- CODEOWNERS e templates de PR estiverem ativos.
- Agentes receberem escopo permitido, áreas proibidas e testes obrigatórios.
- Drivers experimentais permanecerem isolados no SignallQ Nethal.
- `packages/api-contracts` e `packages/telemetry-schema` tiverem testes de contrato ativos.
- Migrations D1 seguirem expand-migrate-contract com dry-run no CI.
- Repositórios antigos forem arquivados apenas após publicação pelo monorepo.

## 17. Checklist antes do merge

| Verificação | Obrigatório |
|---|---|
| O PR resolve uma única mudança identificável? | Sim |
| Arquivos alterados estão dentro do escopo declarado? | Sim |
| Os consumidores dos módulos compartilhados foram testados? | Sim |
| Há mudança de contrato, schema ou migration? | Revisão específica |
| Feature incompleta está protegida por flag e ainda funcional? | Quando aplicável |
| Evidências e comandos executados constam no PR? | Sim |
| Não há segredo, credencial ou dado de cliente versionado? | Sim |
| Release ou deploy afetará somente o produto correto? | Sim |
| Issues fora do escopo foram registradas sem alteração oportunista? | Sim |

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes e decisões (prevalece sobre este).
- `01_SignallQ_Platform_Arquitetura_v5.md` — visão consolidada, portfólio, árvore do monorepo e serviços de plataforma.
- `02_SignallQ_Platform_Especificacao_Tecnica_v5.md` — leitura dos repos, consolidação, backend, dados, plano de migração em fases e ADRs.
