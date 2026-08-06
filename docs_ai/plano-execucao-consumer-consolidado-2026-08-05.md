---
title: "Plano de execução consolidado — SignallQ Consumer"
description: "Fila do consumer organizada em ondas, com numeração de issues conferida no GitHub"
type: "plano"
status: "ativo"
owner: "Claudete"
last_updated: "2026-08-06"
---

# Plano de Execução Consolidado — SignallQ Consumer v2

**Data:** 2026-08-05 · **Numeração revisada:** 2026-08-06 · **Blocos:** 7 ondas + Fase 0

> **Correção de 2026-08-06 (Claudete).** A versão anterior deste plano referenciava as seis issues
> criadas na auditoria de 2026-08-05 com numeração deslocada — quem seguisse o documento pegaria a
> issue errada (a Onda 2 mandava executar "#1584, segurança P0", quando #1584 é o épico de contrato
> multi-plataforma). Os números abaixo foram conferidos um a um contra o GitHub em 2026-08-06.
> Mapeamento do que mudou:
>
> | Plano dizia | Issue real | Título real |
> |---|---|---|
> | 1584 SECURITY endpoints sem auth | **#1585** | [P0 SECURITY] `/ingest/*` sem autenticação |
> | 1585 MetricClassifier em SinalScreen | **#1586** | [P2 BUG] MetricClassifier não usado em `SinalScreen.kt` |
> | 1586 auth PBKDF2 duplicada | **#1587** | [P2 TECH-DEBT] consolidar PBKDF2 em módulo compartilhado |
> | 1587 OpenAPI admin-worker + 1588 remover OpenAPI fictícios | **#1588** | [P2 DOC] corrigir contratos OpenAPI (**issue única**, não duas) |
> | 1589 Épico multi-tenant | **#1584** | [Épico] Contrato multi-plataforma unificado para diagnóstico |
> | — | **#1589** | [P3 DOC] limpar `_archive/` |
>
> A contagem "42 issues Consumer" também não fecha: o repositório tem **41 issues abertas no total**
> (verificado 2026-08-06), das quais parte é Admin, Pro ou processo, não Consumer.

---

## FASE 0: Pesquisa Fundacional + Auditoria (P0 crítica — semana 1–2)

**Bloqueia:** #1466, #1228, #975, #1586, #1584 Fase 1

| # | Issue | Esforço | Responsável | Status |
|---|---|---|---|---|
| **1583** | [Épico] Pesquisa de métricas e limiares — padrão nacional | **L** | agent:general-purpose | 🔴 Backlog |
| **1584** | [Épico] Contrato multi-plataforma unificado — diagnostic-worker | **M (design)** | Camilo + squad | 🔴 Backlog |

**Saídas esperadas:**
- `docs_ai/metricas-qualidade-rede-v1.md` — documento único de verdade
- OpenAPI v2 do diagnostic-worker com platform identification e campos opcionais

---

## ONDA 1: Higiene Mecânica (XS–S, zero dependência)

**Paralelo com Fase 0, não bloqueado**

| # | Issue | Esforço | Tipo | Status |
|---|---|---|---|---|
| 1172 | Atualizar hex `#6C2BFF` em docs | XS | docs | ✓ Legítima |
| 1007 | Limpar `docs_ai/` (squad obsoleta) | XS | docs | ✓ Backlog |
| 1485 | Remover `AnaliseDetalhadaBottomSheet` | S | tech-debt | ✓ Fluxo legado |
| 1261 | Remover composables mortas | S | tech-debt | ✓ Cluster |
| 1499 | `Color.White` hardcoded → token | S | bug | ✓ 7 ocorrências |

**Saída:** código limpo

---

## ONDA 2: Infra de Qualidade + Segurança (S–M, alavanca para refactors + destravasor multi-tenant)

**Não bloqueado, desbloqueia Onda 4–6**

| # | Issue | Esforço | Tipo | Status | Motivo |
|---|---|---|---|---|---|
| **1495** | `ktlint`/`detekt` em core/feature | S–M | tech-debt | ✓ **P1** | Rede de proteção |
| **1581** | Testes instrumentados (local + CI) | XS + M | infra | ⚠️ Bloqueada por ambiente | Exige device/emulador |
| **1585** | [SECURITY] Endpoints `/ingest/*` sem auth | **XS/S** | **security** | 🔴 **P0** | Destrava multi-tenant seguro |
| **1587** | Consolidar auth PBKDF2 duplicada | M | tech-debt | ✓ Fila | Reduz manutenção |

