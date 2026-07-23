---
title: Decisão — Cronograma de Lançamento (revisão 2026-07-20)
status: vigente
última_validação: 2026-07-21
escopo: roadmap, milestones SignallQ
responsável: Luiz (CEO)
referência: GitHub issue #1222
documento_anterior: MILESTONE.md (snapshot 2026-07-17)
---

## Histórico de revisões

| Data | Estado anterior | Nova data | Motivação |
|---|---|---|---|
| **2026-07-17** | Release público em 07/08 | RC em 07/08, público em 14/08 | Teste fechado mais longo (M2 para 21/07) |
| **2026-07-20** | Público em 14/08 | **Público em 21/08** | Espaço pra não cortar escopo/dívida técnica sob pressão (reverteu corte de #1207/#1209 de bugs P0) |

## Decisão vigente

**Lançamento público em 21/08/2026** (trilha `production`, staged rollout). Fonte de verdade: GitHub issue #1222 (não este arquivo).

## Razão

Após revisão da auditoria de 2026-07-17, 6 issues de QA de M2 não estavam cobertas pela varredura de regressão visual do redesign MD3 — cenários funcionais profundos (#618, #620, #616, #614, #615) exigem rodada dedicada em device real (SIM físico, controle de conectividade, app de versão anterior). Sem isso, abertura do Open Beta (M4 em 04/08) e lançamento público cascateariam. O slack extra de uma semana (21 vs 14 de agosto) elimina a pressão de corte apressado.

## Guardrails técnicos

- `promote-release.yml` hoje só aceita `internal`/`alpha` como destino — beta e produção exigem decisão explícita do Luiz (não são automáticas)
- Requisitos de elegibilidade do Google (duração mínima de teste fechado, contas criadas antes de nov/2023) não foram confirmados com precisão — validar direto no Play Console antes de prometer a data

## Próximo passo

Executar M2 (Beta Fechado em 21/07) com a rodada dedicada de QA em device real. M4 (Open Beta) depende de M2 fechado.
