# Context Policy

> **Fonte da verdade:** [`AGENTS.md`](../../AGENTS.md) (contexto do repo) + [`ai-governance/`](../../../ai-governance/) (governança organizacional). Este arquivo é um resumo apontador.
> **Decisão canônica:** [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md).
> **Última atualização:** 2026-08-15.

## Fontes de contexto (ordem de prioridade)

Alinhada com [`ai-governance/policies/agent-operating-contract.md` §2](../../../ai-governance/policies/agent-operating-contract.md) (hierarquia de instruções):

1. Instrução explícita e atual do Luiz.
2. Políticas organizacionais em [`ai-governance/policies/`](../../../ai-governance/policies/).
3. [`AGENTS.md`](../../AGENTS.md) do repositório e regras em [`.claude/rules/`](../rules/).
4. Definições canônicas de agentes em [`ai-governance/agents/`](../../../ai-governance/agents/).
5. Documentação técnica ativa em [`docs_ai/`](../../docs_ai/) — arquitetura, módulos, contratos, telas.
6. Resumos apontadores em `.claude/fluxos/`.
7. Skills em [`.claude/skills/`](../skills/).
8. Codebase — `android/`, `integrations/cloudflare/`.
9. Documentação histórica (ADRs superseded, decisões arquivadas).

## Estratégia de carregamento

- Leia [`docs_ai/README.md`](../../docs_ai/README.md) como ponto de entrada.
- Carregue apenas os docs relevantes à task.
- Use Grep/Glob para localizar arquivos e símbolos antes de ler módulos inteiros. Busca de código é ferramenta nativa — não há agente de busca.
- Evite carregar docs completos quando a busca por símbolo resolve mais rápido.

## O que NÃO fazer

- **Não referencie `.claude/agents/*.md`** — path inexistente neste repo. Fonte canônica de agentes é [`ai-governance/agents/`](../../../ai-governance/agents/).
- **Não trate `.claude/fluxos/*` como verdade paralela** — são resumos apontadores; o canônico é `ai-governance/` + `AGENTS.md`.
- **Não referencie personas legadas** (Felipe, Lia, Gema, Rhodolfo, Juninho, Marina, Claudio, Nina, Taisa, Marcelo, Otávio) como responsáveis ativos — ver [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md) §2.
- Não infira paths não confirmados no código.
- Não invente comportamento de feature não confirmado.

## Docs de referência rápida

- [`docs_ai/ARQUITETURA/README.md`](../../docs_ai/ARQUITETURA/README.md) — design do sistema
- [`docs_ai/TECNICO.md`](../../docs_ai/TECNICO.md) — build, dependências, integrações
- [`docs_ai/ARQUITETURA/MODULOS/`](../../docs_ai/ARQUITETURA/MODULOS/) — breakdown de módulos
- [`HANDOFF_RULES.md`](HANDOFF_RULES.md) — protocolo de handoff
- [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md) — fluxo completo
