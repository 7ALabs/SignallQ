---
title: "Índice Central de Documentação — SignallQ"
description: "Mapa de toda documentação viva do projeto, com responsáveis e status"
type: "índice"
version: "1.0.0"
last_updated: "2026-08-05"
consolidacao: "Fase 1-3 concluída em 2026-08-05 (ver commits 98204fcf + 3099b3d3)"
owner: "Squad"
status: "ativo"
---

# 📚 Índice Central de Documentação — SignallQ

**Última atualização:** 2026-08-05  
**Mantido por:** Squad  
**Próxima revisão:** 2026-11-05

---

## 🎯 Começar aqui

**Novo no projeto?** Leia nesta ordem:

1. [`AGENTS.md`](../AGENTS.md) — O que é SignallQ, stack, agentes
2. [`docs_ai/README.md`](README.md) — Visão geral rápida
3. [`docs_ai/FUNCIONAL.md`](FUNCIONAL.md) — O que o app faz
4. [`docs_ai/TECNICO.md`](TECNICO.md) — Como é construído
5. [`docs_ai/ARQUITETURA/README.md`](ARQUITETURA/README.md) — Estrutura de módulos

---

## 📋 Documentação por tipo

### Funcional (jornadas, features, critérios)

| Documento | Status | Owner | Última atualização |
|---|---|---|---|
| [**FUNCIONAL.md**](FUNCIONAL.md) | ✅ Ativo | Claudete | 2026-08-04 |
| [functional/FEATURE_FLAGS.md](functional/FEATURE_FLAGS.md) | ✅ Ativo | Claudete | 2026-07-28 |
| [functional/DIAGNOSTICO_GUIADO_MODO_GAMER_SPEC.md](functional/DIAGNOSTICO_GUIADO_MODO_GAMER_SPEC.md) | ✅ Ativo | Claudete | 2026-07-15 |

---

### Técnico (arquitetura, engines, motores)

| Documento | Status | Owner | Última atualização |
|---|---|---|---|
| [**TECNICO.md**](TECNICO.md) | ✅ Ativo | Camilo | 2026-08-04 |
| [**ARQUITETURA/README.md**](ARQUITETURA/README.md) | ✅ Ativo | Camilo | 2026-08-01 |
| [ARQUITETURA/MODULOS/core-diagnostico.md](ARQUITETURA/MODULOS/core-diagnostico.md) | ✅ Ativo | Camilo | 2026-07-20 |
| [ARQUITETURA/MODULOS/feature-diagnostico.md](ARQUITETURA/MODULOS/feature-diagnostico.md) | ✅ Ativo | Camilo | 2026-07-20 |
| [technical/auditoria-motores-diagnostico-e-analise.md](technical/auditoria-motores-diagnostico-e-analise.md) | ✅ Ativo | Camilo | 2026-08-05 |
| [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) | ✅ Ativo | Juliana | 2026-07-30 |

---

### Decisões (ADRs)

| Documento | Status | Owner | Versão |
|---|---|---|---|
| [**decisions/README.md**](decisions/README.md) | ✅ Ativo | Claudete | — |
| [decisions/ADR-001-shadow-mode-remote-diagnostics.md](decisions/ADR-001-shadow-mode-remote-diagnostics.md) | ✅ Aceito | Claudete | 1.0 |
| [decisions/ADR-013-consolidate-feature-flags.md](decisions/ADR-013-consolidate-feature-flags.md) | ✅ Aceito | Claudete | 1.2 |

---

### Operações (runbooks, deploy, release)

