---
title: "Índice — documentação SignallQ"
description: "Mapa de todos os documentos existentes em docs_ai, gerado a partir do disco"
type: "índice"
status: "ativo"
owner: "Squad"
last_updated: "2026-08-06"
---

# Índice da documentação

**116 documentos.** Escopo: app consumer Android e backend Cloudflare. Perímetro e o que saiu em
2026-08-06 estão em [`README.md`](README.md).

> ✅ **Canônicos regenerados do código em 2026-08-06 (PR 2).** `TECNICO.md` e
> `ARQUITETURA/README.md` carregam um bloco de inventário **gerado** por
> `scripts/gerar-inventario-docs.sh` — versões, módulos, workers, tabelas D1 e contagem de
> endpoints saem direto do código, e o CI reprova se divergirem. Não edite esse bloco à mão.

---

## Começar por aqui

1. [`../AGENTS.md`](../AGENTS.md) — o que é o SignallQ, stack, agentes
2. [`README.md`](README.md) — perímetro e mapa das pastas
3. [`FUNCIONAL.md`](FUNCIONAL.md) — o que o app faz
4. [`TECNICO.md`](TECNICO.md) — como é construído
5. [`ARQUITETURA/README.md`](ARQUITETURA/README.md) — módulos e dependências

---

## Canônicos

| Documento | Estado |
|---|---|
| [TECNICO.md](TECNICO.md) | ✅ reescrito do código · inventário gerado |
| [ARQUITETURA/README.md](ARQUITETURA/README.md) | ✅ reescrito do código · inventário gerado |
| [FUNCIONAL.md](FUNCIONAL.md) | ✅ reescrito do código · 5 abas, 16 overlays, 77 citações de código |
| [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) | ✅ tokens conferidos 1 a 1 em `SignallQTheme.kt` |
| [RELEASES.md](RELEASES.md) | não regenerado — histórico de releases, sai do git |
| [plano-execucao-consumer-consolidado-2026-08-05.md](plano-execucao-consumer-consolidado-2026-08-05.md) | plano ativo, fila do Consumer em 7 ondas |

## Arquitetura por módulo — `ARQUITETURA/MODULOS/`

**19 documentos — um por módulo consumer, todos reescritos do código em 2026-08-06.** Mesmo
template: responsabilidade, dependências, consumidores, componentes principais, riscos e dívidas.

`app` · `core-database` · `core-datastore` · `core-diagnostico` · `core-featureflags` ·
`core-network` · `core-permissions` · `core-recommendation` · `core-relatorio` · `core-telephony` ·
`feature-devices` · `feature-diagnostico` · `feature-dns` · `feature-fibra` · `feature-history` ·
`feature-home` · `feature-settings` · `feature-speedtest` · `feature-wifi`.

Os três que faltavam (`core-relatorio`, `core-diagnostico`, `core-featureflags`) foram criados.
Módulos `:pro:*` não têm documento: estão on hold.

Também em `ARQUITETURA/`: `AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`.

## Contratos — `CONTRATOS/`

7 contratos OpenAPI 3.0.3, **122 endpoints** (`CONTRATOS/openapi/`):

| Contrato | Versão | Paths |
|---|---|---:|
| `signallq-admin-api.yaml` | 2.1.0 | 59 |
| `signallq-diagnostic-worker.yaml` | 1 | 43 |
| `signallq-integrations-api.yaml` | 1.0.0 | 9 |
| `signallq-analytics-events.yaml` | 1.0.0 | 5 |
| `ai-diagnosis-worker.yaml` | 2 | 2 |
| `game-latency-probe-worker.yaml` | 1 | 2 |
| `signallq-privacy-worker.yaml` | 1 | 2 |

Mais `CONTRATOS/schemas/README.md`. A reconciliação dos dois últimos contratos transversais com o
código real é a issue **#1588**.

## Decisões — `decisions/` · **preservadas, não regeneráveis**

**ADRs (13):** `ADR-001` Timber · `ADR-002` Ktlint/Detekt · `ADR-003` DispatcherProvider ·
`ADR-004` estrutura multi-módulo · `ADR-005` custo de IA e fallback · `ADR-006` workflow do squad ·
`ADR-007` iOS adiado · `ADR-008` features D1-only · `ADR-009` vocabulário de diagnóstico ·
`ADR-010` monetização do consumer · `ADR-011` motor canônico fase 0 · `ADR-012` executionId/
rulesVersion · `ADR-013` unificação latência/perda/upload.

**Decisões de negócio (9):** consolidação do squad, cronograma de lançamento, modelo de dados de
avaliações Google Play, modelo de dados de integrações Play/Firebase, status de credenciais,
mudanças de equipe, e `NOTA_DIVERGENCIA_GITHUB_PROJECTS`.

> Próximo número livre de ADR: **014**.

## Operações — `operations/` (26)

