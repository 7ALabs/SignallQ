---
title: "Documentação — SignallQ"
description: "Ponto de entrada da documentação do app consumer Android e do backend Cloudflare"
type: "índice"
status: "ativo"
owner: "Claudete (processo) · Camilo (técnico)"
last_updated: "2026-08-15"
---

# Documentação — SignallQ

- **Fonte de verdade:** este arquivo é só um índice — não repete conteúdo de nenhum documento
  listado. Para os fatos, abra o documento apontado.
- **Escopo:** **app consumer Android** (`io.signallq.app`) e **backend Cloudflare** (5 Workers, 2
  bancos D1). Nada além disso.

> ⚠️ **Reconstrução em andamento (2026-08-06).** Este repositório passou por mudança de perímetro
> — Admin e web saíram para repositórios próprios, o Pro entrou em hold — e a documentação está
> sendo refeita a partir do código. Os documentos canônicos (`FUNCIONAL.md`, `TECNICO.md`,
> `ARQUITETURA/`, `DESIGN_SYSTEM.md`) **ainda são os da árvore antiga e contêm dados defasados** —
> por exemplo, `TECNICO.md` declara `0.30.1`/`67` enquanto o código está em `0.31.0`/`72`.
> Até a reescrita, **confirme todo dado numérico direto no código.** Ordem de precedência em
> `.claude/rules/higiene-e-padronizacao-repositorio.md §3`.

---

## Perímetro: o que é deste repositório

| Produto | Onde vive | Documentação |
|---|---|---|
| **SignallQ consumer (Android)** | `android/` neste repo | aqui |
| **Backend Cloudflare** (5 Workers, D1) | `integrations/cloudflare/` neste repo | aqui |
| SignallQ Admin | repo **`buildea-admin`** | no repo dele |
| Site / PWA | repo **`signallq-web`** (Next 16 + PWA) | no repo dele |
| SignallQ Pro | **descontinuado permanentemente** (ver [ADR-016](decisions/ADR-016-portfolio-buildea.md)) | removida — módulos, docs e skill saíram do repo nas Fases 4a-b do épico #1623 |
| SignallQ Nethal | repo separado | fora deste repositório |

O `signallq-admin-worker` é **deste** repositório, embora o painel Admin que o consome não seja.

---

## Documentos centrais

| Documento | Conteúdo |
|---|---|
| [`HISTORIA.md`](./HISTORIA.md) | Origem, propósito e princípios que explicam por que o SignallQ existe |
| [`FUNCIONAL.md`](./FUNCIONAL.md) | O que o app faz — navegação, telas, funcionalidades, permissões |
| [`TECNICO.md`](./TECNICO.md) | Como é construído — stack, build, Workers, persistência, analytics, segurança |
| [`DESIGN_SYSTEM.md`](./DESIGN_SYSTEM.md) | Cores, tipografia, espaçamento, componentes, tokens |
| [`ARQUITETURA/README.md`](./ARQUITETURA/README.md) | Visão de sistema e dependências entre módulos |
| [`ARQUITETURA/MODULOS/`](./ARQUITETURA/MODULOS/) | Um documento por módulo Gradle |
| [`CONTRATOS/openapi/`](./CONTRATOS/openapi/) | 7 contratos OpenAPI 3.0 · 122 endpoints |
| [`CONTRATOS/schemas/`](./CONTRATOS/schemas/) | Índice dos schemas reais (Room, D1) |
| [`RELEASES.md`](./RELEASES.md) | Histórico de releases |
| [`INDICE.md`](./INDICE.md) | Mapa completo com status e responsável |

## Pastas

| Pasta | Conteúdo |
|---|---|
| `decisions/` | ADRs (`ADR-001`…`ADR-013`) e decisões de negócio — **preservados, não regeneráveis** |
| `foundation-linka/` | Material provisório do produto Linka (ADR-016) — migra para o repo `buildea-labs/linka` quando ele nascer |
| `operations/` | Runbooks: release, deploy, hotfix, rollback, assinatura, custos |
| `technical/` | Referências técnicas pontuais: schema da Admin API, mapas de campo de equipamento (Intelbras, Nokia, TP-Link), fluxo de IA, ping executor |
| `functional/` | Specs funcionais que não migraram para `FUNCIONAL.md` |
| `design-system/` | Decisões de design (tokens, paleta, topbar) — conteúdo vigente em `DESIGN_SYSTEM.md` |
| `legal/` | Política de privacidade e termos de uso — **instrumentos jurídicos, não editar sem revisão** |
| `templates/` | Modelos para documento novo (técnico, funcional, ADR, runbook) |
| `_archive/` | Vazia por decisão — ver [`_archive/README.md`](./_archive/README.md) |

Assets de marca vivem em `brand/` na raiz do repositório, não aqui.

---

## O que saiu em 2026-08-06

Removidos da árvore; recuperáveis via git a partir do commit `10b2f05d` (instruções em
[`_archive/README.md`](./_archive/README.md)).

| Removido | Qtd | Motivo |
|---|---:|---|
| `_archive/` | 100 | Git já é o arquivo; a pasta poluía toda busca com versões substituídas |
| `plataforma/` (pacote v5, docs 00–07) | 10 | Descrevia monorepo `signallq-platform` que não existe, Admin que saiu do repo e inventário defasado. A paleta que ele prescrevia está implementada e conferida no código |
| `ai/` | 8 | Fluxo de trabalho de agente, não documentação de produto → movido para `.claude/fluxos/` |
| `plano-execucao-consumer-2026-08-05.md` | 1 | Superado no mesmo dia pela versão consolidada v2, que permanece |
| `AUDIT_REPORT_2026-08-05.md` | 1 | Continha afirmações derrubadas pela validação contra código de 2026-08-06 |
| `design-system/_archive/` | 1 | Mesma regra do `_archive/` |

**235 → 116 documentos.**

## O que saiu em 2026-08-15

Removidos da árvore; recuperáveis via git a partir do commit `0daa424a`.

| Removido | Qtd | Motivo |
|---|---:|---|
| `pro-onhold/` | 7 | SignallQ Pro descontinuado permanentemente (ADR-016); Fase 4b do épico #1623 |

**116 → 111 documentos.**

## Regra a partir de agora

Documento substituído é **removido**, não arquivado. A substituição fica registrada no documento
que substituiu e no histórico do git.
