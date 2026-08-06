---
title: "Plano de execução consolidado — SignallQ Consumer"
description: "Fila de 42 issues do consumer organizada em ondas"
type: "plano"
status: "ativo"
owner: "Claudete"
last_updated: "2026-08-06"
---

# Plano de Execução Consolidado — SignallQ Consumer v2

**Data:** 2026-08-05 · **Fila Consumer:** 42 issues (36 anteriores + 6 novas) · **Blocos:** 7 ondas + Fase 0

---

## FASE 0: Pesquisa Fundacional + Auditoria (P0 crítica — semana 1–2)

**Bloqueia:** #1466, #1228, #975, #1585, #1589 Fase 1

| # | Issue | Esforço | Responsável | Status |
|---|---|---|---|---|
| **1583** | [Épico] Pesquisa de métricas e limiares — padrão nacional | **L** | agent:general-purpose | 🔴 Backlog |
| **1589** | [Épico] Contrato multi-plataforma unificado — diagnostic-worker | **M (design)** | Camilo + squad | 🔴 Backlog |

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
| **1581** | Testes instrumentados (local + CI) | XS + M | infra | ✓ Fila | Execução local agora |
| **1584** | [SECURITY] Endpoints sem auth | **XS/S** | **security** | 🔴 **P0** | Destrava multi-tenant seguro |
| **1586** | Consolidar auth PBKDF2 duplicada | M | tech-debt | ✓ Fila | Reduz manutenção |

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
| **1585** | MetricClassifier em SinalScreen | M | #1583 ✓ | 🔴 Bloqueada |
| **1520** | Restaurar Uptime grid | S | Nenhum | ✓ Fila (pode ir) |

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
| **1589-F2** | [Épico] Multi-tenant — Android implementation | M–L | #1589-F1 ✓ | Fase 2 |
| **1589-F3** | [Épico] Multi-tenant — Web implementation | M–L | #1589-F1 ✓ | Fase 3 |

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
| 4 | #1520 | ✓ Fila (restaurar + apagar) |
| 5 | #1466 | ✓ Re-bloqueada por #1583 |
| 6–10 | #1124, #885, #1172, #1255, #1495 | ✓ Canceladas/ajustadas |
| 11 | #1463 | ✓ Fila (investigação) |
| Novo 1583 | #1583 | ✓ Épica de pesquisa |
| Novo 1582 | #1582 | ✓ Feedback de tester |
| Novo 1584 | #1584 | ✓ [P0 SECURITY] endpoints sem auth |
| Novo 1585 | #1585 | ✓ [P2 BUG] MetricClassifier não usado |
| Novo 1586 | #1586 | ✓ [P2 Tech-debt] auth duplicada |
| Novo 1587 | #1587 | ✓ [P2 Doc] OpenAPI admin-worker |
| Novo 1588 | #1588 | ✓ [P3 Doc] remover OpenAPI fictícios |
| Novo 1589 | #1589 | ✓ [Épico] Multi-tenant unificado |

---

## Fila do Consumer: 42 issues abertas

| Status | Qtd | Próximos passos |
|---|---|---|
| 🟢 Fechadas | 1 | #1502 |
| 🟢 In progress | 2 | #1330 |
| 🟡 Fila (sem bloqueio) | 18 | Ondas 1, 3, 4, 5 (pode começar) |
| 🔴 Bloqueada | 8 | Pós-#1583 (Fase 0) |
| 📋 Épico/Research | 3 | #1583, #1228, #1589 |
| 🔴 Canceladas | — | Site, Admin, PRO |

---

## Início recomendado (próxima semana)

**Paralelo:**
- agent:general-purpose começa #1583 (pesquisa métricas)
- Camilo + squad começa #1589 Fase 1 (design multi-tenant)
- Squad executa Onda 1 (higiene mecânica)
- Camilo executa Onda 3 (features P1 prontas)
- Camilo executa #1584 (segurança P0)

**Resultado semana 2:**
- Pesquisa de métricas pronta
- Multi-tenant contract design pronto
- Segurança de base implementada
- Features de update do app avançadas
- Código limpo de tech-debt óbvia

