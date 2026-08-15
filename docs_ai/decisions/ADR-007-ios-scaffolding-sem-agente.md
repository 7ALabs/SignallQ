# ADR-007 — iOS: scaffolding criado, agente adiado

> **⚠️ SUPERSEDED por [ADR-014](ADR-014-squad-canonico-ai-governance.md) em 2026-08-15.**
> A decisão de não criar agente iOS continua válida (app descontinuado desde 2026-07-04),
> mas a regra de "quando iOS voltar, cria camilo-ios" foi absorvida pelo ADR-014: iOS,
> se retomado, é responsabilidade natural de Camilo; nova persona só surge por decisão
> explícita do Luiz.
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
