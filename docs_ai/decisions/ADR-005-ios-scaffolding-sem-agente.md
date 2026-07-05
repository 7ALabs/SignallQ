# ADR-005 — iOS: scaffolding criado, agente adiado

> **Nota (2026-07-04):** o app iOS foi descontinuado; este ADR é mantido como
> registro histórico da decisão.

**Data:** 2026-06-24
**Status:** Aceito

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
