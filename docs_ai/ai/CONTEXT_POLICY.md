# Context Policy

> Referência principal: `AGENTS.md` na raiz deste projeto.
> Comandos de agentes: `.claude/commands/`

## Fontes de contexto (ordem de prioridade)

1. `docs_ai/technical/` — arquitetura, módulos, telas
2. `docs_ai/ai/` — regras de workflow, handoffs
3. `docs_ai/design-system/` — consistência visual
4. Codebase da raiz — módulos `app/`, `core*` e `feature*`
5. `.claude/commands/` — comandos auxiliares de agentes

## Estratégia de carregamento

- Leia `docs_ai/README.md` como ponto de entrada.
- Carregue apenas os docs relevantes à task (ex: `ARCHITECTURE.md` para perguntas de estrutura).
- Use `Grep` para localizar arquivos e símbolos específicos antes de ler módulos inteiros.
- Evite carregar docs completos quando a busca por símbolo resolve mais rápido.
- Delegue buscas de código ao Marcelo (Haiku) para preservar contexto Sonnet.
- Delegue buscas de documentação à Nina (Haiku) para triagem inicial de docs.

## O que NÃO fazer

- Não assuma que módulos de feature existem separados do `app/` sem verificar no código.
- Não infira paths não confirmados explicitamente.
- Não referencie `.orbit/` — esse sistema foi descontinuado e substituído por `.claude/`.
- Não invente comportamento de feature que não está confirmado no código ou nos docs.

## Docs de referência rápida

- `technical/ARCHITECTURE.md` — design do sistema
- `technical/MODULES.md` — breakdown de módulos
- `technical/SCREEN_MAP.md` — localização de telas
- `ai/HANDOFF_RULES.md` — protocolo de handoff entre agentes
- `ai/AGENT_WORKFLOW.md` — fluxo completo do sistema multiagente
