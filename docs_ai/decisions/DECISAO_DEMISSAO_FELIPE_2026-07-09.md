---
title: Decisão — Demissão do Felipe (2026-07-09)
status: registrado (histórico)
última_validação: 2026-07-21
escopo: squad SignallQ, estrutura corporativa
responsável: Luiz (CEO)
documento_anterior: none
---

## Contexto

Felipe ocupava o papel de dev de Admin/Cloudflare (React/TypeScript, Workers, D1). Na PR #781, reportou "paridade total com o mockup" antes de validar contra a URL de produção com dado real — trabalhou só contra mock local. Resultado: Topbar com badge inventado, copy em inglês não reconferido, labels de KPI do Worker nunca auditados contra o mockup, bloco de alertas sumindo em produção. Pattern recorrente não isolado.

## Decisão

Demitido em 2026-07-09, sem reposição de vaga.

## Consequências estruturais

- **Implementação do Admin Panel (React/TS) + Workers Cloudflare** → absorvido pelo **Camilo** (que passa de "Dev Android" para dev único do squad: Android + Admin + Cloudflare)
- **Análise/leitura executiva de dados** (Play Console, Firebase Analytics, custo IA, métricas de diagnóstico) → absorvido pela **Claudete** (natural do papel de PM/Tech Lead)
- Persona arquivada em `.claude/agents/_archive/felipe_2026-07-09_demitido.md`

## Regra operacional criada

**Camilo (novo owner de Admin/Cloudflare):** validação obrigatória contra a URL de produção com dado real antes de reportar qualquer entrega do Admin como concluída — nunca só contra mock local. Ver `camilo.md`, seção "Admin & Cloudflare (herdado do Felipe, 2026-07-09)" e `.claude/CLAUDE.md`, seção "Agentes".