| Documento | Status | Owner | Severidade |
|---|---|---|---|
| [**operations/README.md**](operations/README.md) | ✅ Ativo | Gustavo | — |
| [operations/RELEASE_ANDROID.md](operations/RELEASE_ANDROID.md) | ✅ Ativo | Camilo | P1 |
| [operations/DEPLOY_WORKERS.md](operations/DEPLOY_WORKERS.md) | ✅ Ativo | Camilo | P1 |
| [operations/INCIDENT_RESPONSE.md](operations/INCIDENT_RESPONSE.md) | ✅ Ativo | Gustavo | P1 |
| [operations/INFRASTRUCTURE_COSTS.md](operations/INFRASTRUCTURE_COSTS.md) | ✅ Ativo | Gustavo | P2 |

---

### Legal / Compliance

| Documento | Status | Owner | Última atualização |
|---|---|---|---|
| [legal/PRIVACIDADE.md](legal/PRIVACIDADE.md) | ✅ Ativo | Claudete | 2026-07-10 |
| [legal/TERMOS_SERVICO.md](legal/TERMOS_SERVICO.md) | ✅ Ativo | Claudete | 2026-07-10 |

---

### Contratos / OpenAPI

| Documento | Status | Owner | Última validação |
|---|---|---|---|
| [CONTRATOS/README.md](CONTRATOS/README.md) | ⚠️ Parcial | Camilo | 2026-08-05 |
| [CONTRATOS/openapi/signallq-diagnostic-worker.yaml](CONTRATOS/openapi/signallq-diagnostic-worker.yaml) | ✅ Sincronizado | Camilo | 2026-08-05 |
| [CONTRATOS/openapi/signallq-admin-api.yaml](CONTRATOS/openapi/signallq-admin-api.yaml) | ⚠️ Incompleto | Camilo | 2026-06-15 |
| [CONTRATOS/openapi/signallq-analytics-events.yaml](CONTRATOS/openapi/signallq-analytics-events.yaml) | ❌ Fictício | Camilo | — |
| [CONTRATOS/openapi/signallq-integrations-api.yaml](CONTRATOS/openapi/signallq-integrations-api.yaml) | ❌ Fictício | Camilo | — |

---

### Templates (meta-documentação)

| Documento | Status | Uso |
|---|---|---|
| [templates/README.md](templates/README.md) | ✅ Ativo | Guia para criar novas documentações |
| [templates/TEMPLATE_TECNICO.md](templates/TEMPLATE_TECNICO.md) | ✅ Ativo | Copiar para novos docs técnicos |
| [templates/TEMPLATE_FUNCIONAL.md](templates/TEMPLATE_FUNCIONAL.md) | ✅ Ativo | Copiar para novos docs funcionais |
| [templates/TEMPLATE_ADR.md](templates/TEMPLATE_ADR.md) | ✅ Ativo | Copiar para novos ADRs |
| [templates/TEMPLATE_RUNBOOK.md](templates/TEMPLATE_RUNBOOK.md) | ✅ Ativo | Copiar para novos runbooks |

---

### Planos / Roadmaps

| Documento | Status | Tipo |
|---|---|---|
| [plano-execucao-consumer-consolidado-2026-08-05.md](plano-execucao-consumer-consolidado-2026-08-05.md) | ✅ Ativo | Plano de execução Consumer 2026 |

---

## 🚨 Documentação legada / desatualizada

| Documento | Motivo | Ação |
|---|---|---|
| `_archive/2026-07-16_ANDROID_FUNCIONAL.md` | Substituído por FUNCIONAL.md | Arquivado |
| `_archive/2026-07-16_SCREENS_ANDROID.md` | Telas removidas (JogosScreen) | Arquivado |
| `.issues/` (24 arquivos) | Backlog em markdown obsoleto | Mover para `_archive/` |

---

## 📊 Status da documentação

### Saúde geral

| Categoria | Ativo | Draft | Legado | Taxa atualização |
|---|---|---|---|---|
| Funcional | 3 | 0 | 1 | 100% |
| Técnico | 6 | 0 | 3 | 90% |
| Decisões | 13 | 0 | 0 | 100% |
| Operações | 5 | 0 | 0 | 80% |
| Contratos | 2/5 | 0 | 0 | 40% |