**Ordem interna da onda:** #1485 e #1261 (Onda 1) **antes** de #1495 — ligar `ktlint`/`detekt` sobre
código que já está condenado gera correção descartada na sequência.

**Nota sobre #1581:** o `.buildea/ambiente-local.json` de 2026-08-04 reporta `ANDROID_HOME` e
`JAVA_HOME` nulos; sem device ou emulador detectado por `adb devices` a issue não avança.

**Saída:** proteção de estilo + testes + segurança de base para multi-tenant

---

## ONDA 3: Features P1 com caminho livre (M–L)

**Paralelo, sem bloqueio**

| # | Issue | Esforço | Bloqueio | Status |
|---|---|---|---|---|
| **1481** | CI gate de Feature Flags | M | Nenhum | ✓ Fila |
| **1472** | Receber push + comparar versão | M–L | Nenhum | ✓ Desbloqueado |
| **1473** | Exibir versão em Ajustes | M | Depende #1472 | ✓ Fila |
| **1312** | Feat #1312 (guarda-chuva) | — | Fecha com 1472+1473 | — |

**Saída:** sistema de atualização do app completo

---

## ONDA 4: Diagnóstico — testes + feedback (S–M)

**Parcialmente paralelo com Fase 0**

| # | Issue | Esforço | Bloqueio | Status |
|---|---|---|---|---|
| 1460 | Testes de diagnóstico (schemaVersion, timeout) | S | Nenhum | ✓ Fila |
| **1582** | [BUG] Divergência 6 GHz vs 5 GHz | S investigar | Nenhum | ✓ Criada |

**Saída:** cobertura de testes + issue concreta de multi-platform divergência

---

## ONDA 5: Alinhamento de métricas (pós-Fase 0)

**BLOQUEADA por #1583 — pesquisa pronta**

| # | Issue | Esforço | Pré-requisito | Status |
|---|---|---|---|---|
| **1466** | Alinhar latência/perda/upload | M | #1583 ✓ | 🔴 Bloqueada |
| **1586** | MetricClassifier em SinalScreen | M | #1583 ✓ | 🔴 Bloqueada |
| **1520** | Uptime: restaurar grid simples + apagar código morto | S | Nenhum | ✓ Fila (decisão fechada) |

**Saída:** régua única de qualidade + Histórico com grid

---

## ONDA 6: Motores grandes (L–XL)

**BLOQUEADA por #1583 + #1495 (pesquisa + infra)**

| # | Issue | Esforço | Pré-requisitos | Nota |
|---|---|---|---|---|
| **975** | Motor canônico de topologia Wi-Fi | L | #1495 ✓, #1583 ✓ | 5 motores → 1 |
| **1228** | Centralizar diagnóstico e recomendações | **XL** | #1495 ✓, #1583 ✓, #975 ✓ | Épico guarda-chuva |
| 1169 | Design System integral | L | #1495 ✓ | Absorve #1499 |

**Saída:** arquitetura de diagnóstico unificada

---

## ONDA 7: Superfícies + Multi-tenant Implementation (M–L)

**Paralelo ou pós-Onda 6**

| # | Issue | Esforço | Bloqueio | Status |
|---|---|---|---|---|
| 1330 | AdMob nativo | M–L | ✓ Desbloqueado (teste) | in_progress |
| 1361 | MediaView em NativeAdRow/ListRow | M | Depende 1330 | Fila |
| 1252 | Consolidar Ajustes (parcial) | L | Nenhum | Reescopar |
| 1376 | Branding "by 7A" (reescopado) | M | Nenhum | Reescopar |
| 1463 | Investigar ASN | S investigar | Nenhum | ✓ Fila |
| 1015 | Subsetar fonte Material Symbols | M | #1014 (ícones) | Bloqueado |
| **1584-F2** | [Épico] Multi-tenant — Android implementation | M–L | #1584-F1 ✓ | Fase 2 |
| **1584-F3** | [Épico] Multi-tenant — Web implementation | M–L | #1584-F1 ✓ | Fase 3 |

**Saída:** superfícies grandes + multi-tenant funcionando em App/Web

---

## Resumo Crítico

