---
title: "ADR-007 — iOS: scaffolding criado, agente adiado"
description: "Superseded por ADR-014 e reforçado por ADR-015 em 2026-08-15. Registro histórico da decisão de 2026-06-24 de não criar agente iOS enquanto o app estava adiado; hoje iOS está permanentemente fora do escopo da marca."
type: "adr"
status: "deprecated"
owner: "Luiz (CEO)"
last_updated: "2026-08-15"
version: "2.0.0"
---

# ADR-007 — iOS: scaffolding criado, agente adiado

> **⚠️ SUPERSEDED por [ADR-014](ADR-014-squad-canonico-ai-governance.md) em 2026-08-15
> e reforçado por [ADR-015](ADR-015-plataformas-android-webapp.md) na mesma data.**
> A decisão de "iOS adiado" evoluiu para **iOS fora do escopo permanente da marca
> SignallQ** — ADR-015 declara Android + Webapp como as únicas plataformas do
> portfólio. A porta que este ADR deixava aberta ("Quando criar o agente iOS")
> está fechada; só é reaberta por novo ADR do Luiz que superseda ADR-015.
>
> **Nota (2026-07-04):** o app iOS foi descontinuado; este ADR é mantido como
> registro histórico da decisão.
>
> **Nota (2026-07-16):** renumerado de ADR-005 para ADR-007 -- havia colisão de numeração com
> ADR-005-custo-ia-free-tier-fallback.md. Conteúdo inalterado.

**Data:** 2026-06-24
**Status:** Superseded (2026-08-15) — originalmente Aceito em 2026-06-24

## Contexto

O repositório foi reorganizado como monorepo SignallQ com três plataformas: Android (existente), PWA (em desenvolvimento) e iOS (futuro). A pasta `ios/` foi criada com `README.md` e `CLAUDE.md` como scaffolding.

## Decisão

Não criar agente iOS nem skills iOS neste momento. O desenvolvimento iOS ainda não tem início previsto e criar agente vazio geraria ruído sem valor.

## Quando criar o agente iOS

Antes de iniciar qualquer desenvolvimento Swift/SwiftUI, criar:
1. Agente especializado (sugestão: `camilo-ios`) com contexto de Swift + SwiftUI + Xcode + Firebase iOS
2. Skill `ios-platform-rules` cobrindo: permissões iOS, App Store guidelines, sandbox, APIs nativas
3. Issue no Linear para a criação do agente e setup inicial do projeto Xcode

## Consequências

- `ios/CLAUDE.md` documenta que o agente está pendente de criação
- Qualquer tarefa iOS até lá fica bloqueada — sem agente responsável
- Bundle ID iOS será definido quando o projeto Xcode for inicializado (não reutilizar `io.veloo.app`)
