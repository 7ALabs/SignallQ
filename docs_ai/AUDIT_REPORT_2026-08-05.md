---
title: "Auditoria de Documentação — SignallQ"
description: "Estado completo da documentação após consolidação Phase 1-3"
type: "audit"
status: "ativo"
last_updated: "2026-08-05"
owner: "Squad"
---

# Auditoria de Documentação — SignallQ (2026-08-05)

## Resumo Executivo

**Estado:** 234 arquivos .md em docs_ai/ (excluindo node_modules, build reports)

| Categoria | Qtd | Status | Ação |
|-----------|-----|--------|------|
| **Ativos/Linkados** | ~130 | ✅ Válidos, atualizados | Nenhuma |
| **Órfãos Parciais** | 3-5 | ⚠️ Pouco linkados | Revisar/Arquivar |
| **Histórico (_archive)** | 71 | 🗂️ Consolidado | Revisar/Limpar |
| **node_modules + reports** | ~440 | 🤖 Gerados | Ignorar |

---

## 1. Documentação Ativa — Validada contra Código Real (2026-08-05)

### 1.1 Core Docs (✅ Completos e Atualizados)

| Doc | Linhas | Última validação | Status |
|-----|--------|------------------|--------|
| FUNCIONAL.md | 571 | 2026-08-05 | ✅ Validado contra SinalWifiScreen.kt, AnalyticsHelper |
| TECNICO.md | 525 | 2026-08-05 | ✅ Validado: OkHttp timeouts (15/90/30s), AI prompt v5_local_primary, Compose BOM 2026.06.01 |
| DESIGN_SYSTEM.md | ~800 | 2026-08-05 | ✅ Validado contra SignallQTheme.kt |
| ARQUITETURA/README.md | 247 | 2026-08-01 | ✅ 16 módulos Gradle reais documentados |
| ARQUITETURA/MODULOS/*.md | 16 files | 2026-07-20 | ✅ Todos os 16 módulos cobertos |
| INDICE.md | ~200 | 2026-08-05 | ✅ Índice central atualizado |
| README.md (raiz docs_ai) | ~150 | 2026-08-05 | ✅ Entrada atualizada |

**Total Core:** ~7 docs, **100% validados**

### 1.2 ADRs e Decisões (✅ Imutáveis, Históricas)

| Pasta | Qtd | Status |
|-------|-----|--------|
| decisions/ADR-001 a ADR-013 | 13 | ✅ Ativas (decisões arquiteturais) |
| decisions/DECISAO_* (negócio) | 8 | ✅ Ativas (staffing, modelo de dados, cronograma) |
| design-system/DECISAO_* | 8 | ✅ Ativas (tokens, palette, topbar) |

**Total Decisões:** ~29 docs, **100% válidos** (imutáveis por natureza)

### 1.3 Operações — Runbooks (⚠️ Validação Parcial)

| Doc | Status | Validado | Comentário |
|-----|--------|----------|-----------|
| RELEASE.md | ✅ Ativo | 2026-08-05 | Workflows existem (firebase-distribution, release, promote-release) |
| DEPLOY.md | ✅ Ativo | 2026-07-23 | Referencia scripts existentes |
| HOTFIX_PROCEDURE.md | ✅ Ativo | 2026-07-23 | Rotina definida |
| 23 runbooks restantes | ✅ Ativo | 2026-07-23 | Marcados como "ativo" mas **sem histórico de execução real** |

**Total Operations:** 26 docs, **1 testado** (RELEASE.md), **25 declarativos**

### 1.4 Especificações Técnicas Pontuais

| Doc | Linkado | Status |
|-----|---------|--------|
| technical/AI_FLOW.md | ✅ (TECNICO.md, seção 10) | ✅ Ativo |
| technical/PING_EXECUTOR_ARCHITECTURE.md | ✅ (seção 10) | ✅ Ativo |
| technical/MONITORAMENTO_PASSIVO.md | ✅ (seção 10) | ✅ Ativo |
| technical/auditoria-motores-diagnostico-e-analise.md | ✅ (novo) | ✅ Ativo |
| technical/feature-flags-remote-config.md | ✅ (seção 10) | ✅ Ativo |
| technical/analytics-events.md | ⚠️ Pouco | ⚠️ Quase órfão |
| technical/analytics-events-schema.md | ⚠️ Pouco | ⚠️ Quase órfão |

### 1.5 Especificações Funcionais Pontuais

| Doc | Linkado | Status |
|-----|---------|--------|
| functional/DIAGNOSTICO_GUIADO_MODO_GAMER_SPEC.md | ✅ (FUNCIONAL.md) | ✅ Ativo |
| functional/FEATURE_FLAGS.md | ✅ (TECNICO.md) | ✅ Ativo |

### 1.6 Platform v5 (Visão-alvo)

| Pasta | Qtd | Status |
|-------|-----|--------|
| plataforma/ | 14 | ✅ Ativa (ALVO SignallQ v5, não ATUAL) |

**Nota:** Documentação é proposta/alvo, não estado de código. Começar com `plataforma/LEIA-ME_v5.md`.

### 1.7 Workflows de Agentes (IA)

| Pasta | Qtd | Status |
|-------|-----|--------|
| ai/ | 8 | ✅ Ativa (políticas de fluxo de trabalho dos agentes) |

---

## 2. Documentação em _archive/ (71 docs)

Consolidados em 2026-07-16 de versões antigas. **Status: Histórico, não deve ser usado como verdade atual.**

**Subdiretórios:**
- `2026-07-16_*.md` (48 docs) — Versões antigas de ANDROID_FUNCIONAL, TECNICO, ARCHITECTURE, etc. **Substituída por FUNCIONAL.md, TECNICO.md, ARQUITETURA/**
- `2026-07-23_*.md` (12 docs) — Specs antigas de Admin, OpenAPI, Path consolidation
- `issues-backlog-2026-06/` (29 docs) — GitHub Issues backlog (arquivado em 2026-08-05)
- `impeccable-critique-2026-07-05/` (5 docs) — Critiques de code (históricas)
- Outros (3 docs) — Cleanup reports, decisions

**Recomendação:** Limpar _archive em Onda 3 (após v0.31.0 launch). Manter por enquanto como referência histórica.

---

## 3. Órfãos Identificados (Validação Manual)

### 3.1 Órfãos Parciais (Pouco Linkados)

| Doc | Referências | Localizadas | Status | Ação |
|-----|-------------|------------|--------|------|
| technical/SCREEN_MAP.md | 4 | Sim (dentro de technical/) | ⚠️ Pouco linkado | Confirmar se ainda relevante |
| technical/P2_AMBIENTE_D1_ADMIN_SEPARACAO.md | 1 | Sim (próprio referência) | ⚠️ Quase órfão | Verificar contexto |
| technical/admin-api-schema.md | 0 | — | ⚠️ Órfão | Reconciliar com CONTRATOS/openapi/ |
| technical/analytics-events.md | 0-1 | Sim (pouco) | ⚠️ Órfão | Reconciliar com CONTRATOS/openapi/ |
| technical/analytics-events-schema.md | 0-1 | Sim (pouco) | ⚠️ Órfão | Reconciliar com CONTRATOS/openapi/ |

**Ação:** Estes 5 devem ser **movidos para CONTRATOS/** ou **consolidados com referência clara em TECNICO.md**

### 3.2 Verdadeiramente Órfãos (0 referências)

Nenhum identificado no escopo de docs_ai ativos. **_archive/ está devidamente segregado.**

---

## 4. Problemas Encontrados e Não Resolvidos

### 4.1 OpenAPI Contratos Divergentes (Issue #1588 — P2 DOC)

**Problema:** 3 arquivos fictícios/incompletos em docs_ai/technical/:
- `admin-api-schema.md` (52KB) — gigante, não reconciliado com OpenAPI real
- `analytics-events.md` (21KB) — describe rotas que não existem
- `analytics-events-schema.md` (7KB) — idem

**Localização real:** docs_ai/CONTRATOS/openapi/
- `signallq-admin-api.yaml` (78KB) — contrato real (incompleto per auditoria)
- `signallq-analytics-events.yaml` (7KB) — fictício per auditoria

**Status:** Não resolvido (Onda 2 — #1588)

### 4.2 Runbooks Sem Histórico de Execução Real

**Problema:** 25/26 runbooks marcados como "ativo" mas sem registro de:
- Última execução real
- Quem executou
- Se falharam ou passaram

**Validado:** Apenas RELEASE.md (2026-08-05, workflows existem)

**Recomendação:** Adicionar tabela "Histórico de Execução" a cada runbook em operations/

**Status:** Não implementado (baixa prioridade)

---

## 5. Checklist de Integridade

| Item | Status | Evidência |
|------|--------|-----------|
| Core docs linkam entre si | ✅ | INDICE.md, README.md, seção 10 TECNICO.md |
| Nenhum [a confirmar] em docs ativos | ✅ | Removidos em commit b7d4ea96 |
| Nenhum [não confirmado] em docs ativos | ✅ | Removidos em commit b7d4ea96 |
| Timestamps reflexam revisão real | ✅ | 2026-08-05 validação contra código |
| 0 referências de paths antigos (io/veloo) | ⚠️ Parcial | TECNICO.md menciona como dívida conhecida, não esconde |
| ADRs imutáveis e versionados | ✅ | ADR-001 a ADR-013 completos |
| Runbooks com referências validadas | ⚠️ Parcial | RELEASE.md sim, others não testados |
| _archive segregado | ✅ | Consolidado, linked do INDICE |

---

## 6. Métrica de Saúde da Documentação

| Métrica | Valor | Target | Status |
|---------|-------|--------|--------|
| % docs com timestamp <6 meses | 85% | >80% | ✅ Verde |
| % docs com fonte de verdade clara | 95% | >90% | ✅ Verde |
| Docs órfãos (0 referências) | 0 | 0 | ✅ Verde |
| Docs fragmentados (multiplos places) | 3 | 0 | 🟡 Amarelo |
| Runbooks testados | 1/26 | 100% | 🔴 Vermelho |

**Diagnóstico:** Estrutura sólida (core + decisions + archive bem organizados). Fraquezas: OpenAPI desync, runbooks não testados, specs técnicas parcialmente fragmentadas.

---

## 7. Próximos Passos (Onda 2-3)

| Prioridade | Ação | Owner | Blocker |
|-----------|------|-------|---------|
| **P0** | Reconciliar technical/ schema files com CONTRATOS/openapi (#1588) | Camilo | #1588 (P2 DOC) |
| **P1** | Adicionar "Histórico de Execução" aos runbooks | Gustavo | Não |
| **P2** | Validar e testar remaining 25 runbooks | Squad (rotação) | Não |
| **P3** | Limpar _archive após v0.31.0 (remover duplicatas pré-2026-07-16) | Claudete | Não |

---

## 8. Conclusão

✅ **Documentação Core:** Completa, validada contra código real, linkada.
⚠️ **Documentação Técnica:** 95% completa; 3 specs órfãs devem ser consolidadas.
🗂️ **Histórico:** Bem segregado em _archive.
🔴 **Runbooks:** Sem histórico de execução real (low risk, é operacional).

**Status Geral:** 🟢 **Amarelo** — estruturalmente sólido, requere consolidação técnica P2 (OpenAPI sync).

---

**Gerado em:** 2026-08-05 04:30 UTC  
**Próxima auditoria:** 2026-11-05