| Onda | Tipo | Semanas | Destravasor |
|---|---|---|---|
| **Fase 0** | Pesquisa + Design | **1–2** | 📋 Métricas + Multi-tenant contract |
| **1** | Higiene | **1** | Paralelo, sem bloqueio |
| **2** | Infra + Security | **1–2** | Protege refactors + multi-tenant |
| **3** | Features | **2–3** | Independente (update do app) |
| **4** | Testes + Feedback | **1** | Paralelo com Fase 0 |
| **5** | Alinhamento | **1–2** | Pós-Fase 0 |
| **6** | Motores | **4–6** | Pós-Onda 5 + Infra |
| **7** | Superfícies + Multi-tenant | **3–4** | Paralelo ou final |

---

## Caminho Crítico (sem paralelismo)

```
Fase 0 (pesquisa + design multi-tenant)
    ↓
Onda 2 (infra + segurança)
    ↓
Onda 6 (motores grandes)
    ↓
Onda 7 (superfícies + implementação multi-tenant)
```

**Duração estimada:** 12–14 semanas se tudo linear

**Paralelismo possível:** Ondas 1, 3, 4 rodam junto com Fase 0 e Onda 2

---

## Decisões Incorporadas (11 + 6 novas)

| Decisão | Issue | Efeito |
|---|---|---|
| 1 | #1502 | ✓ Fechada (validação dispensada) |
| 2 | #1330 | ✓ In_progress (teste) |
| 3 | #1581 | ✓ Fila (a+b) |
| 4 | #1520 | ✓ Fila — decisão de Luiz em 2026-08-05: restaurar grid simples de 7 dias no `HistoricoScreen`, apagar `UptimeChartUseCase` e `UptimeNarrativaEngine` (mantendo `UptimeGridChart`) |
| 5 | #1466 | ✓ Re-bloqueada por #1583 |
| 6–10 | #1124, #885, #1172, #1255, #1495 | ✓ Canceladas/ajustadas |
| 11 | #1463 | ✓ Fila (investigação) — mas rotulada `fase-1`, fora do escopo congelado em 2026-07-21 |
| Novo | #1583 | ✓ Épica de pesquisa de métricas |
| Novo | #1582 | ✓ Feedback de tester (6 GHz vs 5 GHz) |
| Novo | #1584 | ✓ [Épico] Contrato multi-plataforma unificado |
| Novo | #1585 | ✓ [P0 SECURITY] endpoints `/ingest/*` sem auth |
| Novo | #1586 | ✓ [P2 BUG] MetricClassifier não usado em `SinalScreen.kt` |
| Novo | #1587 | ✓ [P2 TECH-DEBT] auth PBKDF2 duplicada |
| Novo | #1588 | ✓ [P2 DOC] OpenAPI: admin-api incompleto + remover contratos fictícios |
| Novo | #1589 | ✓ [P3 DOC] limpar `_archive/` |

---

## Fila: 41 issues abertas no repositório (verificado 2026-08-06)

| Status | Próximos passos |
|---|---|
| 🟢 In progress | #1330, #1520, #1463, #1581 |
| 🟡 Fila (sem bloqueio) | Ondas 1, 2, 3, 4 — pode começar |
| 🔴 Bloqueada por #1583 | #1466, #1586, #975, #1228, #1169 |
| 🔴 Bloqueada por ambiente | #1581 (device Android), #1015 (depende de #1014) |
| 📋 Épico/Research | #1583, #1584, #1228 |
| ⏸️ Fora do escopo congelado (`fase-1`/`fase-2`) | #547, #907, #951, #1247, #1248, #1319, #1320, #1321, #1463 |
| ↗️ Fora do Consumer | #1347, #1587, #1588, #1589, #1007, #1255, #1376 |

---

## Início recomendado (próxima semana)

**Paralelo:**
- agent:general-purpose começa #1583 (pesquisa métricas)
- Camilo + squad começa #1584 Fase 1 (design do contrato multi-plataforma)
- Squad executa Onda 1 (higiene mecânica: #1485, #1261, #1499, #1172)
- Camilo executa Onda 3 (features P1 prontas: #1472 → #1473 → fecha #1312; #1481)
- Camilo executa **#1585** (segurança P0) — revisão obrigatória de Caio

**Resultado semana 2:**
- Pesquisa de métricas pronta
- Multi-tenant contract design pronto
- Segurança de base implementada
- Features de update do app avançadas
- Código limpo de tech-debt óbvia

