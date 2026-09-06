---
title: "Índice — documentação SignallQ"
description: "Mapa de todos os documentos existentes em docs_ai, gerado a partir do disco"
type: "índice"
status: "ativo"
owner: "Squad"
last_updated: "2026-08-15"
---

# Índice da documentação

**111 documentos.** Escopo: app consumer Android e backend Cloudflare, mais a foundation do produto
Linka em preparação (`foundation-linka/`, ver seção própria abaixo). Perímetro e o que saiu em
2026-08-06 e 2026-08-15 estão em [`README.md`](README.md).

> ✅ **Canônicos regenerados do código em 2026-08-06 (PR 2).** `TECNICO.md` e
> `ARQUITETURA/README.md` carregam um bloco de inventário **gerado** por
> `scripts/gerar-inventario-docs.sh` — versões, módulos, workers, tabelas D1 e contagem de
> endpoints saem direto do código, e o CI reprova se divergirem. Não edite esse bloco à mão.

---

## Começar por aqui

1. [`../AGENTS.md`](../AGENTS.md) — o que é o SignallQ, stack, agentes
2. [`POSICIONAMENTO_PRODUTO.md`](POSICIONAMENTO_PRODUTO.md) — posicionamento obrigatório para Android e Web/PWA
3. [`design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md`](design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md) — direção futura de identidade e experiência
4. [`functional/JORNADA_ANDROID_GUIADA_2_SPEC.md`](functional/JORNADA_ANDROID_GUIADA_2_SPEC.md) — arquitetura futura da jornada Android
5. [`prototypes/signallq-android-2-0/`](prototypes/signallq-android-2-0/) — protótipo navegável da Jornada 2.0 (abrir `index.html`)
6. [`README.md`](README.md) — perímetro e mapa das pastas
7. [`FUNCIONAL.md`](FUNCIONAL.md) — o que o app faz
8. [`TECNICO.md`](TECNICO.md) — como é construído
9. [`ARQUITETURA/README.md`](ARQUITETURA/README.md) — módulos e dependências

---

## Canônicos

| Documento | Estado |
|---|---|
| [POSICIONAMENTO_PRODUTO.md](POSICIONAMENTO_PRODUTO.md) | ✅ diretriz de produto aprovada para Android e Web/PWA |
| [TECNICO.md](TECNICO.md) | ✅ reescrito do código · inventário gerado |
| [ARQUITETURA/README.md](ARQUITETURA/README.md) | ✅ reescrito do código · inventário gerado |
| [FUNCIONAL.md](FUNCIONAL.md) | ✅ reescrito do código · 4 raízes, 16 overlays, 77 citações de código |
| [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) | ✅ tokens conferidos 1 a 1 em `SignallQTheme.kt` |
| [RELEASES.md](RELEASES.md) | não regenerado — histórico de releases, sai do git |
| [plano-execucao-consumer-consolidado-2026-08-05.md](plano-execucao-consumer-consolidado-2026-08-05.md) | plano ativo, fila do Consumer em 7 ondas |

## Arquitetura por módulo — `ARQUITETURA/MODULOS/`

**20 documentos — um por módulo consumer.** Mesmo template: responsabilidade, dependências,
consumidores, componentes principais, riscos e dívidas.

`app` · `core-database` · `core-datastore` · `core-diagnostico` · `core-featureflags` ·
`core-nds` · `core-network` · `core-permissions` · `core-recommendation` · `core-relatorio` ·
`core-telephony` · `feature-devices` · `feature-diagnostico` · `feature-dns` · `feature-fibra` ·
`feature-history` · `feature-home` · `feature-settings` · `feature-speedtest` · `feature-wifi`.