Release e build: `RELEASE.md`, `DEPLOY.md`, `GuiaReleaseBuild.md`, `APK_OUTPUT_POLICY.md`,
`VERSIONING.md`, `SIGNING.md`, `ci-cd.md`, `SCRIPTS.md`.
Incidente e continuidade: `HOTFIX_PROCEDURE.md`, `ROLLBACK_PLAN.md`, `ROLLOUT_TRANSITION.md`,
`HYPERCARE_PLAN.md`, `INCIDENTE_BYPASS_BLOQUEIO_SEGURANCA_2026-07-20.md`, `MAINTENANCE_PLAN.md`.
Qualidade e lançamento: `GO_NOGO_CHECKLIST.md`, `BETA_CRITERIA.md`, `DEVICE_TEST_MATRIX.md`,
`MANIFEST_AUDIT.md`, `PLAY_STORE_LISTING.md`, `ENVIRONMENTS.md`, `INFRASTRUCTURE_COSTS.md`.
Processo: `PROCESSO_PR_E_AGENTES_2026-07-16.md`, `WORKFLOW_BOARD.md`, `FAQ_USERS.md`,
`THIRD_PARTY_NOTICES.md`, `RUNBOOK_LAUNCH.md`.

> Apenas `RELEASE.md` teve as referências conferidas (2026-08-05). Os demais estão marcados "ativo"
> sem histórico de execução. Consolidação de 26 → ~12 fica para o PR 2.

## Referências técnicas — `technical/` (14)

`admin-api-schema.md` (schema do worker `signallq-admin`, validado 2026-08-04) ·
`analytics-events.md` · `analytics-events-schema.md` · `AI_FLOW.md` ·
`PING_EXECUTOR_ARCHITECTURE.md` · `MONITORAMENTO_PASSIVO.md` · `feature-flags-remote-config.md` ·
`auditoria-motores-diagnostico-e-analise.md` · `SCREEN_MAP.md` ·
`PARIDADE_REC_WORKER_2026-07-26.md` · `P2_AMBIENTE_D1_ADMIN_SEPARACAO.md` ·
`INTELBRAS_RX1500_FIELD_MAP.md` · `NOKIA_GPON_FIELD_MAP.md` · `TPLINK_ARCHER_ROUTER_FIELD_MAP.md`
· `MATRIZ_DIAGNOSTICO_2026-07-03.xlsx`.

## Funcional pontual — `functional/` (2)

`FEATURE_FLAGS.md` · `DIAGNOSTICO_GUIADO_MODO_GAMER_SPEC.md`.

## Design — `design-system/` (11)

Decisões de design de 2026-07: alinhamento TOBE, cores do console, container de logo, topbar padrão,
renomeação SignallQ Design, separação DS/protótipos, três seções do console, tokens MD3, plano de
aplicação, auditoria de telas, endosso de marca. Conteúdo vigente consolidado em `DESIGN_SYSTEM.md`.

## Legal — `legal/` (2) · **não editar sem revisão**

`PRIVACY_POLICY.md` · `TERMS_OF_USE.md`.

## Testes — `testing/`

`firebase-test-cases.yaml` — casos de teste do Firebase Test Lab. Único artefato da pasta e não é
Markdown, por isso não aparece nas contagens de documento.

## Templates — `templates/` (5)

`README.md` · `TEMPLATE_TECNICO.md` · `TEMPLATE_FUNCIONAL.md` · `TEMPLATE_ADR.md` ·
`TEMPLATE_RUNBOOK.md`.

## Congelado — `pro-onhold/` (7)

Specs do SignallQ Pro, **on hold por tempo indeterminado** até a maturação do consumer em produção.
Não manter. Ver [`pro-onhold/README.md`](pro-onhold/README.md).

## Vazio por decisão — `_archive/`

Ver [`_archive/README.md`](_archive/README.md) para recuperar qualquer documento removido.

---

## Dívidas conhecidas

| # | Dívida | Onde |
|---|---|---|
| **#1585** | `/ingest/provider-detection` e `/ingest/diagnostic-divergence` aceitam POST anônimo — o padrão `INGEST_KEY` já existe no admin-worker e não foi aplicado | `signallq-diagnostic-worker/src/index.ts:1141,1145` |
| **#1586** | `MetricClassifier` não usado em `SinalScreen.kt`; limiares duplicados em três lugares | Android + worker |
| **#1587** | `auth.ts` duplicado byte-a-byte entre admin-worker e diagnostic-worker | `integrations/cloudflare/*/src/auth.ts` |
| **#1588** | OpenAPI transversais a reconciliar com o código | `CONTRATOS/openapi/` |
| — | `TECNICO.md` e `ARQUITETURA/README.md` com inventário defasado | PR 2 |
| — | Caminho físico legado `io/veloo` em ~460 arquivos `.kt` | `.claude/rules/higiene…§4.1` |

## Manutenção

Política em [`.claude/rules/politica-documentacao-viva.md`](../.claude/rules/politica-documentacao-viva.md).
Documento substituído é **removido**, não arquivado. Próxima auditoria: **2026-11-06**.
