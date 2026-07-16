# Context Policy

> **Fonte da verdade:** `.claude/CLAUDE.md` + `.claude/agents/*.md`. Este arquivo é um resumo apontador.
> Decisão de fluxo: `docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`.
> Versão: v0.23.0 · 2026-07-05.

## Fontes de contexto (ordem de prioridade)

1. `.claude/CLAUDE.md` + `.claude/agents/*` — squad, fluxo e regras operacionais
2. `docs_ai/technical/` — arquitetura, módulos, telas
3. `docs_ai/ai/` — resumos apontadores de workflow e handoff
4. `docs_ai/design-system/` — consistência visual
5. Codebase — módulos `android/` e `SignallQ Admin/`

## Estratégia de carregamento

- Leia `docs_ai/README.md` como ponto de entrada.
- Carregue apenas os docs relevantes à task.
- Use Grep/Glob para localizar arquivos e símbolos antes de ler módulos inteiros. Busca de código é ferramenta nativa — não há agente de busca.
- Evite carregar docs completos quando a busca por símbolo resolve mais rápido.

## O que NÃO fazer

- Não referencie `.signallq/` — descontinuado, substituído por `.claude/`.
- Não trate `docs_ai/ai/*` como verdade paralela — são resumos; o canônico é `.claude/CLAUDE.md` + `.claude/agents/*`.
- Não infira paths não confirmados no código.
- Não invente comportamento de feature não confirmado.

## Docs de referência rápida

- `ARQUITETURA/README.md` — design do sistema
- `TECNICO.md / ARQUITETURA/MODULOS/` — breakdown de módulos
- `technical/SCREEN_MAP.md` — localização de telas
- `ai/HANDOFF_RULES.md` — protocolo de handoff
- `ai/AGENT_WORKFLOW.md` — fluxo completo
