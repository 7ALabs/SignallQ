# Context Policy

> **Fonte da verdade:** [`AGENTS.md`](../../AGENTS.md) (repo) + [`ai-governance/policies/`](../../../ai-governance/policies/) (org).
> **Decisão canônica:** [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).
> **Última atualização:** 2026-08-15.

## Precedência de instruções

Alinhada com [contrato op §2](../../../ai-governance/policies/agent-operating-contract.md):

1. Instrução explícita e atual do Luiz.
2. Políticas em [`ai-governance/policies/`](../../../ai-governance/policies/).
3. [`AGENTS.md`](../../AGENTS.md) do repo e regras em [`.claude/rules/`](../rules/).
4. Personas em [`.claude/agents/`](../agents/) (Claudete, Camilo, Caio).
5. Documentação técnica ativa em [`docs_ai/`](../../docs_ai/).
6. Resumos apontadores em `.claude/fluxos/`.
7. Skills em [`.claude/skills/`](../skills/).
8. Codebase — `android/`, `integrations/cloudflare/`.
9. Documentação histórica (ADRs superseded).

## Estratégia de carregamento

- Leia [`docs_ai/README.md`](../../docs_ai/README.md) como ponto de entrada.
- Carregue apenas os docs relevantes à task.
- Use Grep/Glob para localizar antes de ler módulo inteiro. Busca é ferramenta nativa — não há agente de busca.

## O que NÃO fazer

- **Não referencie `ai-governance/agents/*.md`** como fonte da verdade do squad SignallQ — [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md) migrou pra `.claude/agents/` deste repo.
- **Não trate `.claude/fluxos/*` como verdade paralela** — são resumos apontadores.
- **Não referencie personas legadas** como responsáveis ativos:
  - Do squad antigo (superseded por ADR-016): Juliana, Marcos, Gustavo — agora são skills invocáveis.
  - Fantasmas históricos: Felipe, Lia, Gema, Rhodolfo, Juninho, Marina, Claudio, Nina, Taisa, Marcelo, Otávio.
- Não infira paths não confirmados no código.
- Não invente comportamento de feature não confirmado.

## Docs de referência rápida

- [`docs_ai/ARQUITETURA/README.md`](../../docs_ai/ARQUITETURA/README.md), [`docs_ai/TECNICO.md`](../../docs_ai/TECNICO.md)
- [`docs_ai/ARQUITETURA/MODULOS/`](../../docs_ai/ARQUITETURA/MODULOS/)
- [`HANDOFF_RULES.md`](HANDOFF_RULES.md), [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md)
