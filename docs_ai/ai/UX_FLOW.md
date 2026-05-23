# UX Flow for Agents

> Referência: `AGENTS.md` na raiz deste projeto.
> Design system: `docs_ai/design-system/`

## Objetivos de UX

- **Intuitividade e eficiência**: padrões documentados em `design-system/NAVIGATION.md` e `design-system/COMPONENTS_ANDROID.md`.
- **Consistência**: aderência estrita a Material Design 3 (`design-system/MD3_GUIDELINES.md`).
- **Acessibilidade**: contraste verificado em `design-system/COLORS.md`, targets de toque adequados.
- **Clareza de diagnóstico**: dados de rede traduzidos em linguagem acessível ao usuário final.

## Quando Lia é acionada

Lia é obrigatória sempre que a task envolver:
- Tela nova ou modificação de tela existente.
- Estado visual novo: loading, vazio, erro, sucesso, thinking.
- Texto ou microcopy visível ao usuário.
- Resposta de IA ou diagnóstico exibido na tela.
- Mudança de fluxo de navegação.

Dispensada apenas em mudanças puramente nos módulos `:core*` sem impacto visual.

## Dois momentos de atuação da Lia

1. **Antes da implementação**: revisão do plano do Cláudio — valida UX antes de codificar.
2. **Pós-implementação**: revisão paralela com Gema — valida UX, MD3, microcopy do entregável real.

## Princípios de design aplicados pelos agentes

1. **Centralidade no usuário**: decisões de design priorizadas pelos fluxos em `functional/`.
2. **Aderência a MD3**: uso rigoroso das diretrizes de `design-system/MD3_GUIDELINES.md`.
3. **Acessibilidade**: contraste e tipografia conforme `design-system/COLORS.md` e `design-system/TYPOGRAPHY.md`.
4. **Design iterativo**: melhorias informadas pelos fluxos funcionais e feedback de usuário.
5. **Consistência**: novos elementos de UI alinhados com `design-system/COMPONENTS_ANDROID.md` e `design-system/NAVIGATION.md`.

## Agentes de UX

| Agente | Responsabilidade |
|---|---|
| Lia | Revisão de UI, MD3, microcopy, acessibilidade, estados visuais — edita somente UI/layout |
| Camilo | Implementa UI conforme specs da Lia |
| Gema | Valida que a implementação de UI não introduz bugs ou regressões |

## Referências

- `design-system/COMPONENTS_ANDROID.md` — 25 componentes (Orbit, SpeedTest, Layout)
- `design-system/MD3_GUIDELINES.md` — diretrizes Material Design 3
- `design-system/COLORS.md` — tokens de cor, contraste
- `design-system/TYPOGRAPHY.md` — escala tipográfica MD3
- `design-system/MOTION.md` — animações e transições
- `design-system/NAVIGATION.md` — padrões de navegação
- `functional/AI_ASSISTANT.md` — apresentação de respostas de IA ao usuário
- `functional/DIAGNOSTIC_FLOW.md` — fluxo de diagnóstico