`core-nds` (fatia NDS-01, issue #1744, ADR-017) é o mais novo — cliente HTTP e contrato tipado do
Network Diagnostics Service, isolado, sem consumidor real ainda.

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
`ADR-004` estrutura multi-módulo · `ADR-005` custo de IA e fallback · `ADR-008` features D1-only ·
`ADR-009` vocabulário de diagnóstico · `ADR-011` motor canônico fase 0 · `ADR-012` executionId/
rulesVersion · `ADR-013` unificação latência/perda/upload · **`ADR-016` portfólio Buildea:
SignallQ + Linka como produtos comerciais separados, descontinua Pro/ISP/Nethal, squad de 3
agentes com personalidade** · **`ADR-017` motor de diagnóstico e IA migram para o NDS, substitui
`core/diagnostico`, `ai-diagnosis-worker` e `signallq-diagnostic-worker` (shadow)** ·
**`ADR-018` schema canônico `DiagnosticSnapshot` do payload NDS — nome/tipo/opcionalidade/origem
dos 16 blocos, convenção de proveniência (`source`) e `capabilities` vs. campo presente**.

**Removidos na Fase 3 do épico [#1623](https://github.com/buildea-labs/signallq/issues/1623)
em 2026-08-15** (consolidados no ADR-016 ou superseded; git preserva por SHA):
`ADR-006` workflow squad 5 (superseded); `ADR-007` iOS adiado (consolidado); `ADR-010` monetização
consumer (consolidado); `ADR-014` squad canônico ai-governance (superseded); `ADR-015` plataformas
Android+Web (consolidado).

**Decisões de negócio (9) — removidas na Fase 4d do épico
[#1623](https://github.com/buildea-labs/signallq/issues/1623) em 2026-08-15:** eram registro de
decisão organizacional/operacional que nunca foram ADRs (consolidação do squad, cronograma de
lançamento, modelo de dados de avaliações Google Play, modelo de dados de integrações
Play/Firebase, status de credenciais, mudanças de equipe, `NOTA_DIVERGENCIA_GITHUB_PROJECTS`) —
git preserva por SHA quem precisar recuperar. O conteúdo técnico de schema (avaliações/integrações
Google Play e Firebase) já vive nos comentários das migrations reais em
`integrations/cloudflare/signallq-admin-worker/migrations/016_gh1342_gh1344_integration_history.sql`
e `017_gh1341_google_play_reviews.sql`; o cronograma de lançamento vive na issue
[#1222](https://github.com/buildea-labs/signallq/issues/1222).

> Próximo número livre de ADR: **017**.

## Foundation Linka — `foundation-linka/` (3)

Material provisório do produto Linka (ADR-016), preparado na Fase 8 do épico
[#1623](https://github.com/buildea-labs/signallq/issues/1623) para migrar quando o repositório
`buildea-labs/linka` for criado. Nada aqui executa neste repo — é template e checklist.

`README.md` (propósito da pasta e instruções de migração) · `AGENTS.md.template` (template do
`AGENTS.md` do repo Linka — extensão `.md.template`, não conta na contagem de documentos) ·
`squad-template.md` (rascunho das 3 personas do squad Linka) ·
`skills-apple-checklist.md` (skills Apple a criar no repo novo).

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

## Referências técnicas — `technical/` (16)

`admin-api-schema.md` (schema do worker `signallq-admin`, validado 2026-08-04) ·
`analytics-events.md` · `analytics-events-schema.md` · `AI_FLOW.md` ·
`PING_EXECUTOR_ARCHITECTURE.md` · `MONITORAMENTO_PASSIVO.md` · `feature-flags-remote-config.md` ·
`auditoria-motores-diagnostico-e-analise.md` · `SCREEN_MAP.md` ·
`PARIDADE_REC_WORKER_2026-07-26.md` · `P2_AMBIENTE_D1_ADMIN_SEPARACAO.md` ·
`INTELBRAS_RX1500_FIELD_MAP.md` · `NOKIA_GPON_FIELD_MAP.md` · `TPLINK_ARCHER_ROUTER_FIELD_MAP.md`
· `MATRIZ_DIAGNOSTICO_2026-07-03.xlsx` · `appshell-overlay-registry.md` (padrão de extensão de
overlays do `AppShell.kt`, issue #1695) · `appshell-root-content-registry.md` (padrão irmão para
root content/raízes, issue #1698 — cobre os ~85% do crescimento que o de overlays não cobria).

## Funcional pontual — `functional/` (3)

`FEATURE_FLAGS.md` · `DIAGNOSTICO_GUIADO_MODO_GAMER_SPEC.md` ·
`JORNADA_ANDROID_GUIADA_2_SPEC.md` (draft da jornada futura orientada por sintomas).

## Design — `design-system/` (12)

Decisões de design de 2026-07: alinhamento TOBE, cores do console, container de logo, topbar padrão,
renomeação SignallQ Design, separação DS/protótipos, três seções do console, tokens MD3, plano de
aplicação, auditoria de telas, endosso de marca. Conteúdo implementado no Android consolidado em
`DESIGN_SYSTEM.md`; direção futura compartilhada em `SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md` (draft).

## Protótipos — `prototypes/` (0 documentos na raiz; 3 pacotes)

`signallq-android-2-0/` — **protótipo navegável da Jornada Android 2.0** (`README.md` ·
`COVERAGE.md` · `NAVIGATION_AUDIT.md` · `brand-spec.md` + `index.html`). Referência visual e de
navegação do épico #1647, contra a qual cada fatia é comparada. Versionado em 2026-08-16; antes
disso existia só numa máquina, fora de repositório.

`signallq-design-system-2-board/README.md` — prancha visual de foundations e componentes do
Design System 2.0.

`open-design-signallq-android-v2/README.md` · `PROMPT_INICIAL.md` ·
`CHECKLIST_REVISAO.md`. Pacote operacional para gerar e revisar a primeira rodada no Open Design;
não substitui as especificações canônicas.

## Legal — `legal/` (2) · **não editar sem revisão**

`PRIVACY_POLICY.md` · `TERMS_OF_USE.md`.

## Testes — `testing/`

`firebase-test-cases.yaml` — casos de teste do Firebase Test Lab. Único artefato da pasta e não é
Markdown, por isso não aparece nas contagens de documento.

## Templates — `templates/` (5)

`README.md` · `TEMPLATE_TECNICO.md` · `TEMPLATE_FUNCIONAL.md` · `TEMPLATE_ADR.md` ·
`TEMPLATE_RUNBOOK.md`.

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
| — | ~~Caminho físico legado `io/veloo` em 525 arquivos `.kt`~~ — RESOLVIDO em 2026-08-15 (#1645) | `.claude/rules/higiene…§4.1` |

## Manutenção

Política em [`.claude/rules/politica-documentacao-viva.md`](../.claude/rules/politica-documentacao-viva.md).
Documento substituído é **removido**, não arquivado. Próxima auditoria: **2026-11-06**.
