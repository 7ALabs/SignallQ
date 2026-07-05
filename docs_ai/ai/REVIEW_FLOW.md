# Review Flow for Agents

> Referência: `AGENTS.md` e `docs_ai/ai/AGENT_WORKFLOW.md`.

## Tipos de revisão

| Tipo | Critérios | Agente |
|---|---|---|
| Código | Corretude, arquitetura, performance, segurança, testes, ausência de regressão | Gema |
| UX/Design | Aderência MD3, componentes, fluxos, microcopy, acessibilidade | Lia |
| Documentação | Precisão, clareza, concisão, ausência de linguagem inferencial | Taisa |

## Processo de revisão

1. **Gatilho**: Camilo conclui implementação.
2. **Checks automáticos**: `./gradlew lint` e `./gradlew test` devem passar.
3. **Revisão especializada em paralelo**:
   - Gema: bugs, regressões, arquitetura, risco técnico.
   - Lia: UX, MD3, microcopy, estados visuais, acessibilidade.
4. **Feedback**: agentes emitem parecer; humano aprova ou solicita correção.
5. **Handoff para Nina**: após aprovação, bump de versão e changelog.

## Padrões de qualidade

- Código: `technical/ARCHITECTURE.md`, `technical/BUILD_SYSTEM.md`
- Design: `design-system/` (MD3, componentes, cores, tipografia)
- Documentação: sem linguagem inferencial ("likely", "probably", "appears to") — apenas fatos verificáveis no código

## O que Gema não faz

- Não implementa correções — devolve para Camilo.
- Não aprova mudanças de arquitetura sem consultar Cláudio.

## O que Lia não faz

- Não edita lógica de negócio — apenas UI e layout.
- Não aprova UX de feature que não foi especificada antes da implementação.

## Referências

- `ai/AGENT_WORKFLOW.md` — fluxo completo do sistema multiagente
- `design-system/MD3_GUIDELINES.md` — referência de revisão visual
- `technical/ARCHITECTURE.md` — referência de revisão técnica
