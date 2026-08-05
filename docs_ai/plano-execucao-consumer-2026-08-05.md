# Plano de Execução Reordenado — SignallQ Consumer

**Data:** 2026-08-05 · **Fila:** 34 issues abertas · **Blocos de trabalho:** 7 ondas

---

## FASE 0: Pesquisa Fundacional (P0 crítica — semana 1)

**Bloqueia:** #1466, #1228, #975, #1330 (AdMob), qualidade geral

| # | Issue | Esforço | Status | Decisão |
|---|---|---|---|---|
| **1583** | [Épico] Pesquisa de métricas e limiares — padrão nacional | **L** | 🔴 Backlog | ✓ Aprovada (sem entrevistas) |

**Saída esperada:** `docs_ai/metricas-qualidade-rede-v1.md` — documento único de verdade

---

## ONDA 1: Higiene Mecânica (XS–S, zero dependência)

**Paralelo com Fase 0, não bloqueado por ela**

| # | Issue | Esforço | Tipo | Status |
|---|---|---|---|---|
| 1172 | Atualizar hex `#6C2BFF` em docs | XS | docs | ✓ Legítima (DS é fonte) |
| 1007 | Limpar `docs_ai/` (squad obsoleta) | XS | docs | ✓ Em backlog |
| 1485 | Remover `AnaliseDetalhadaBottomSheet` | S | tech-debt | ✓ Fluxo legado (#1475 fechada) |
| 1261 | Remover composables mortas (3 funções) | S | tech-debt | ✓ Cluster fechado |
| 1499 | `Color.White` hardcoded → token | S | bug | ✓ 7 ocorrências |

**Saída:** código limpo, zero tech-debt óbvia

---

## ONDA 2: Infra de Qualidade (S–M, alavanca para refactors)

**Não bloqueado, mas desbloqueia Onda 4–5**

| # | Issue | Esforço | Tipo | Status | Motivo |
|---|---|---|---|---|---|
| **1495** | `ktlint`/`detekt` em core/feature | S–M | tech-debt | ✓ **P1 elevada** | Rede de proteção para #1228/#975 |
| 1581 | Testes instrumentados (local + CI) | XS + M | infra | ✓ Fila | Execução local agora, CI depois |

**Saída:** proteção de estilo + testes automatizados

---

## ONDA 3: Features P1 com caminho livre (M–L)

**Bloqueado por Fase 0 de pesquisa? NÃO (paralelo)**

| # | Issue | Esforço | Bloqueio | Status |
|---|---|---|---|---|
| **1481** | CI gate de Feature Flags | M | Nenhum (#1477–#1480 fechadas) | ✓ Fila |
| **1472** | Receber push + comparar versão | M–L | Nenhum (#1471/#1440 prontos) | ✓ Desbloqueado |
| **1473** | Exibir versão em Ajustes | M | Depende #1472 | ✓ Fila |
| **1312** | Feat #1312 (guarda-chuva) | — | Fecha quando 1472+1473 fecharem | — |

**Saída:** sistema de atualização do app completo

---

## ONDA 4: Diagnóstico — testes + decisão (S–M)

**Parcialmente paralelo com Fase 0**

| # | Issue | Esforço | Bloqueio | Status |
|---|---|---|---|---|
| 1460 | Testes de diagnóstico (schemaVersion, timeout) | S | Nenhum | ✓ Fila |
| **1582** (novo) | [BUG] Divergência 6 GHz vs 5 GHz | S investigar | Nenhum | ✓ Criada (feedback real) |

**Saída:** cobertura de testes + issue concreta do problema de métricas

---

## ONDA 5: Alinhamento de métricas (pós-pesquisa)

**BLOQUEADA por #1583 (Fase 0)**

| # | Issue | Esforço | Pré-requisito | Status |
|---|---|---|---|---|
| **1466** | Alinhar latência/perda/upload | M | #1583 pronta | 🔴 Bloqueada |
| **1520** | Restaurar Uptime grid | S | Nenhum | ✓ Fila (pode ir agora) |

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

## ONDA 7: Superfícies grandes + futuro

**Paralelo ou pós-Onda 6**

| # | Issue | Esforço | Bloqueio | Status |
|---|---|---|---|---|
| 1330 | AdMob nativo | M–L | ✓ Desbloqueado (teste) | Em in_progress |
| 1361 | MediaView em NativeAdRow/ListRow | M | Depende 1330 | Fila |
| 1252 | Consolidar Ajustes (parcial) | L | Nenhum | Reescopar |
| 1376 | Branding "by 7A" (reescopado) | M | Nenhum | Reescopar |
| 1463 | Investigar ASN | S investigar | Nenhum | ✓ Fila |
| 1015 | Subsetar fonte Material Symbols | M | #1014 (migração ícones) | Bloqueado |

---

## Resumo Crítico

| Onda | Tipo | Semanas | Destravasor |
|---|---|---|---|
| **Fase 0** | Pesquisa | **1–2** | 📋 Pesquisa de métricas nacionais |
| **1** | Higiene | **1** | Paralelo, sem bloqueio |
| **2** | Infra | **1–2** | Protege refactors |
| **3** | Features | **2–3** | Independente (update do app) |
| **4** | Testes + feedback | **1** | Paralelo com Fase 0 |
| **5** | Alinhamento | **1–2** | Pós-Fase 0 |
| **6** | Motores | **4–6** | Pós-Onda 5 + Infra |
| **7** | Superfícies | **3–4** | Paralelo ou final |

---

## Caminho Crítico (sem paralelo)

```
Fase 0 (pesquisa)
    ↓
Onda 2 (infra)
    ↓
Onda 6 (motores grandes)
    ↓
Onda 7 (superfícies)
```

**Duração estimada:** 10–12 semanas se tudo linear

**Paralelo possível:** Ondas 1, 3, 4 rodam junto com Fase 0 e Onda 2

---

## Decisões Incorporadas

| # Decisão | Registrada em | Efeito |
|---|---|---|
| 1 | #1502 | ✓ Fechada |
| 2 | #1330 | ✓ In_progress (teste) |
| 3 | #1581 | ✓ Fila (a+b) |
| 4 | #1520 | ✓ Fila (restaurar + apagar) |
| 5 | #1466 | ✓ Re-bloqueada por #1583 |
| 6–10 | #1124, #885, #1172, #1255, #1495 | ✓ Canceladas/ajustadas |
| 11 | #1463 | ✓ Fila (investigação) |
| Novo | #1583 | ✓ Épica de pesquisa |
| Novo | #1582 | ✓ Feedback de tester |

