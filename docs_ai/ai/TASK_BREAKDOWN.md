# Task Breakdown

> Referência completa: `AGENTS.md` e `docs_ai/ai/AGENT_WORKFLOW.md`.

## Princípios

- **Modularidade**: alinhe com os módulos `:app`, `:core*`, `:feature*` do Android.
- **Responsabilidade única**: cada sub-task tem um objetivo verificável.
- **Acionável**: mudanças ou investigações com resultado concreto.
- **Dependências**: rastreie bloqueios e sequências de execução.

## Processo de decomposição

1. **Analise** a task → colete contexto de `docs_ai/technical/` e do código.
2. **Estime escopo**: se >5 módulos Android ou >1 dia de trabalho → interrompa e pergunte ao usuário antes de avançar.
3. **Decomponha** em tasks pequenas (prefira 10 tasks pequenas a 1 task gigante).
4. **Atribua** ao agente correto com base na especialização (ver tabela abaixo).
5. **Mapeie dependências** e ordene a execução.
6. **Execute** e verifique.

## Regra de granularidade

- Nenhum dev (Camilo) recebe tarefa vaga, aberta ou monstruosa.
- Bugfix simples (5 arquivos ou menos, sem mudança de contrato) → Camilo direto, sem passar pelo Cláudio.
- Tasks médias ou grandes → obrigatório passar pelo Cláudio para decomposição.

## Mapeamento de agentes por tipo de task

| Tipo de task | Agente responsável |
|---|---|
| Arquitetura, planejamento, decomposição | Cláudio |
| Implementação Android (Kotlin, Compose, MVVM) | Camilo |
| UX, design, Material Design 3, microcopy | Lia |
| APIs Android, permissões, hardware, OEM quirks | Otávio |
| Revisão de qualidade, bugs, regressão | Gema |
| Versionamento, changelog, resumo técnico | Nina |
| Documentação funcional, técnica, fluxos, PPT, HTML | Taisa |
| Busca de código, grep de símbolos, listagem de arquivos | Marcelo |

## Referências

- `ai/AGENT_WORKFLOW.md` — fluxo completo com responsabilidades por agente
- `ai/HANDOFF_RULES.md` — protocolo de handoff
- `technical/MODULES.md` — módulos Android disponíveis