### Problemas conhecidos

🔴 **CRÍTICO:**
- [ ] Contratos OpenAPI 3 e 4 são fictícios (não correspondem ao código real)
- [ ] CLAUDE.md da raiz está vazio (12 bytes)
- [ ] `.issues/` backlog duplica GitHub Issues

🟡 **IMPORTANTE:**
- [ ] `docs_ai/_archive/` tem 90+ documentos antigos (limpeza necessária)
- [ ] `signallq-admin-api.yaml` incompleto (dezenas de rotas reais não documentadas)
- [ ] Fragmentação: specs espalhadas em `functional/`, `technical/`, `plataforma/`

🟢 **LEVE:**
- [ ] Alguns runbooks não foram executados há >3 meses (rotação de ondas anterior)
- [ ] Path legado `io/veloo` em 460 arquivos Android (documentação cita ambos)

---

## 🔄 Política de manutenção

### Cadência

**Semanal:** Revisar documentação que será tocada pela sprint  
**Mensal:** Validar que runbooks ainda funcionam  
**Trimestral:** Auditoria completa (este documento)  
**Quando código muda:** Atualizar doc relacionada na mesma PR

### Responsabilidades

| Quem | O quê |
|---|---|
| **Claudete** | FUNCIONAL.md, ADRs, decisões |
| **Camilo** | TECNICO.md, ARQUITETURA, contratos |
| **Juliana** | DESIGN_SYSTEM.md |
| **Gustavo** | operations/, métricas |
| **Caio** | Revisar mudanças críticas |

---

## 🆘 Links rápidos

**Para contribuidores:**
- [Como criar nova documentação?](templates/README.md)
- [Checklist de manutenção](templates/README.md#-checklist-manter-documentação-viva)

**Para operações:**
- [Runbooks](operations/README.md)
- [Release Android](operations/RELEASE_ANDROID.md)
- [Deploy Workers](operations/DEPLOY_WORKERS.md)

**Para arquitetura:**
- [ADRs](decisions/README.md)
- [Módulos](ARQUITETURA/MODULOS/)
- [Auditoria de motores](technical/auditoria-motores-diagnostico-e-analise.md)

**Para produto:**
- [Funcionalidades](FUNCIONAL.md)
- [Design System](DESIGN_SYSTEM.md)

---

## 📈 Próximas ações (Onda 0 de documentação)

**Semana 1 (imediato):**
- [ ] Atualizar CLAUDE.md da raiz (ou deletar)
- [ ] Arquivar `.issues/` backlog para `_archive/issues-backlog-2026-06/`
- [ ] Adicionar README em `.agents/skills/` e `.github/skills/` explicitando que são gerados
- [ ] Criar MEMORY.md na raiz com sumário executivo

**Semana 2-3:**
- [ ] Revisar/limpar `_archive/` (remover cópias de 2026-07-16)
- [ ] Consolidar specs de `functional/` em FUNCIONAL.md principal
- [ ] Fixar contratos fictícios (#1587/#1588)
- [ ] Validar runbooks (executar cada um)

**Semana 4+:**
- [ ] Abrir issue para eliminar path `io/veloo` (escopo grande)
- [ ] Abrir issue para extrair HomeScreen/SinalScreen (refatoração)
- [ ] Arquivar personas extintas em `docs/archive/ai-governance/legacy-agents/`

---

## 🔗 Conexões

**Documentação relacionada em outros repos:**
- `ai-governance/` — políticas gerais de governança
- `buildea-admin/AGENTS.md` — escopo do repositório Admin
- `signallq-web/docs_ai/` — documentação web (compartilhando arquitetura)

---

**Este índice mantém-se vivo.** Atualizado toda vez que documentação é criada, deletada ou movida.

**Última atualização:** 2026-08-05  
**Próxima revisão agendada:** 2026-11-05  
**Mantido por:** Squad SignallQ
